package com.konfigyr.identity.authentication;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Service used to retrieve {@link AccountIdentity account identities} that would be used
 * to create a Spring {@link org.springframework.security.core.Authentication}.
 * <p>
 * This interface also uses the {@link OAuth2UserService} that would try to resolve the
 * {@link OAuth2User} from OAuth Authorization server. When the {@link OAuth2User user attributes}
 * are resolved, the matching, or a new, {@link AccountIdentity} should be returned.
 * <p>
 * Keep in mind that we intentionally do not wish to extend this interface with Spring Security
 * {@link org.springframework.security.core.userdetails.UserDetailsService} in order to exclude
 * its registration via the {@code InitializeUserDetailsBeanManagerConfigurer} global configurer.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Service
public interface AccountIdentityService {

	/**
	 * Looks up a {@link AccountIdentity} using the {@link EntityId account identifier}.
	 *
	 * @param id account identifier, never {@literal null}
	 * @return located account identity, never {@literal null}
	 * @throws UsernameNotFoundException  if the account could not be found
	 */
	@NonNull AccountIdentity get(@NonNull EntityId id) throws UsernameNotFoundException;

	/**
	 * Looks up a {@link AccountIdentity} using the username attribute. The username attribute
	 * should be a serialized external value of the {@link EntityId}.
	 * <p>
	 * If the username is not a valid {@link EntityId}, this method would throw a {@link UsernameNotFoundException}.
	 *
	 * @param username account username, never {@literal null}
	 * @return located account identity, never {@literal null}
	 * @throws UsernameNotFoundException  if the account could not be found or username is invalid
	 */
	@NonNull AccountIdentity get(@NonNull String username) throws UsernameNotFoundException;

	/**
	 * Attempts to obtain the user attributes from the OAuth2 UserInfo Endpoint, or OpenID Token, using
	 * the Access Token granted to the {@link ClientRegistration} via given {@link OAuth2UserService}
	 * and returning an {@link AccountIdentity}.
	 * <p>
	 * The {@link AccountIdentity} that is returned would first be checked if one already exists for the
	 * resolved {@link OAuth2AuthenticatedPrincipal}. In case the {@link AccountIdentity} does not exist
	 * for the given {@link OAuth2AuthenticatedPrincipal} user, this service would attempt to provision
	 * a new identity using the retrieved user attributes.
	 * <p>
	 * The principal lookup is performed by matching the email address of the user account,
	 * therefore it is important that the {@link OAuth2AuthenticatedPrincipal#getName()} returns a value of
	 * the email attribute. Please configure your {@link ClientRegistration OAuth2 Clients accordingly}.
	 *
	 * @param service the OAuth 2.0 user to execute the request, can't be {@literal null}
	 * @param request the OAuth 2.0 user request, can't be {@literal null}
	 * @return located account identity or a new one, never {@literal null}
	 */
	AccountIdentity get(@NonNull OAuth2UserService<? super OAuth2UserRequest, ? extends OAuth2User> service,
						@NonNull OAuth2UserRequest request) throws OAuth2AuthenticationException;

}
