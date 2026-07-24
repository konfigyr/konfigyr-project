package com.konfigyr.namespace.controller;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.namespace.dashboard.DashboardSummary;
import com.konfigyr.namespace.dashboard.Dashboards;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces/{slug}")
class DashboardController {

	private final NamespaceManager namespaces;
	private final Dashboards dashboards;

	@GetMapping("/dashboard")
	@PreAuthorize("isMember(#slug)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	DashboardSummary get(@PathVariable String slug) {
		final Namespace namespace = namespaces.findBySlug(slug)
				.orElseThrow(() -> new NamespaceNotFoundException(slug));

		return dashboards.summary(namespace);
	}

}
