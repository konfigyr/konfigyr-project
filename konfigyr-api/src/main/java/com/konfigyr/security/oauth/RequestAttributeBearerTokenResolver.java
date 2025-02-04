package com.konfigyr.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Implementation of the {@link BearerTokenResolver} that uses HTTP request attributes to cache the Bearer Tokens
 * that are resolved using delegating resolver implementations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RequestAttributeBearerTokenResolver implements BearerTokenResolver {

	static final String REQUEST_ATTRIBUTE_NAME = RequestAttributeBearerTokenResolver.class.getName();

	private final BearerTokenResolver delegate;

	/**
	 * Creates a new instance of the {@link RequestAttributeBearerTokenResolver} that uses the
	 * {@link DefaultBearerTokenResolver} to resolve Bearer Tokens from HTTP requests.
	 *
	 * @see DefaultBearerTokenResolver
	 */
	public RequestAttributeBearerTokenResolver() {
		this(new DefaultBearerTokenResolver());
	}

	@Override
	public String resolve(HttpServletRequest request) {
		String token = (String) request.getAttribute(REQUEST_ATTRIBUTE_NAME);

		if (token == null) {
			token = delegate.resolve(request);

			if (token != null) {
				if (log.isTraceEnabled()) {
					log.trace("Successfully resolved Bearer Token, storing it as {} request attribute", REQUEST_ATTRIBUTE_NAME);
				}

				request.setAttribute(REQUEST_ATTRIBUTE_NAME, token);
			}
		}

		return token;
	}
}
