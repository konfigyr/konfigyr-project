package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

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

	private final Marker REGISTERED = MarkerFactory.getMarker("ACCOUNT_REGISTERED");

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
					.set(generateIdentifier())
					.set(ACCOUNTS.EMAIL, registration.email())
					.set(ACCOUNTS.FIRST_NAME, registration.firstName())
					.set(ACCOUNTS.LAST_NAME, registration.lastName())
					.set(ACCOUNTS.AVATAR, registration.avatar())
					.set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
					.set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
					.returning(ACCOUNTS.fields())
					.fetchOne(mapper());
		} catch (DuplicateKeyException e) {
			throw new AccountExistsException(registration, e);
		} catch (Exception e) {
			throw new AccountException("Unexpected exception occurred while registering an account", e);
		}

		if (account == null) {
			throw new IllegalStateException("Could not create user account from: " + registration);
		}

		publisher.publishEvent(new AccountRegisteredEvent(account.id()));

		log.info(REGISTERED, "Successfully registered new account {} from {}", account.id(), registration);

		return account;
	}

	private Optional<Account> fetch(@NonNull Condition condition) {
		return context
			.select(ACCOUNTS.fields())
			.from(ACCOUNTS)
			.where(condition)
			.fetchOptional(mapper());
	}

	@NonNull
	private Map<? extends Field<Long>, Long> generateIdentifier() {
		return EntityId.generate()
				.map(EntityId::get)
				.map(id -> Map.of(ACCOUNTS.ID, id))
				.orElseGet(Collections::emptyMap);
	}

	private static RecordMapper<Record, Account> mapper() {
		return record -> Account.builder()
				.id(record.get(ACCOUNTS.ID))
				.status(record.get(ACCOUNTS.STATUS))
				.email(record.get(ACCOUNTS.EMAIL))
				.firstName(record.get(ACCOUNTS.FIRST_NAME))
				.lastName(record.get(ACCOUNTS.LAST_NAME))
				.avatar(record.get(ACCOUNTS.AVATAR))
				.lastLoginAt(record.get(ACCOUNTS.LAST_LOGIN_AT))
				.createdAt(record.get(ACCOUNTS.CREATED_AT))
				.updatedAt(record.get(ACCOUNTS.UPDATED_AT))
				.build();
	}

}
