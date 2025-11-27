package com.konfigyr.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Implementation of the {@link RequestMatcher} that would be used by the {@link CsrfFilter} to check
 * if CSRF protection is required or not for the incoming {@link HttpServletRequest}.
 * <p>
 * The CSRF checks should be performed for requests that do not use Bearer Token authentication, such as
 * Basic, Digest or Cookie based authentication.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class CsrfRequestMatcher implements RequestMatcher {

	private final BearerTokenResolver bearerTokenResolver;
	private final RequestMatcher delegate;

	/**
	 * Create a new instance of the {@link CsrfRequestMatcher} with the given {@link BearerTokenResolver}
	 * and a {@link CsrfFilter#DEFAULT_CSRF_MATCHER default CSRF matcher}.
	 *
	 * @param bearerTokenResolver resolver to check if Bearer Authentication Token is present in the request
	 *                            headers, can't be {@literal null}
	 */
	public CsrfRequestMatcher(BearerTokenResolver bearerTokenResolver) {
		this(bearerTokenResolver, CsrfFilter.DEFAULT_CSRF_MATCHER);
	}

	@Override
	public boolean matches(@NonNull HttpServletRequest request) {
		if (containsBearerToken(request)) {
			return false;
		}

		return delegate.matches(request);
	}

	private boolean containsBearerToken(HttpServletRequest request) {
		try {
			return this.bearerTokenResolver.resolve(request) != null;
		} catch (OAuth2AuthenticationException ex) {
			return false;
		}
	}
}
