package com.konfigyr.security;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountEvent;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountRegistration;
import com.konfigyr.entity.EntityId;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Implementation of the {@link PrincipalService} that is able to load the {@link AccountPrincipal}
 * using the {@link com.konfigyr.account.AccountManager}.
 *
 * @author Vladimir Spasic
 **/
@Slf4j
@RequiredArgsConstructor
public class AccountPrincipalService implements PrincipalService {

	private final AccountManager manager;
	private final UserCache cache;

	/**
	 * Creates a new instance of the {@link AccountPrincipalService} with {@link NullUserCache} as
	 * the {@link UserCache} implementation. This would not cache {@link AccountPrincipal principals}
	 * that were retrieved by the {@link AccountManager}.
	 *
	 * @param manager account manager used to lookup accounts, can't be {@literal null}
	 */
	public AccountPrincipalService(AccountManager manager) {
		this(manager, new NullUserCache());
	}

	@NonNull
	@Override
	public AccountPrincipal lookup(@NonNull OAuth2User user, @NonNull Supplier<AccountRegistration> supplier) {
		final Account account = manager.findByEmail(user.getName())
				.orElseGet(() -> manager.create(supplier.get()));

		Assert.notNull(account.id(), "User account identifier can not be null");
		Assert.hasText(account.email(), "User account needs to have an email address set");

		return AccountPrincipal.from(account);
	}

	@NonNull
	@Override
	public AccountPrincipal lookup(@NonNull String username) throws UsernameNotFoundException {
		final EntityId id;

		try {
			id = EntityId.from(username);
		} catch (IllegalArgumentException ex) {
			log.debug("Attempted to load an account principal for invalid username: {}", username);

			throw new UsernameNotFoundException("Failed to lookup user account for invalid username: " + username, ex);
		}

		return lookup(id);
	}

	@NonNull
	@Override
	public AccountPrincipal lookup(@NonNull EntityId id) throws UsernameNotFoundException {
		AccountPrincipal user = lookupFromCache(id.serialize());

		if (user == null) {
			user = lookupFromManager(id);
		}

		if (user == null) {
			throw new UsernameNotFoundException("Failed to lookup user account for identifier: " + id);
		}

		cache.putUserInCache(user);

		return user;
	}

	@DomainEventHandler(name = "updated", namespace = "accounts")
	@TransactionalEventListener(classes = AccountEvent.Updated.class)
	void onAccountUpdatedEvent(@NonNull AccountEvent.Updated event) {
		resetSecurityContextForPrincipal(event.id());
	}

	@DomainEventHandler(name = "deleted", namespace = "accounts")
	@TransactionalEventListener(classes = AccountEvent.Deleted.class)
	void onAccountDeletedEvent(@NonNull AccountEvent.Deleted event) {
		resetSecurityContextForPrincipal(event.id());
	}

	private void resetSecurityContextForPrincipal(@NonNull EntityId principal) {
		log.debug("Clearing security context and user cache for account principal: {}", principal);

		cache.removeUserFromCache(principal.serialize());
		SecurityContextHolder.getContextHolderStrategy().getContext().setAuthentication(null);
	}

	@Nullable
	private AccountPrincipal lookupFromCache(@NonNull String username) {
		final UserDetails user = cache.getUserFromCache(username);

		if (user == null) {
			return null;
		}

		if (user instanceof AccountPrincipal principal) {
			return principal;
		}

		if (log.isDebugEnabled()) {
			log.debug("Found an invalid type of user in the user cache, removing cache entry: {}", user);
		}

		// remove from cache as it contains an invalid user
		cache.removeUserFromCache(username);

		return null;
	}

	@Nullable
	private AccountPrincipal lookupFromManager(@NonNull EntityId id) {
		return manager.findById(id)
				.map(AccountPrincipal::from)
				.orElse(null);
	}

}
