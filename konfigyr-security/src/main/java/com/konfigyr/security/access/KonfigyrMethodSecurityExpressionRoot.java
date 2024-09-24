package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
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

	private Object filterObject;
	private Object returnObject;
	private Object target;

	KonfigyrMethodSecurityExpressionRoot(Authentication authentication) {
		this(() -> authentication);
	}

	KonfigyrMethodSecurityExpressionRoot(Supplier<Authentication> authentication) {
		super(authentication);
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

	@Override
	public Object getThis() {
		return this.target;
	}

}
