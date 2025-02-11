package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

/**
 * Konfigyr method security expression root object which contains {@link com.konfigyr.account.Memberships} Spring
 * Security expression checks.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see KonfigyrSecurityExpressionOperations
 * @see KonfigyrMethodSecurityExpressionHandler
 **/
@Getter
@Setter
class KonfigyrMethodSecurityExpressionRoot extends SecurityExpressionRoot
		implements KonfigyrSecurityExpressionOperations, MethodSecurityExpressionOperations {

	private final AccessService accessService;

	private Object filterObject;
	private Object returnObject;
	private Object target;

	KonfigyrMethodSecurityExpressionRoot(AccessService accessService, Authentication authentication) {
		this(accessService, () -> authentication);
	}

	KonfigyrMethodSecurityExpressionRoot(AccessService accessService, Supplier<Authentication> authentication) {
		super(authentication);
		this.accessService = accessService;
	}

	@Override
	public AuthorizationResult isMember(@NonNull Namespace namespace) {
		return KonfigyrSecurityExpressionOperations.super.isMember(namespace);
	}

	@Override
	public AuthorizationResult isMember(@NonNull String namespace) {
		return accessService.hasAccess(getAuthentication(), namespace);
	}

	@Override
	public AuthorizationResult isAdmin(@NonNull Namespace namespace) {
		return KonfigyrSecurityExpressionOperations.super.isAdmin(namespace);
	}

	@Override
	public AuthorizationResult isAdmin(@NonNull String namespace) {
		return accessService.hasAccess(getAuthentication(), namespace, NamespaceRole.ADMIN);
	}

	@Override
	public Object getThis() {
		return this.target;
	}

}
