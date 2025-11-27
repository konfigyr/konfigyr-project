package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Konfigyr web security expression root object which contains {@link com.konfigyr.account.Memberships} Spring
 * Security expression checks.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see KonfigyrSecurityExpressionOperations
 * @see KonfigyrMethodSecurityExpressionHandler
 **/
class KonfigyrWebSecurityExpressionRoot extends SecurityExpressionRoot<FilterInvocation>
		implements KonfigyrSecurityExpressionOperations {

	private final AccessService accessService;
	private final HttpServletRequest request;

	KonfigyrWebSecurityExpressionRoot(AccessService accessService, Authentication a, FilterInvocation fi) {
		super(() -> a, fi);
		this.accessService = accessService;
		this.request = fi.getRequest();
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

	/**
	 * Takes a specific IP address or a range using the IP/Netmask (e.g. 192.168.1.0/24 or 202.24.0.0/14).
	 *
	 * @param ipAddress the address or range of addresses from which the request must come.
	 * @return true if the IP address of the current request is in the required range.
	 */
	public boolean hasIpAddress(String ipAddress) {
		final IpAddressMatcher matcher = new IpAddressMatcher(ipAddress);
		return matcher.matches(request);
	}

}
