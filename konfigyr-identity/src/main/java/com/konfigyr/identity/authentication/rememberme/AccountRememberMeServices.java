package com.konfigyr.identity.authentication.rememberme;

import com.konfigyr.entity.EntityId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Implementation of the {@link RememberMeServices} that
 * is based on the {@link org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices}.
 * <p>
 * The difference here is that we are not using the password of the {@link UserDetails user} to generate
 * the cookie token value. User accounts within this application do not use passwords, but we do require
 * the {@link FactorGrantedAuthority} to be present in the {@link Authentication} that should be stored by
 * this implementation.
 * <p>
 * The cookie encoded by this implementation adopts the following form:
 * <pre>
 * username + ":" + factor + ":" + issuedTime + ":" + expiryTime + HEX(SHA-256(username + ":" + expiryTime + ":" + key))
 * </pre>
 * <p>
 * Keep in mind that this implementation of the {@link RememberMeServices} would <strong>always</strong>
 * add, or extend the existing, cookie when there was a successful authentication. The cookie that is
 * set would have a name of <code>konfigyr.account</code> and would expire after <strong>14 days</strong>.
 *
 * @author Vladimir Spasic
 * @see org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices
 **/
@NullMarked
public class AccountRememberMeServices implements RememberMeServices, LogoutHandler {

	/**
	 * The key used to validate the {@link Authentication} that is returned by this service
	 * when the {@link UserDetails} was successfully resolved.
	 */
	public static final String KEY = EntityId.from(123456789).serialize();

	static final String STORED_FACTOR_GRANTED_AUTHORITY = AccountRememberMeServices.class.getName() + "#FACTOR_GRANTED_AUTHORITY";
	static final String COOKIE_NAME = "konfigyr.account";
	static final String DIGEST_ALGORITHM = "SHA-256";
	static final Duration TOKEN_VALIDITY = Duration.ofDays(14);

	private final AbstractRememberMeServices delegate;

	/**
	 * Creates a new instance of the {@link AccountRememberMeServices} that uses the {@link UserDetailsService}
	 * to retrieve {@link UserDetails}.
	 * <p>
	 *
	 * @param service user details service used to load users, can't be {@literal null}
	 */
	public AccountRememberMeServices(UserDetailsService service) {
		this.delegate = new InternalRememberMeServices(KEY, service);
		delegate.setTokenValiditySeconds((int) TOKEN_VALIDITY.toSeconds());
		delegate.setCookieName(COOKIE_NAME);
		delegate.setAlwaysRemember(true);
		delegate.afterPropertiesSet();
	}

