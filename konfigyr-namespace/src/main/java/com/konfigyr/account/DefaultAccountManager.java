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
	private static final Marker UPDATED = MarkerFactory.getMarker("ACCOUNT_UPDATED");
	private static final Marker DELETED = MarkerFactory.getMarker("ACCOUNT_DELETED");

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
									.set(ACCOUNTS.STATUS, AccountStatus.ACTIVE.name())
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
		final Field<List<Membership>> memberships = createMembershipsMultiselectField();

		return context.select(ACCOUNTS.ID, memberships)
				.from(ACCOUNTS)
				.where(ACCOUNTS.ID.eq(id.get()))
				.fetchOptional(record -> Memberships.of(record.get(memberships)))
				.orElseThrow(() -> new AccountNotFoundException(id));
	}

	@NonNull
	@Override
	@Transactional
	public Account update(@NonNull Account account) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to update account with data: {}", account);
		}

		final int count;

		try {
			count = context.update(ACCOUNTS)
					.set(ACCOUNTS.FIRST_NAME, account.firstName())
					.set(ACCOUNTS.LAST_NAME, account.lastName())
					.set(ACCOUNTS.AVATAR, account.avatar())
					.set(ACCOUNTS.UPDATED_AT, OffsetDateTime.now())
					.where(ACCOUNTS.ID.eq(account.id().get()))
					.execute();
		} catch (Exception e) {
			throw new AccountException("Failed to update account with identifier: " + account.id(), e);
		}

		if (count == 0) {
			throw new AccountNotFoundException(account.id());
		}

		publisher.publishEvent(new AccountEvent.Updated(account.id()));

		log.info(UPDATED, "Successfully updated account with identifier {}", account.id());

		return findById(account.id()).orElseThrow(
				() -> new IllegalStateException("Failed to lookup updated account with identifier: " + account.id())
		);
	}

	@Override
	@Transactional
	public void delete(@NonNull EntityId id) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to delete account with identifier: {}", id);
		}

		final Account account = findById(id).orElseThrow(() -> new AccountNotFoundException(id));

		// check if the account is an administrator member of any non-personal namespaces...
		if (!account.isDeletable()) {
			throw new AccountException("Can not delete account that is still an admin of non-personal namespaces");
		}

		final int count;

		try {
			count = context.delete(ACCOUNTS)
					.where(ACCOUNTS.ID.eq(id.get()))
					.execute();
		} catch (Exception e) {
			throw new AccountException("Failed to delete account with identifier: " + id, e);
		}

		Assert.state(count == 1, "Failed to delete account from the database");

		publisher.publishEvent(new AccountEvent.Deleted(id));

		log.info(DELETED, "Successfully deleted account with identifier {}", id);
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
						NAMESPACES.TYPE,
						NAMESPACES.AVATAR
				)
				.from(NAMESPACE_MEMBERS)
				.join(NAMESPACES)
				.on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.where(ACCOUNTS.ID.eq(NAMESPACE_MEMBERS.ACCOUNT_ID))
		).as(MEMBERSHIPS_ALIAS).convertFrom(mapper::memberships);
	}

}
