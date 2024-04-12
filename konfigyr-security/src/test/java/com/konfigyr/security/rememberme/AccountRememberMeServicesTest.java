package com.konfigyr.security.rememberme;

import com.konfigyr.account.Account;
import com.konfigyr.security.AccountPrincipal;
import com.konfigyr.security.PrincipalService;
import com.konfigyr.test.TestAccounts;
import jakarta.servlet.http.Cookie;
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
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountRememberMeServicesTest {

	final Account account = TestAccounts.john().build();
	final AccountPrincipal principal = new AccountPrincipal(account);

	@Mock
	PrincipalService service;

	MockHttpServletRequest request;
	MockHttpServletResponse response;
	AccountRememberMeServices services;

	@BeforeEach
	public void createTokenBasedRememberMeServices() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		services = new AccountRememberMeServices(service);
	}

	@Test
	@DisplayName("should return null when cookies are present in the request")
	public void autoLoginReturnsNullIfNoCookiePresented() {
		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNull();
	}

	@Test
	@DisplayName("should return null when remember-me cookie is not present")
	public void autoLoginIgnoresUnrelatedCookie() {
		request.setCookies(new Cookie("some", "cookie"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNull();
	}

	@Test
	@DisplayName("should return null when remember-me cookie is invalid and clear it")
	public void autoLoginReturnsNullAndClearsCookieIfMissingThreeTokensInCookieValue() {
		request.setCookies(createCookie(encode("x")));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie is not Base64 encoded and clear it")
	public void autoLoginClearsNonBase64EncodedCookie() {
		request.setCookies(createCookie("non-encoded-value"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie is expired and clear it")
	public void autoLoginReturnsNullForExpiredCookieAndClearsCookie() {
		request.setCookies(createCookie(System.currentTimeMillis() - 1000000, principal.getUsername()));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me cookie signature does not match and clear it")
	public void autoLoginClearsCookieIfSignatureBlocksDoesNotMatchExpectedValue() {
		doReturn(principal).when(service).lookup(anyString());

		request.setCookies(createCookie(System.currentTimeMillis() + 1000000, principal.getUsername(), "WRONG_KEY"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when remember-me signing algorithm is different")
	public void autoLoginClearsCookieIfSignatureAlgorithmDoesNotMatch() {
		doReturn(principal).when(service).lookup(anyString());

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
	public void autoLoginClearsCookieIfTokenDoesNotContainANumberInCookieValue() {
		request.setCookies(createCookie(encode("username:NOT_A_NUMBER:signature")));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should return null when user account is not found and clear it")
	public void autoLoginClearsCookieIfUserNotFound() {
		doThrow(UsernameNotFoundException.class).when(service).lookup(anyString());

		request.setCookies(createCookie(System.currentTimeMillis() + 1000000, "not-found"));

		assertThat(services.autoLogin(request, response))
				.isNull();

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should throw internal service error when user service returns null")
	public void autoLoginClearsCookieIfUserServiceMisconfigured() {
		doReturn(null).when(service).lookup(anyString());

		request.setCookies(createCookie(System.currentTimeMillis() + 1000000, principal.getUsername()));

		assertThatThrownBy(() -> this.services.autoLogin(request, response))
				.isInstanceOf(InternalAuthenticationServiceException.class);
	}

	@Test
	@DisplayName("should create authentication for valid cookie")
	public void autoLoginWithValidTokenAndUserSucceeds() {
		doReturn(principal).when(service).lookup(anyString());

		request.setCookies(createCookie(System.currentTimeMillis() + 1000000, principal.getUsername()));

		assertThat(services.autoLogin(request, response))
				.isNotNull()
				.returns(principal, Authentication::getPrincipal)
				.returns(principal.getUsername(), Authentication::getName)
				.returns(principal.getAuthorities(), Authentication::getAuthorities);
	}

	@Test
	@DisplayName("should clear cookie when authentication fails")
	public void loginFailClearsCookie() {
		assertThatNoException().isThrownBy(() -> services.loginFail(request, response));

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(0, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should create cookie even without remember-me parameter set")
	public void loginSuccessWhenParameterNotSetOrFalse() {
		request.addParameter(AccountRememberMeServices.DEFAULT_PARAMETER, "false");

		services.loginSuccess(request, response, createAuthentication(principal));

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNotNull()
				.returns(AccountRememberMeServices.TOKEN_VALIDITY, Cookie::getMaxAge);
	}

	@Test
	@DisplayName("should create cookie with remember-me parameter set")
	public void loginSuccessSetsCookie() {
		request.addParameter(AccountRememberMeServices.DEFAULT_PARAMETER, "on");

		services.loginSuccess(request, response, createAuthentication(principal));

		final var cookie = response.getCookie(AccountRememberMeServices.COOKIE_NAME);

		assertThat(cookie)
				.isNotNull()
				.returns(AccountRememberMeServices.TOKEN_VALIDITY, Cookie::getMaxAge)
				.returns(true, Cookie::getSecure)
				.returns("/", Cookie::getPath)
				.returns(null, Cookie::getDomain);

		assertThat(cookie.getValue())
				.isNotBlank()
				.isBase64();

		assertThat(decode(cookie.getValue()).split(":"))
				.hasSize(3)
				.contains(principal.getUsername(), Index.atIndex(0))
				.satisfies(tokens -> assertThat(Long.parseLong(tokens[1]))
						.isCloseTo(
								System.currentTimeMillis() + AccountRememberMeServices.TOKEN_VALIDITY,
								Offset.offset(300L) // should not take more than 300ms to generate cookie
						)
				)
				.satisfies(tokens -> assertThat(tokens[2])
						.isNotBlank()
						.isHexadecimal()
				);
	}

	@Test
	@DisplayName("should not create cookie without a valid principal in authentication")
	public void loginSuccessWithoutAuthenticationName() {
		final var authentication = new TestingAuthenticationToken(null, "", "authority");
		services.loginSuccess(request, response, authentication);

		assertThat(response.getCookie(AccountRememberMeServices.COOKIE_NAME))
				.isNull();
	}

	@Test
	@DisplayName("should catch unknown signing algorithm exceptions")
	public void shouldCatchUnknownAlgorithmExceptions() {
		assertThatThrownBy(() -> AccountRememberMeServices.generateSignature("test", 1, "key", "unknown-algo"))
				.isInstanceOf(IllegalStateException.class)
				.hasRootCauseInstanceOf(NoSuchAlgorithmException.class);
	}

	@Test
	@DisplayName("should disable setters")
	public void testDisabledSetters() {
		assertThatThrownBy(() -> services.setAlwaysRemember(false))
				.isInstanceOf(UnsupportedOperationException.class);

		assertThatThrownBy(() -> services.setCookieName("cookie name"))
				.isInstanceOf(UnsupportedOperationException.class);

		assertThatThrownBy(() -> services.setUseSecureCookie(true))
				.isInstanceOf(UnsupportedOperationException.class);

		assertThatThrownBy(() -> services.setParameter("parameter name"))
				.isInstanceOf(UnsupportedOperationException.class);

		assertThatThrownBy(() -> services.setTokenValiditySeconds(1238564))
				.isInstanceOf(UnsupportedOperationException.class);

		assertThat(services.getKey())
				.isEqualTo(AccountRememberMeServices.KEY);

		assertThat(services.getParameter())
				.isEqualTo(AccountRememberMeServices.DEFAULT_PARAMETER);
	}

	private static Authentication createAuthentication(UserDetails user) {
		return new TestingAuthenticationToken(user, user.getPassword(), user.getAuthorities());
	}

	private static Cookie createCookie(long expiryTime, String username) {
		return createCookie(expiryTime, username, AccountRememberMeServices.KEY);
	}

	private static Cookie createCookie(long expiryTime, String username, String key) {
		return createCookie(expiryTime, username, key, AccountRememberMeServices.DIGEST_ALGORITHM);
	}

	private static Cookie createCookie(long expiryTime, String username, String key, String algorithm) {
		final String signature = AccountRememberMeServices.generateSignature(username, expiryTime, key, algorithm);
		return createCookie(encode(username + ":" + expiryTime + ":" + signature));
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