	@Nullable
	@Override
	public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
		return delegate.autoLogin(request, response);
	}

	@Override
	public void loginFail(HttpServletRequest request, HttpServletResponse response) {
		delegate.loginFail(request, response);
	}

	@Override
	public void loginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {
		delegate.loginSuccess(request, response, successfulAuthentication);
	}

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, @Nullable Authentication authentication) {
		delegate.logout(request, response, authentication);
	}

	static String generateSignature(String username, String factor, long issuedTime, long tokenExpiryTime, String key, String algorithm) {
		final String data = username + ":" + ":" + factor + ":" + issuedTime + ":" + tokenExpiryTime + ":" + key;

		try {
			final MessageDigest digest = MessageDigest.getInstance(algorithm);
			return new String(Hex.encode(digest.digest(data.getBytes())));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("No '" + algorithm + "' digest algorithm is available!", ex);
		}
	}

	static boolean signaturesMatch(String expected, String actual) {
		return MessageDigest.isEqual(Utf8.encode(expected), Utf8.encode(actual));
	}

	static FactorGrantedAuthority resolveFactorGrantedAuthority(Authentication authentication) {
		FactorGrantedAuthority authority = null;
		for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
			if (grantedAuthority instanceof FactorGrantedAuthority factorGrantedAuthority) {
				if (authority == null || factorGrantedAuthority.getIssuedAt().isAfter(authority.getIssuedAt())) {
					authority = factorGrantedAuthority;
				}
			}
		}
		if (authority == null) {
			throw new IllegalStateException("No FactorGrantedAuthority found in Authentication: " + authentication);
		}
		return authority;
	}

	/**
	 * This class is intentionally made internal as we do not wish to expose the API of the
	 * {@link AbstractRememberMeServices} via {@link AccountRememberMeServices}. We should prevent
	 * any customization, i.e. different cookie names or max age..., of the {@link RememberMeServices}
	 * that would be registered in a Spring HTTP security filter chain.
	 */
	private static final class InternalRememberMeServices extends AbstractRememberMeServices {

		private InternalRememberMeServices(String key, UserDetailsService userDetailsService) {
			super(key, userDetailsService);
		}

		@Override
		protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
			final String username = retrieveUserName(authentication);

			// If we are unable to find a username do not generate any remember-me cookie
			if (!StringUtils.hasLength(username)) {
				logger.debug(LogMessage.format(
						"Can not add remember-me cookie due to a missing username in: %s",
						authentication
				));
				return;
			}

			final FactorGrantedAuthority factor = resolveFactorGrantedAuthority(authentication);
			final long expiryTime = System.currentTimeMillis() + (1000L * getTokenValiditySeconds());
			final String signature = generateSignature(username, factor.getAuthority(), factor.getIssuedAt().toEpochMilli(),
					expiryTime, getKey(), DIGEST_ALGORITHM);

			setCookie(new String[] { username, factor.getAuthority(), Long.toString(factor.getIssuedAt().toEpochMilli()),
							Long.toString(expiryTime), signature }, getTokenValiditySeconds(), request, response);

			if (this.logger.isDebugEnabled()) {
				this.logger.debug(LogMessage.format(
						"Added remember-me cookie for principal '%s' that expires at: '%s'",
						username,
						new Date(expiryTime)
				));
			}
		}

		@Override
		protected UserDetails processAutoLoginCookie(
				String[] tokens,
				HttpServletRequest request,
				HttpServletResponse response
		) throws RememberMeAuthenticationException, UsernameNotFoundException {
			if (tokens.length != 5) {
				throw new InvalidCookieException("Cookie needs to contain 5 tokens: '" + Arrays.asList(tokens) + "'");
			}

			final long factorIssuedTime = extractTokenTimestamp(tokens, 2);
			final long tokenExpiryTime = extractTokenTimestamp(tokens, 3);

			if (tokenExpiryTime < System.currentTimeMillis()) {
				throw new InvalidCookieException("Cookie has expired (expired on '" + new Date(tokenExpiryTime) + "')");
			}

			// a workaround that would pass the FactorGrantedAuthority to the resulting authentication
			request.setAttribute(STORED_FACTOR_GRANTED_AUTHORITY, FactorGrantedAuthority.withAuthority(tokens[1])
					.issuedAt(Instant.ofEpochMilli(factorIssuedTime))
					.build()
			);

			// Generate the signature to check if it matches the signature of the token
			final String signature = generateSignature(tokens[0], tokens[1], factorIssuedTime, tokenExpiryTime, getKey(), DIGEST_ALGORITHM);

			if (signaturesMatch(signature, tokens[4])) {
				return getUserDetailsService().loadUserByUsername(tokens[0]);
			}

			throw new InvalidCookieException("Cookie contained signature '" + tokens[2] + "' but expected '" + signature + "'");
		}

		@Override
		protected Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
			final Collection<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>(user.getAuthorities());
			// use the FactorGrantedAuthority that was stored in the request and extracted from the cookie
			mappedAuthorities.add((FactorGrantedAuthority) request.getAttribute(STORED_FACTOR_GRANTED_AUTHORITY));
			// clear the attribute as it is no longer required
			request.removeAttribute(STORED_FACTOR_GRANTED_AUTHORITY);

			final RememberMeAuthenticationToken authentication = new RememberMeAuthenticationToken(
					getKey(), user, mappedAuthorities
			);
			authentication.setDetails(getAuthenticationDetailsSource().buildDetails(request));
			return authentication;
		}

		private long extractTokenTimestamp(String[] tokens, int index) {
			final long timestamp;

			try {
				timestamp = Long.parseLong(tokens[index]);
			} catch (NumberFormatException nfe) {
				throw new InvalidCookieException("Cookie did not contain a valid number (contained '" + tokens[1] + "')");
			}

			return timestamp;
		}

		@Nullable
		private String retrieveUserName(Authentication authentication) {
			Assert.notNull(authentication, "Authentication can not be null");

			if (authentication.getPrincipal() == null) {
				return null;
			}

			return switch (authentication.getPrincipal()) {
				case String username -> username;
				case UserDetails user -> user.getUsername();
				case AuthenticatedPrincipal principal -> principal.getName();
				default -> Objects.toString(authentication.getPrincipal());
			};
		}

	}
}
