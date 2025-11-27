package com.konfigyr.identity.authentication.idenitity;

import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityEvent;
import com.konfigyr.identity.authentication.AccountIdentityExistsException;
import com.konfigyr.identity.authentication.AccountIdentityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

/**
 * Repository that can be used to retrieve or create {@link AccountIdentity Account Identities}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
public class AccountIdentityRepository {

	private static final List<Field<?>> ACCOUNT_FIELDS = List.of(
			ACCOUNTS.ID,
			ACCOUNTS.EMAIL,
			ACCOUNTS.STATUS,
			ACCOUNTS.FIRST_NAME,
			ACCOUNTS.LAST_NAME,
			ACCOUNTS.AVATAR
	);

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Transactional(readOnly = true, label = "account-identity-repository.find-by-email")
	public Optional<AccountIdentity> findByEmail(@NonNull String email) {
		return fetch(ACCOUNTS.EMAIL.equalIgnoreCase(email));
	}

	@NonNull
	@Transactional(readOnly = true, label = "account-identity--repository.find-by-id")
	public Optional<AccountIdentity> findById(@NonNull EntityId id) throws UsernameNotFoundException {
		return fetch(ACCOUNTS.ID.eq(id.get()));
	}

	@NonNull
	@Transactional(label = "account-identity-service.authenticate")
	@DomainEventPublisher(publishes = "authentication.account-identity-created")
	public AccountIdentity create(@NonNull OAuth2AuthenticatedPrincipal user, @NonNull ClientRegistration client) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create account from: {}", user);
		}

		final SettableRecord record = AccountIdentityMapper.map(user, client)
				.set(ACCOUNTS.ID, EntityId.generate().map(EntityId::get))
				.set(ACCOUNTS.STATUS, AccountIdentityStatus.ACTIVE.name())
				.set(ACCOUNTS.CREATED_AT, OffsetDateTime.now())
				.set(ACCOUNTS.CREATED_AT, OffsetDateTime.now());

		final AccountIdentity identity;

		try {
			identity = context.insertInto(ACCOUNTS)
					.set(record.get())
					.returning(ACCOUNT_FIELDS)
					.fetchOne(AccountIdentityMapper::map);
		} catch (DuplicateKeyException ex) {
			throw new AccountIdentityExistsException(user);
		} catch (Exception ex) {
			throw new InternalAuthenticationServiceException("Failed to create account identity for OAuth2 " +
					"client with registration " + client.getRegistrationId(), ex);
		}

		Assert.state(identity != null, () -> "Could not create user account from: " + user);

		log.info("Successfully registered new account {} from {}", identity.getId(), user);

		publisher.publishEvent(new AccountIdentityEvent.Created(identity));

		return identity;
	}

	private Optional<AccountIdentity> fetch(@NonNull Condition condition) {
		return context.select(ACCOUNT_FIELDS)
				.from(ACCOUNTS)
				.where(condition)
				.fetchOptional(AccountIdentityMapper::map);
	}

}
