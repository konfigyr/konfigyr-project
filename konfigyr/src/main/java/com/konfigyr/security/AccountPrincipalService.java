package com.konfigyr.security;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountEvent;
import com.konfigyr.account.AccountManager;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Member;
import com.konfigyr.namespace.NamespaceEvent;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.security.provision.ProvisioningRequiredException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link PrincipalService} that is able to load the {@link AccountPrincipal}
 * using the {@link com.konfigyr.account.AccountManager}.
 *
 * @author Vladimir Spasic
 **/
@Slf4j
@RequiredArgsConstructor
public class AccountPrincipalService implements PrincipalService {

	private final AccountManager accounts;
	private final NamespaceManager namespaces;
	private final UserCache cache;

	/**
	 * Creates a new instance of the {@link AccountPrincipalService} with {@link NullUserCache} as
	 * the {@link UserCache} implementation. This would not cache {@link AccountPrincipal principals}
	 * that were retrieved by the {@link AccountManager}.
	 *
	 * @param accounts account manager used to lookup accounts, can't be {@literal null}
	 * @param namespaces account manager used to lookup memberships, can't be {@literal null}
	 */
	public AccountPrincipalService(AccountManager accounts, NamespaceManager namespaces) {
		this(accounts, namespaces, new NullUserCache());
	}

	@NonNull
	@Override
	public AccountPrincipal lookup(@NonNull OAuth2AuthenticatedPrincipal user, @NonNull String provider) {
		final Account account = accounts.findByEmail(user.getName())
				.orElseThrow(() -> new ProvisioningRequiredException(user, provider));

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

	@DomainEventHandler(name = "deleted", namespace = "namespaces")
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, classes = NamespaceEvent.Deleted.class)
	void onNamespaceDeletedEvent(@NonNull NamespaceEvent.Deleted event) {
		final Page<Member> members;

		try {
			members = namespaces.findMembers(event.id(), Pageable.unpaged());
		} catch (Exception ex) {
			log.warn("Unexpected error occurred while retrieving namespace members to clear from user cache for: {}", event, ex);
			return;
		}

		members.forEach(member -> cache.removeUserFromCache(member.account().serialize()));
	}

	@DomainEventHandler(name = "member-added", namespace = "namespaces")
	@TransactionalEventListener(classes = NamespaceEvent.MemberAdded.class)
	void onMemberAddedEvent(@NonNull NamespaceEvent.MemberAdded event) {
		cache.removeUserFromCache(event.account().serialize());
	}

	@DomainEventHandler(name = "member-updated", namespace = "namespaces")
	@TransactionalEventListener(classes = NamespaceEvent.MemberUpdated.class)
	void onMemberUpdatedEvent(@NonNull NamespaceEvent.MemberUpdated event) {
		cache.removeUserFromCache(event.account().serialize());
	}

	@DomainEventHandler(name = "member-removed", namespace = "namespaces")
	@TransactionalEventListener(classes = NamespaceEvent.MemberRemoved.class)
	void onMemberRemovedEvent(@NonNull NamespaceEvent.MemberRemoved event) {
		cache.removeUserFromCache(event.account().serialize());
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
		return accounts.findById(id)
				.map(AccountPrincipal::from)
				.orElse(null);
	}

}
