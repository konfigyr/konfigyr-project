package com.konfigyr.web.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.WebAttributes;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.function.Supplier;

/**
 * Resolver that is used to extract the {@link OAuth2Error} from the request or {@link Throwable}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuth2Error
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class OAuth2ErrorResolver {

	private static final Supplier<OAuth2ErrorResolver> instance = SingletonSupplier.of(OAuth2ErrorResolver::new);

	/**
	 * Returns the {@link OAuth2ErrorResolver} instance to resolve {@link OAuth2Error OAuth 2.0. Errors}.
	 *
	 * @return the OAuth 2.0 Error resolver, never {@literal null}
	 */
	@NonNull
	public static OAuth2ErrorResolver getInstance() {
		return instance.get();
	}

	/**
	 * Attempts to resolve the {@link OAuth2Error} that is present in the {@link HttpServletRequest}.
	 *
	 * @param request HTTP servlet request that may contain an {@link OAuth2Error}, can't be {@literal null}
	 * @return the resolved {@link OAuth2Error} or {@literal null}
	 */
	@Nullable
	public OAuth2Error resolve(@NonNull HttpServletRequest request) {
		return resolve(new ServletWebRequest(request));
	}

	/**
	 * Attempts to resolve the {@link OAuth2Error} that is present in the {@link WebRequest}.
	 *
	 * @param request request that may contain an {@link OAuth2Error}, can't be {@literal null}
	 * @return the resolved {@link OAuth2Error} or {@literal null}
	 */
	@Nullable
	public OAuth2Error resolve(@NonNull WebRequest request) {
		OAuth2Error error = extract(request, RequestAttributes.SCOPE_REQUEST);

		if (error == null) {
			error = extract(request, RequestAttributes.SCOPE_SESSION);
		}

		return error;
	}

	/**
	 * Attempts to resolve the {@link OAuth2Error} from the given {@link Throwable}. Usually the Spring
	 * OAuth 2.0 Security API stores the {@link OAuth2Error} in one of the following exception types.
	 * <ul>
	 *     <li>{@link OAuth2AuthenticationException}</li>
	 *     <li>{@link OAuth2AuthorizationException}</li>
	 * </ul>
	 *
	 * @param ex exception that may contain an {@link OAuth2Error}, can be {@literal null}
	 * @return the resolved {@link OAuth2Error} or {@literal null}
	 */
	@Nullable
	public OAuth2Error resolve(@Nullable Throwable ex) {
		return switch (ex) {
			case OAuth2AuthenticationException exception -> exception.getError();
			case OAuth2AuthorizationException exception -> exception.getError();
			case null, default -> null;
		};
	}

	@Nullable
	private OAuth2Error extract(@NonNull WebRequest request, @NonNull int scope) {
		final Object value = request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, scope);

		if (value instanceof Throwable ex) {
			return resolve(ex);
		}

		return null;
	}

}
