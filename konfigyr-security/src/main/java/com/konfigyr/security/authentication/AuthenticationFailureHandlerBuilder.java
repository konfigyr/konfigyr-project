package com.konfigyr.security.authentication;

import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;

/**
 * Builder class used to create a {@link DelegatingAuthenticationFailureHandler} that can be used by different
 * {@link org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter authentication filters}
 * to handle {@link AuthenticationException authentication exceptions}.
 * <p>
 * The builder would return a failure handler that uses the {@link SimpleUrlAuthenticationFailureHandler}
 * as the default one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see DelegatingAuthenticationFailureHandler
 * @see SimpleUrlAuthenticationFailureHandler
 **/
public class AuthenticationFailureHandlerBuilder {

	private final LinkedHashMap<Class<? extends AuthenticationException>, AuthenticationFailureHandler> handlers;
	private final String defaultHandlerRedirectUrl;

	/**
	 * Creates a new builder with a URL to send users if authentication fails redirect when no
	 * specific failure handler strategy is found.
	 *
	 * @param failureUrl redirect URL
	 */
	public AuthenticationFailureHandlerBuilder(String failureUrl) {
		Assert.hasText(failureUrl, "Failure URL can not be blank");

		this.handlers = new LinkedHashMap<>();
		this.defaultHandlerRedirectUrl = failureUrl;
	}

	/**
	 * Registers a new {@link SimpleUrlAuthenticationFailureHandler} that would be invoked when this
	 * {@link AuthenticationException} type is thrown by the authentication filters that would redirect
	 * the user to the given failure URL.
	 *
	 * @param type exception type for which the handler should be invoked, can't be {@literal null}
	 * @param failureUrl the failure URL used by the handler, can't be {@literal null}
	 * @return Authentication failure handler builder, never {@literal null}
	 */
	public AuthenticationFailureHandlerBuilder register(
			@NonNull Class<? extends AuthenticationException> type,
			@NonNull String failureUrl
	) {
		return this.register(type, createSimpleAuthenticationFailureHandler(failureUrl));
	}

	/**
	 * Registers a new {@link AuthenticationFailureHandler} that would be invoked when this
	 * {@link AuthenticationException} type is thrown by the authentication filters.
	 *
	 * @param type exception type for which the handler should be invoked, can't be {@literal null}
	 * @param handler failure handler that should be invoked for the exception type, can't be {@literal null}
	 * @return Authentication failure handler builder, never {@literal null}
	 */
	public AuthenticationFailureHandlerBuilder register(
			@NonNull Class<? extends AuthenticationException> type,
			@NonNull AuthenticationFailureHandler handler
	) {
		if (this.handlers.containsKey(type)) {
			throw new IllegalArgumentException("Authentication failure handler for exception type '" + type +
					"' already exists. You attempted to override it with: " + handler);
		}

		this.handlers.put(type, handler);
		return this;
	}

	/**
	 * Creates the {@link AuthenticationFailureHandler} that uses a strategy pattern when it comes
	 * when handling different {@link AuthenticationException authentication exceptions}.
	 *
	 * @return delegating authentication failure handler, never {@literal null}
	 * @see DelegatingAuthenticationFailureHandler
	 */
	@NonNull
	public AuthenticationFailureHandler build() {
		if (CollectionUtils.isEmpty(handlers)) {
			return createSimpleAuthenticationFailureHandler(defaultHandlerRedirectUrl);
		}

		return new DelegatingAuthenticationFailureHandler(handlers,
				createSimpleAuthenticationFailureHandler(defaultHandlerRedirectUrl));
	}

	static AuthenticationFailureHandler createSimpleAuthenticationFailureHandler(String redirectUrl) {
		Assert.hasText(redirectUrl, "Failure redirect URL can not be blank");

		final SimpleUrlAuthenticationFailureHandler defaultHandler = new SimpleUrlAuthenticationFailureHandler();
		defaultHandler.setDefaultFailureUrl(redirectUrl);
		defaultHandler.setAllowSessionCreation(true);
		defaultHandler.setUseForward(false);
		return defaultHandler;
	}
}
