package com.konfigyr.identity.authorization.controller;

import com.konfigyr.identity.KonfigyrIdentityRequestMatchers;
import com.konfigyr.identity.authorization.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping(KonfigyrIdentityRequestMatchers.CONSENTS_PAGE)
class AuthorizationConsentController {

	private final AuthorizationService service;
	private final RegisteredClientRepository repository;

	@GetMapping
	String consent(
			@RequestParam(name = "scope") String scope,
			@RequestParam(name = "state") String state,
			@RequestParam(name = "client_id") String clientId,
			@NonNull Model model,
			@NonNull Authentication authentication
	) {

		final RegisteredClient client = repository.findById(clientId);
		Assert.notNull(client, "OAuth 2.0 client registration not found");

		final OAuth2AuthorizationConsent consent = service.findById(clientId, authentication.getName());
		final Set<AuthorizedScope> scopes = AuthorizedScope.from(scope, consent);

		model.addAttribute("client", client)
				.addAttribute("consent", consent)
				.addAttribute("scope", scope)
				.addAttribute("state", state)
				.addAttribute("scopes", scopes);

		return "consents";
	}

}
