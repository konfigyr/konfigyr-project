package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;

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

	private static final Marker UPDATED = MarkerFactory.getMarker("ACCOUNT_UPDATED");
	private static final Marker DELETED = MarkerFactory.getMarker("ACCOUNT_DELETED");

	private final AccountMapper mapper = new AccountMapper();

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
	public Account update(@NonNull Account account) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to update account with data: {}", account);
		}

		final int count;

		try {
			count = context.update(ACCOUNTS)
					.set(ACCOUNTS.FIRST_NAME, account.firstName())
					.set(ACCOUNTS.LAST_NAME, account.lastName())
					.set(ACCOUNTS.AVATAR, account.avatar().get())
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

		// Verify the account exists before checking admin membership
		if (!context.fetchExists(ACCOUNTS, ACCOUNTS.ID.eq(id.get()))) {
			throw new AccountNotFoundException(id);
		}

		// Accounts that are the sole administrator of any namespace cannot be deleted.
		// This check queries the membership table directly to avoid a cross-module dependency.
		final boolean isAdmin = context.fetchExists(
				NAMESPACE_MEMBERS,
				NAMESPACE_MEMBERS.ACCOUNT_ID.eq(id.get()).and(NAMESPACE_MEMBERS.ROLE.eq("ADMIN"))
		);

		if (isAdmin) {
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
				ACCOUNTS.UPDATED_AT
			)
			.from(ACCOUNTS)
			.where(condition)
			.fetchOptional(mapper::account);
	}

}
