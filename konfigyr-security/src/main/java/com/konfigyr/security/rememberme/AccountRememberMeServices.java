package com.konfigyr.security.rememberme;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.PrincipalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Implementation of the {@link org.springframework.security.web.authentication.RememberMeServices} that
 * is based on the {@link org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices}.
 * <p>
 * The difference here is that we are not using the password of the {@link UserDetails user} to generate
 * the cookie token value. User accounts within this application do not use passwords.
 * <p>
 * The cookie encoded by this implementation adopts the following form:
 * <pre>
 * username + ":" + expiryTime + HEX(SHA-256(username + ":" + expiryTime + ":" + key))
 * </pre>
 *
 * @author Vladimir Spasic
 * @see org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices
 **/
public class AccountRememberMeServices extends AbstractRememberMeServices {

	static final String KEY = EntityId.from(123456789).serialize();
	static final String COOKIE_NAME = "konfigyr.account";
	static final String DIGEST_ALGORITHM = "SHA-256";
	static final int TOKEN_VALIDITY = (int) Duration.ofDays(14).toSeconds();

	public AccountRememberMeServices(PrincipalService service) {
		super(KEY, service::lookup);

		super.setTokenValiditySeconds(TOKEN_VALIDITY);
		super.setCookieName(COOKIE_NAME);
		super.setUseSecureCookie(true);
		super.setAlwaysRemember(true);
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

		final long expiryTime = System.currentTimeMillis() + getTokenValiditySeconds();
		final String signature = generateSignature(username, expiryTime);

		setCookie(new String[] { username, Long.toString(expiryTime), signature }, getTokenValiditySeconds(), request, response);

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
			String[] cookieTokens,
			HttpServletRequest request,
			HttpServletResponse response
	) throws RememberMeAuthenticationException, UsernameNotFoundException {
		assertTokenLength(cookieTokens);
		long tokenExpiryTime = extractTokenExpiryTime(cookieTokens);

		final UserDetails userDetails = retrieveUserDetails(cookieTokens[0]);

		// Generate the signature to check if it matches the signature of token
		final String signature = generateSignature(userDetails.getUsername(), tokenExpiryTime);

		if (signaturesMatch(signature, cookieTokens[2])) {
			return userDetails;
		}

		throw new InvalidCookieException("Cookie contained signature '" + cookieTokens[2] + "' but expected '"
				+ signature + "'");
	}

	private long extractTokenExpiryTime(String[] cookieTokens) {
		final long expiryTime;

		try {
			expiryTime = Long.parseLong(cookieTokens[1]);
		} catch (NumberFormatException nfe) {
			throw new InvalidCookieException("Cookie did not contain a valid number (contained '" + cookieTokens[1] + "')");
		}

		if (expiryTime < System.currentTimeMillis()) {
			throw new InvalidCookieException("Cookie has expired (expired on '" + new Date(expiryTime) + "')");
		}

		return expiryTime;
	}

	@Override
	public void setAlwaysRemember(boolean alwaysRemember) {
		unsupportedOperation("alwaysRemember");
	}

	@Override
	public void setCookieName(String cookieName) {
		unsupportedOperation("cookieName");
	}

	@Override
	public void setUseSecureCookie(boolean useSecureCookie) {
		unsupportedOperation("useSecureCookie");
	}

	@Override
	public void setParameter(String parameter) {
		unsupportedOperation("parameter");
	}

	@Override
	public void setTokenValiditySeconds(int tokenValiditySeconds) {
		unsupportedOperation("tokenValiditySeconds");
	}

	private UserDetails retrieveUserDetails(String username) {
		final UserDetails userDetails = getUserDetailsService().loadUserByUsername(username);

		if (userDetails == null) {
			throw new InternalAuthenticationServiceException("User details service returned null for username '"
					+ username + "'. This is considered as an interface contract violation.");
		}

		return userDetails;
	}

	private String retrieveUserName(Authentication authentication) {
		Assert.notNull(authentication, "Authentication can not be null");

		if (authentication.getPrincipal() instanceof UserDetails user) {
			return user.getUsername();
		}

		return Objects.toString(authentication.getPrincipal(), null);
	}

	private String generateSignature(String username, long tokenExpiryTime) {
		return generateSignature(username, tokenExpiryTime, getKey(), DIGEST_ALGORITHM);
	}

	private static void assertTokenLength(String[] tokens) {
		if (tokens.length != 3) {
			throw new InvalidCookieException("Cookie needs to contain 3 tokens: '" + Arrays.asList(tokens) + "'");
		}
	}

	static String generateSignature(String username, long tokenExpiryTime, String key, String algorithm) {
		final String data = username + ":" + tokenExpiryTime + ":" + key;

		try {
			final MessageDigest digest = MessageDigest.getInstance(algorithm);
			return new String(Hex.encode(digest.digest(data.getBytes())));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("No '" + algorithm + "' digest algorithm is available!", ex);
		}
	}

	private static boolean signaturesMatch(String expected, String actual) {
		return MessageDigest.isEqual(Utf8.encode(expected), Utf8.encode(actual));
	}

	private static void unsupportedOperation(String field) {
		throw new UnsupportedOperationException("It is not possible to set '" + field + "' field for " +
				"this implementation of the remember me services.");
	}
}
