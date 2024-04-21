package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import com.konfigyr.jooq.SettableRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/**
 * Implementation of the {@link AccountManager} that uses {@link DSLContext jOOQ} to communicate with the
 * persistence layer.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
class DefaultAccountManager implements AccountManager {

	static Name MEMBERSHIPS_ALIAS = DSL.name("memberships");

	private static final Marker REGISTERED = MarkerFactory.getMarker("ACCOUNT_REGISTERED");

	private final AccountMapper mapper = new AccountMapper(MEMBERSHIPS_ALIAS);

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Transactional(readOnly = true)
	public Optional<Account> findById(@NonNull EntityId id) {
		return fetch(ACCOUNTS.ID.eq(id.get()));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true)
	public Optional<Account> findByEmail(@NonNull String email) {
		return fetch(ACCOUNTS.EMAIL.equalIgnoreCase(email));
	}

	@NonNull
	@Override
	@Transactional
	public Account create(@NonNull AccountRegistration registration) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create account from: {}", registration);
		}

		final Account account;

		try {
			account = context.insertInto(ACCOUNTS)
					.set(
							SettableRecord.of(context, ACCOUNTS)
									.set(ACCOUNTS.ID, EntityId.generate().map(EntityId::get))
									.set(ACCOUNTS.EMAIL, registration.email())
									.set(ACCOUNTS.FIRST_NAME, registration.firstName())
									.set(ACCOUNTS.LAST_NAME, registration.lastName())
									.set(ACCOUNTS.AVATAR, registration.avatar())
									.set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
									.set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
									.get()
					)
					.returning(ACCOUNTS.fields())
					.fetchOne(mapper::account);
		} catch (DuplicateKeyException e) {
			throw new AccountExistsException(registration, e);
		} catch (Exception e) {
			throw new AccountException("Unexpected exception occurred while registering an account", e);
		}

		Assert.state(account != null, () -> "Could not create user account from: " + registration);

		publisher.publishEvent(new AccountEvent.Registered(account.id()));

		log.info(REGISTERED, "Successfully registered new account {} from {}", account.id(), registration);

		return account;
	}

	@NonNull
	@Override
	@Transactional(readOnly = true)
	public Memberships findMemberships(@NonNull EntityId id) {
		final var memberships = createMembershipsMultiselectField();

		return context.select(ACCOUNTS.ID, memberships)
				.from(ACCOUNTS)
				.where(ACCOUNTS.ID.eq(id.get()))
				.fetchOptional(record -> Memberships.of(record.get(memberships)))
				.orElseThrow(() -> new AccountNotFoundException(id));
	}

	private Optional<Account> fetch(@NonNull Condition condition) {
		return context.select(
				ACCOUNTS.ID,
				ACCOUNTS.EMAIL,
				ACCOUNTS.STATUS,
				ACCOUNTS.FIRST_NAME,
				ACCOUNTS.LAST_NAME,
				ACCOUNTS.AVATAR,
				ACCOUNTS.LAST_LOGIN_AT,
				ACCOUNTS.CREATED_AT,
				ACCOUNTS.UPDATED_AT,
				createMembershipsMultiselectField()
			)
			.from(ACCOUNTS)
			.where(condition)
			.fetchOptional(mapper::account);
	}

	private Field<List<Membership>> createMembershipsMultiselectField() {
		return DSL.multiset(
				DSL.select(
						NAMESPACE_MEMBERS.ID,
						NAMESPACE_MEMBERS.ROLE,
						NAMESPACE_MEMBERS.SINCE,
						NAMESPACES.SLUG,
						NAMESPACES.NAME,
						NAMESPACES.TYPE
				)
				.from(NAMESPACE_MEMBERS)
				.join(NAMESPACES)
				.on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.where(ACCOUNTS.ID.eq(NAMESPACE_MEMBERS.ACCOUNT_ID))
		).as(MEMBERSHIPS_ALIAS).convertFrom(mapper::memberships);
	}

}
