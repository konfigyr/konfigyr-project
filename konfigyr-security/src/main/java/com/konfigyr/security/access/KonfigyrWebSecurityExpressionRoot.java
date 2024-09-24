package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import org.springframework.lang.NonNull;
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

	KonfigyrWebSecurityExpressionRoot(Authentication a, FilterInvocation fi) {
		super(a, fi);
	}

	@Override
	public boolean isMember(@NonNull Namespace namespace) {
		return KonfigyrSecurityExpressionOperations.super.isMember(namespace);
	}

	@Override
	public boolean isMember(@NonNull String namespace) {
		return KonfigyrSecurityExpressionOperations.super.isMember(namespace);
	}

	@Override
	public boolean isAdmin(@NonNull Namespace namespace) {
		return KonfigyrSecurityExpressionOperations.super.isAdmin(namespace);
	}

	@Override
	public boolean isAdmin(@NonNull String namespace) {
		return KonfigyrSecurityExpressionOperations.super.isAdmin(namespace);
	}
}
