package com.konfigyr.identity;


import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Interface that contains Spring request mappings and {@link RequestMatcher matchers}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@SuppressWarnings("checkstyle:InterfaceIsType")
public interface KonfigyrIdentityRequestMatchers {

	/**
	 * Path that matches the login page: <code>/login</code>.
	 */
	String LOGIN_PAGE = "/login";

	/**
	 * Path that matches the OAuth 2.0 Consents page: <code>/oauth/consents</code>.
	 */
	String CONSENTS_PAGE = "/oauth/consents";

}
