package com.konfigyr.security.access;

import com.konfigyr.namespace.Namespace;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * Class used to test {@link PreAuthorize} annotations with our custom implementation of
 * the {@link org.springframework.security.access.expression.SecurityExpressionRoot}.
 *
 * @author Vladimir Spasic
 **/
@RestController
class AuthorizationTestController {

	@NonNull
	@PreAuthorize("isMember(#namespace)")
	String members(@NonNull Namespace namespace) {
		return members(namespace.name());
	}

	@NonNull
	@PreAuthorize("isMember(#namespace)")
	@GetMapping("/authorization/{namespace}/members")
	String members(@PathVariable @NonNull String namespace) {
		return HtmlUtils.htmlEscape(namespace);
	}

	@NonNull
	@PreAuthorize("isAdmin(#namespace)")
	String admins(@NonNull Namespace namespace) {
		return admins(namespace.name());
	}

	@NonNull
	@PreAuthorize("isAdmin(#namespace)")
	@GetMapping("/authorization/{namespace}/admins")
	String admins(@PathVariable @NonNull String namespace) {
		return HtmlUtils.htmlEscape(namespace);
	}

}
