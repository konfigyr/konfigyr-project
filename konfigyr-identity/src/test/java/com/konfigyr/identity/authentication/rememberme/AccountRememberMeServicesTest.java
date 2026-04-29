package com.konfigyr.identity.authentication.rememberme;

import com.konfigyr.entity.EntityId;
import jakarta.servlet.http.Cookie;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.data.Index;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountRememberMeServicesTest {

	final UserDetails principal = User.withUsername(EntityId.from(92746L).serialize())
		.password("not-used")
		.authorities("konfigyr-account")
		.build();

	@Mock
	UserDetailsService service;

	MockHttpServletRequest request;
	MockHttpServletResponse response;
	AccountRememberMeServices services;

	@BeforeEach
	void createTokenBasedRememberMeServices() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		services = new AccountRememberMeServices(service);
	}

	@Test
	@DisplayName("should return null when cookies are present in the request")
	void autoLoginReturnsNullIfNoCookiePresented() {
		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNull();
	}

	@Test
	@DisplayName("should return null when remember-me cookie is not present")
	void autoLoginIgnoresUnrelatedCookie() {
		request.setCookies(new Cookie("some", "cookie"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNull();
	}

	@Test
	@DisplayName("should return null when remember-me cookie is invalid and clear it")
	void autoLoginReturnsNullAndClearsCookieIfMissingThreeTokensInCookieValue() {
		request.setCookies(createCookie(encode("x")));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie is not Base64 encoded and clear it")
	void autoLoginClearsNonBase64EncodedCookie() {
		request.setCookies(createCookie("non-encoded-value"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie is expired and clear it")
	void autoLoginReturnsNullForExpiredCookieAndClearsCookie() {
		request.setCookies(createCookie(System.currentTimeMillis() - 1000000, principal.getUsername()));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie signature does not match and clear it")
	void autoLoginClearsCookieIfSignatureBlocksDoesNotMatchExpectedValue() {
		request.setCookies(createCookie(System.currentTimeMillis() + 1000000, principal.getUsername(), "WRONG_KEY"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me signing algorithm is different")
	void autoLoginClearsCookieIfSignatureAlgorithmDoesNotMatch() {
		request.setCookies(createCookie(
				System.currentTimeMillis() + 1000000, principal.getUsername(), AccountRememberMeServices.KEY, "MD5"
		));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie expiry time is invalid and clear it")
	void autoLoginClearsCookieIfTokenDoesNotContainANumberInCookieValue() {
		request.setCookies(createCookie(encode("username:factor:NOT_A_NUMBER:NOT_A_NUMBER:signature")));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when user account is not found and clear it")
	void autoLoginClearsCookieIfUserNotFound() {
		doThrow(UsernameNotFoundException.class).when(service).loadUserByUsername(anyString());

		request.setCookies(createCookie(System.currentTimeMillis() + 1000000, "not-found"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should create authentication for valid cookie")
	void autoLoginWithValidTokenAndUserSucceeds() {
		final long issuedAt = System.currentTimeMillis();
		doReturn(principal).when(service).loadUserByUsername(anyString());

		request.setCookies(createCookie(issuedAt, issuedAt + 1000000, principal.getUsername()));

		assertThat(services.autoLogin(request, response))
				.isNotNull()
				.returns(principal, Authentication::getPrincipal)
				.returns(principal.getUsername(), Authentication::getName)
				.satisfies(it -> assertThat(it.getAuthorities())
						.hasSize(2)
						.asInstanceOf(InstanceOfAssertFactories.iterable(GrantedAuthority.class))
						.containsExactlyInAnyOrder(
								new SimpleGrantedAuthority("konfigyr-account"),
								FactorGrantedAuthority.withAuthority("factor")
										.issuedAt(Instant.ofEpochMilli(issuedAt))
										.build()
						));

		assertThat(request.getAttribute(AccountRememberMeServices.STORED_FACTOR_GRANTED_AUTHORITY))
				.isNull();
	}

	@Test
	@DisplayName("should clear cookie when authentication fails")
	void loginFailClearsCookie() {
		assertThatNoException().isThrownBy(() -> services.loginFail(request, response));

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should fail to create cookie without issued factor granted authority")
	void loginSuccessWithoutFactorAuthority() {
		request.addParameter(AbstractRememberMeServices.DEFAULT_PARAMETER, "on");

		final var authentication = createAuthentication(principal);

		assertThatIllegalStateException()
				.isThrownBy(() -> services.loginSuccess(request, response, authentication))
				.withMessageContaining("No FactorGrantedAuthority found in Authentication: %s", authentication)
				.withNoCause();
	}

	@Test
	@DisplayName("should create cookie even without remember-me parameter set")
	void loginSuccessWhenParameterNotSetOrFalse() {
		request.addParameter(AbstractRememberMeServices.DEFAULT_PARAMETER, "false");

		services.loginSuccess(request, response, createAuthentication(principal, FactorGrantedAuthority.PASSWORD_AUTHORITY));

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns((int) Duration.ofDays(14).toSeconds(), Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should create cookie with remember-me parameter set")
	void loginSuccessSetsCookie() {
		request.addParameter(AbstractRememberMeServices.DEFAULT_PARAMETER, "on");

		services.loginSuccess(request, response, createAuthentication(principal, FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY));

		final var cookie = response.getCookie(AccountRememberMeServices.COOKIE_NAME);

		assertThat(cookie)
				.isNotNull()
				.returns((int) Duration.ofDays(14).toSeconds(), Cookie::getMaxAge)
				.returns(request.isSecure(), Cookie::getSecure)
				.returns("/", Cookie::getPath)
				.returns(null, Cookie::getDomain);

		assertThat(cookie.getValue())
				.isNotBlank()
				.isBase64();

		assertThat(decode(cookie.getValue()).split(":"))
				.hasSize(5)
				.contains(principal.getUsername(), Index.atIndex(0))
				.contains(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY, Index.atIndex(1))
				.satisfies(tokens -> assertThat(Long.parseLong(tokens[2]))
						.isCloseTo(
								System.currentTimeMillis(),
								Offset.offset(300L) // should not take more than 300ms to generate the cookie
						)
				)
				.satisfies(tokens -> assertThat(Long.parseLong(tokens[3]))
						.isCloseTo(
								System.currentTimeMillis() + Duration.ofDays(14).toMillis(),
								Offset.offset(300L) // should not take more than 300ms to generate the cookie
						)
				)
				.satisfies(tokens -> assertThat(tokens[4])
						.isNotBlank()
						.isHexadecimal()
				);
	}

	@Test
	@DisplayName("should not create cookie without a valid principal in authentication")
	void loginSuccessWithoutAuthenticationName() {
		final var authentication = new TestingAuthenticationToken(null, "", "authority");
		services.loginSuccess(request, response, authentication);

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNull();
	}

	@Test
	@DisplayName("should remove cookie on successful logout")
	void logoutShouldClearRememberMeCookies() {
		request.setCookies(createCookie(System.currentTimeMillis() + 2000, principal.getUsername()));

		services.logout(request, response, createAuthentication(principal));

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should only remove the remember-me cookie")
	void logoutShouldNotClearOtherCookies() {
		response.addCookie(new Cookie("other-cookie", "x"));

		services.logout(request, response, createAuthentication(principal));

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);

		assertThat(response.getCookie("other-cookie"))
				.isNotNull()
				.returns(-1, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should catch unknown signing algorithm exceptions")
	void shouldCatchUnknownAlgorithmExceptions() {
		assertThatThrownBy(() -> AccountRememberMeServices.generateSignature("test", "factor", 1, 1, "key", "unknown-algo"))
				.isInstanceOf(IllegalStateException.class)
				.hasRootCauseInstanceOf(NoSuchAlgorithmException.class);
	}

	private static Authentication createAuthentication(UserDetails user) {
		return new TestingAuthenticationToken(user, user.getPassword(), user.getAuthorities());
	}

	private static Authentication createAuthentication(UserDetails user, String factor) {
		final List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
		authorities.add(FactorGrantedAuthority.fromAuthority(factor));
		return new TestingAuthenticationToken(user, user.getPassword(), authorities);
	}

	private static Cookie createCookie(long expiryTime, String username) {
		return createCookie(expiryTime, username, AccountRememberMeServices.KEY);
	}

	private static Cookie createCookie(long issuedTime, long expiryTime, String username) {
		return createCookie(issuedTime, expiryTime, username, AccountRememberMeServices.KEY, AccountRememberMeServices.DIGEST_ALGORITHM);
	}

	private static Cookie createCookie(long expiryTime, String username, String key) {
		return createCookie(expiryTime, username, key, AccountRememberMeServices.DIGEST_ALGORITHM);
	}

	private static Cookie createCookie(long expiryTime, String username, String key, String algorithm) {
		return createCookie(System.currentTimeMillis(), expiryTime, username, key, algorithm);
	}

	private static Cookie createCookie(long issuedTime, long expiryTime, String username, String key, String algorithm) {
		final String signature = AccountRememberMeServices.generateSignature(username, "factor", issuedTime, expiryTime, key, algorithm);
		return createCookie(encode(username + ":factor:" + issuedTime + ":" + expiryTime + ":" + signature));
	}

	private static Cookie createCookie(String value) {
		return new Cookie(AccountRememberMeServices.COOKIE_NAME, value);
	}

	private static String encode(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes());
	}

	private static String decode(String value) {
		return new String(Base64.getDecoder().decode(value));
	}

}
