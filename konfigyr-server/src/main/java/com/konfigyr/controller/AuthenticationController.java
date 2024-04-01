
package com.konfigyr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller that handles Spring Security authentication endpoints.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Controller
@RequiredArgsConstructor
public class AuthenticationController {

	private static final UriComponents REQUEST_PATTERN = UriComponentsBuilder
		.fromPath(DefaultServerOAuth2AuthorizationRequestResolver.DEFAULT_AUTHORIZATION_REQUEST_PATTERN)
		.build();

	private final OAuth2ClientProperties properties;

	private final ClientRegistrationRepository repository;

	@GetMapping("/login")
	String login(@NonNull Model model, @RequestParam(value = "logout",	required = false) String logout) {
		final Set<AuthenticationOption> options = properties.getRegistration()
			.keySet()
			.stream()
			.map(repository::findByRegistrationId)
			.map(AuthenticationOption::from)
			.collect(Collectors.toUnmodifiableSet());

		model.addAttribute("options", options);
		model.addAttribute("logout", logout != null);

		return "login";
	}

	record AuthenticationOption(String id, String name, URI url) {

		static AuthenticationOption from(ClientRegistration registration) {
			final URI uri = REQUEST_PATTERN.expand(
					Map.of(DefaultServerOAuth2AuthorizationRequestResolver.DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME,
							registration.getRegistrationId()))
				.toUri();

			return new AuthenticationOption(registration.getRegistrationId(), registration.getClientName(), uri);
		}

	}

}
