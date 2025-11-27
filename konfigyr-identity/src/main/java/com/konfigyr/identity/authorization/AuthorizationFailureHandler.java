package com.konfigyr.identity.authorization;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ForwardAuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link AuthenticationFailureHandler} that would handle {@link OAuth2AuthenticationException}
 * by either forward the request to the OAuth error page or perform the redirect to the OAuth Client that initiated
 * the OAuth authorization.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
public class AuthorizationFailureHandler implements AuthenticationFailureHandler {

	/**
	 * The URL of the OAuth error page where this handler is going to forward the requests
	 * when <code>redirect_uri</code> can not be resolved from the {@link AuthenticationException}.
	 */
	public static final String OAUTH_ERROR_PAGE = "/oauth/error";

	private final AuthenticationFailureHandler delegate = new ForwardAuthenticationFailureHandler(OAUTH_ERROR_PAGE);
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void onAuthenticationFailure(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull AuthenticationException exception
	) throws IOException, ServletException {
		final OAuth2AuthorizationCodeRequestAuthenticationToken authenticationToken = resolveAuthentication(exception);

		if (authenticationToken == null || StringUtils.isBlank(authenticationToken.getRedirectUri())) {
			delegate.onAuthenticationFailure(request, response, exception);
			return;
		}

		final OAuth2Error error = resolveError(exception);

		if (error == null) {
			delegate.onAuthenticationFailure(request, response, exception);
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("Redirecting to OAuth client with error: {}", error);
		}

		final UriComponentsBuilder uriBuilder = UriComponentsBuilder
				.fromUriString(authenticationToken.getRedirectUri())
				.queryParam(OAuth2ParameterNames.ERROR, error.getErrorCode());

		if (StringUtils.isNotBlank(error.getDescription())) {
			uriBuilder.queryParam(OAuth2ParameterNames.ERROR_DESCRIPTION,
					UriUtils.encode(error.getDescription(), StandardCharsets.UTF_8));
		}
		if (StringUtils.isNotBlank(error.getUri())) {
			uriBuilder.queryParam(OAuth2ParameterNames.ERROR_URI,
					UriUtils.encode(error.getUri(), StandardCharsets.UTF_8));
		}
		if (StringUtils.isNotBlank(authenticationToken.getState())) {
			uriBuilder.queryParam(OAuth2ParameterNames.STATE,
					UriUtils.encode(authenticationToken.getState(), StandardCharsets.UTF_8));
		}

		// build(true) -> Components are explicitly encoded
		final String redirectUri = uriBuilder.build(true).toUriString();
		this.redirectStrategy.sendRedirect(request, response, redirectUri);
	}

	@Nullable
	private static OAuth2AuthorizationCodeRequestAuthenticationToken resolveAuthentication(AuthenticationException exception) {
		if (exception instanceof OAuth2AuthorizationCodeRequestAuthenticationException ex) {
			return ex.getAuthorizationCodeRequestAuthentication();
		}

		return null;
	}

	@Nullable
	private static OAuth2Error resolveError(AuthenticationException exception) {
		if (exception instanceof OAuth2AuthenticationException ex) {
			return ex.getError();
		}
		return null;
	}

}
