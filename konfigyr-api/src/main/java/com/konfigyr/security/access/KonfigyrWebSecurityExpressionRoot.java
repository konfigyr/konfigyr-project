package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import org.springframework.lang.NonNull;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

/**
 * Konfigyr web security expression root object which contains {@link com.konfigyr.account.Memberships} Spring
 * Security expression checks.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see KonfigyrSecurityExpressionOperations
 * @see KonfigyrMethodSecurityExpressionHandler
 **/
class KonfigyrWebSecurityExpressionRoot extends WebSecurityExpressionRoot
		implements KonfigyrSecurityExpressionOperations {

	private final AccessService accessService;

	KonfigyrWebSecurityExpressionRoot(AccessService accessService, Authentication a, FilterInvocation fi) {
		super(a, fi);
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

}
