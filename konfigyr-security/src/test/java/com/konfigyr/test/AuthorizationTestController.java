package com.konfigyr.test;

import com.konfigyr.namespace.Namespace;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class used to test {@link PreAuthorize} annotations with our custom implementation of
 * the {@link org.springframework.security.access.expression.SecurityExpressionRoot}.
 *
 * @author Vladimir Spasic
 **/
@RestController
public class AuthorizationTestController {

	@NonNull
	@PreAuthorize("isMember(#namespace)")
	public String members(@NonNull Namespace namespace) {
		return members(namespace.name());
	}

	@NonNull
	@PreAuthorize("isMember(#namespace)")
	@GetMapping("/authorization/{namespace}/members")
	public String members(@PathVariable @NonNull String namespace) {
		return namespace;
	}

	@NonNull
	@PreAuthorize("isAdmin(#namespace)")
	public String admins(@NonNull Namespace namespace) {
		return admins(namespace.name());
	}

	@NonNull
	@PreAuthorize("isAdmin(#namespace)")
	@GetMapping("/authorization/{namespace}/admins")
	public String admins(@PathVariable @NonNull String namespace) {
		return namespace;
	}

}
