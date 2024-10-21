package com.konfigyr.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Utility class that is used to declare {@link RequestMatcher request matchers} for Spring Security.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface SecurityRequestMatchers {

	/**
	 * Path that matches the error page: <code>/error</code>.
	 */
	String ERROR_PAGE = "/error";

	/**
	 * Path that matches the login page: <code>/login</code>.
	 */
	String LOGIN_PAGE = "/login";

	/**
	 * Path that matches the authentication error page: <code>/login?error</code>.
	 * @see com.konfigyr.security.authentication.AuthenticationFailureHandlerBuilder
	 */
	String AUTHENTICATION_ERROR_PAGE = "/login?error";

	/**
	 * Path that matches the account provisioning page: <code>/provision</code>.
	 */
	String PROVISIONING_PAGE = "/provision";

	/**
	 * Request matcher that matches the following ant path pattern: <code>/oauth/**</code>.
	 */
	RequestMatcher OAUTH = AntPathRequestMatcher.antMatcher("/oauth/**");

	/**
	 * Request matcher that matches our {@link #LOGIN_PAGE} and {@link HttpMethod#GET} method.
	 */
	RequestMatcher OAUTH_LOGIN = AntPathRequestMatcher.antMatcher(HttpMethod.GET, LOGIN_PAGE);

	/**
	 * Request matcher that contains patterns that should be ignored by Spring security.
	 * */
	RequestMatcher STATIC_ASSETS = new OrRequestMatcher(
			AntPathRequestMatcher.antMatcher("/assets/**"),
			AntPathRequestMatcher.antMatcher("/favicon.ico")
	);

}
