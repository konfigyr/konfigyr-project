package com.konfigyr.security;

import com.konfigyr.account.AccountRegistration;
import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.function.Supplier;

/**
 * Service used to retrieve {@link AccountPrincipal account principals} that would be used
 * to create a Spring {@link org.springframework.security.core.Authentication}.
 * <p>
 * Keep in mind that we intentionally do not wish to extend this interface with Spring Security
 * {@link org.springframework.security.core.userdetails.UserDetailsService} in order to exclude
 * its registration via the {@code InitializeUserDetailsBeanManagerConfigurer} global configurer.
 *
 * @author Vladimir Spasic
 **/
@Service
public interface PrincipalService {

	/**
	 * Looks up a {@link AccountPrincipal} using the {@link EntityId account identifier}.
	 *
	 * @param id account identifier, never {@literal null}
	 * @return located account principal, never {@literal null}
	 * @throws UsernameNotFoundException  if the account could not be found
	 */
	@NonNull AccountPrincipal lookup(@NonNull EntityId id) throws UsernameNotFoundException;

	/**
	 * Looks up a {@link AccountPrincipal} using the username attribute. The username attribute
	 * should be a serialized external value of the {@link EntityId}.
	 * <p>
	 * If the username is not a valid {@link EntityId}, this method would throw a {@link UsernameNotFoundException}.
	 *
	 * @param username account username, never {@literal null}
	 * @return located account principal, never {@literal null}
	 * @throws UsernameNotFoundException  if the account could not be found or username is invalid
	 */
	@NonNull AccountPrincipal lookup(@NonNull String username) throws UsernameNotFoundException;

	/**
	 * Looks up a {@link AccountPrincipal} using the resolved {@link OAuth2User} that was retrieved from
	 * an external OAuth Authorization server.
	 * <p>
	 * The principal lookup is performed by matching the email address of the {@link com.konfigyr.account.Account},
	 * therefore it is important the {@link OAuth2User#getName()} value is configured to use the email attribute.
	 * <p>
	 * In case the {@link AccountPrincipal} does not exist for the given {@link OAuth2User} user, the
	 * {@link AccountRegistration} supplier function is needed. This registration data would be used to create
	 * new {@link com.konfigyr.account.Account} and set up the {@link AccountPrincipal}.
	 *
	 * @param user retrieved OAuth user, never {@literal null}
	 * @param supplier account registration supplier, never {@literal null}
	 * @return existing or new account principal, never {@literal null}
	 */
	@NonNull AccountPrincipal lookup(@NonNull OAuth2User user, @NonNull Supplier<AccountRegistration> supplier);
}
