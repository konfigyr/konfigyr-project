package com.konfigyr.identity.authentication;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.idenitity.AccountIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Implementation of the {@link AccountIdentityService} that is able to load the {@link AccountIdentity}
 * from the {@link AccountIdentityRepository}.
 * <p>
 * This class would use the configured {@link OAuth2UserService} delegate to resolve the
 * {@link OAuth2User OAuth 2.0 User attributes} from OAuth Authorization server.
 * <p>
 * Once the {@link OAuth2User user attributes} are resolved, it would try to load the
 * {@link AccountIdentity} from the repository, if one exists, or it would attempt to register a new one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
class DefaultAccountIdentityService implements AccountIdentityService {

	private final AccountIdentityRepository repository;
	private final UserCache cache;

	/**
	 * Creates a new instance of the {@link DefaultAccountIdentityService} with {@link NullUserCache} as
	 * the {@link UserCache} implementation. This would not cache {@link AccountIdentity account identities}
	 * that were retrieved from the database.
	 *
	 * @param repository account identity repository, can't be {@literal null}
	 */
	DefaultAccountIdentityService(AccountIdentityRepository repository) {
		this(repository, new NullUserCache());
	}

	@NonNull
	@Override
	public AccountIdentity get(@NonNull String username) throws UsernameNotFoundException {
		final EntityId id;

		try {
			id = EntityId.from(username);
		} catch (IllegalArgumentException ex) {
			log.debug("Attempted to load an account principal for invalid username: {}", username);

			throw new UsernameNotFoundException("Failed to lookup user account for invalid username: " + username, ex);
		}

		return get(id);
	}

	@NonNull
	@Override
	public AccountIdentity get(@NonNull EntityId id) throws UsernameNotFoundException {
		AccountIdentity user = lookupFromCache(id.serialize());

		if (user == null) {
			user = repository.findById(id).orElse(null);
		}

		if (user == null) {
			throw new UsernameNotFoundException("Failed to lookup user account for identifier: " + id);
		}

		cache.putUserInCache(user);

		return user;
	}

	@NonNull
	@Override
	public <R extends OAuth2UserRequest, U extends OAuth2User> AccountIdentityUser get(
			@NonNull OAuth2UserService<R, U> service,
			@NonNull R request
	) throws OAuth2AuthenticationException {
		final OAuth2User user = service.loadUser(request);

		if (user == null) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
					"Could not extract user information from authorization server",
					null
			));
		}

		if (log.isDebugEnabled()) {
			log.debug("Successfully loaded OAuth user: {}", user);
		}

		final AccountIdentity identity = repository.findByEmail(user.getName())
				.orElseGet(() -> repository.create(user, request.getClientRegistration()));

		if (log.isDebugEnabled()) {
			log.debug("Successfully created or loaded account identity: [identity={}, user={}]", identity, user);
		}

		if (user instanceof OidcUser oidcUser) {
			return new OidcAccountIdentityUser(identity, oidcUser);
		}

		return new OAuthAccountIdentityUser(identity, user);

	}

	@NonNull
	public AccountIdentity get(@NonNull OAuth2AuthenticatedPrincipal user, @NonNull ClientRegistration client) {
		return repository.findByEmail(user.getName()).orElseGet(() -> repository.create(user, client));
	}

	@Nullable
	private AccountIdentity lookupFromCache(@NonNull String username) {
		final UserDetails user = cache.getUserFromCache(username);

		if (user == null) {
			return null;
		}

		if (user instanceof AccountIdentity identity) {
			return identity;
		}

		if (log.isDebugEnabled()) {
			log.debug("Found an invalid type of user in the user cache, removing cache entry: {}", user);
		}

		// remove from cache as it contains an invalid user
		cache.removeUserFromCache(username);

		return null;
	}

}
