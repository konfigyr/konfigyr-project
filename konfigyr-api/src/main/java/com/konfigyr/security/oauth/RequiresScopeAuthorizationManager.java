package com.konfigyr.security.oauth;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An {@link AuthorizationManager} that determines if the current {@link Authentication} is authorized by
 * evaluating if the {@link Authentication} contains any of the required {@link OAuthScopes} defined by the
 * {@link RequiresScope}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuthScopes
 * @see RequiresScope
 */
class RequiresScopeAuthorizationManager implements AuthorizationManager<MethodInvocation> {

	private final Map<MethodClassKey, OAuthScopes> cache = new ConcurrentHashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public AuthorizationDecision check(Supplier<Authentication> authentication, MethodInvocation invocation) {
		final OAuthScopes scopes = resolveRequiredScopes(invocation);

		if (scopes.isEmpty()) {
			return null;
		}

		final boolean granted = isAuthorized(authentication.get(), scopes);

		return new AuthorityAuthorizationDecision(granted, (Collection<GrantedAuthority>) scopes.toAuthorities());
	}

	@NonNull
	private OAuthScopes resolveRequiredScopes(@NonNull MethodInvocation invocation) {
		final MethodClassKey key = createCacheKey(invocation);
		OAuthScopes scopes = cache.get(key);

		if (scopes == null) {
			scopes = createRequiredScopes(invocation);
			cache.put(key, scopes);
		}

		return scopes;
	}

	private static boolean isAuthorized(Authentication authentication, OAuthScopes scopes) {
		if (authentication == null) {
			return false;
		}

		for (final GrantedAuthority authority : authentication.getAuthorities()) {
			if (scopes.permits(authority)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private static OAuthScopes createRequiredScopes(@NonNull MethodInvocation invocation) {
		MergedAnnotation<RequiresScope> annotation = lookupAnnotation(invocation.getMethod());

		if (annotation == null) {
			annotation = lookupAnnotation(invocation.getMethod().getDeclaringClass());
		}

		if (annotation == null) {
			return OAuthScopes.empty();
		}

		return OAuthScopes.of(annotation.getEnumArray("value", OAuthScope.class));
	}

	@NonNull
	private static MethodClassKey createCacheKey(@NonNull MethodInvocation invocation) {
		final Object target = invocation.getThis();
		final Class<?> targetClass = (target != null) ? target.getClass() : null;
		return new MethodClassKey(invocation.getMethod(), targetClass);
	}

	@Nullable
	private static MergedAnnotation<RequiresScope> lookupAnnotation(@NonNull AnnotatedElement element) {
		final MergedAnnotations annotations = MergedAnnotations.from(element,
				MergedAnnotations.SearchStrategy.DIRECT, RepeatableContainers.none());

		final MergedAnnotation<RequiresScope> annotation = annotations.get(RequiresScope.class);
		return annotation.isPresent() ? annotation : null;
	}
}
