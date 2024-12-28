package com.konfigyr.identity.authentication.controller;

import com.konfigyr.identity.KonfigyrIdentityRequestMatchers;
import com.konfigyr.web.error.OAuth2ErrorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping(KonfigyrIdentityRequestMatchers.LOGIN_PAGE)
class LoginController {

	private static final UriComponents REQUEST_PATTERN = UriComponentsBuilder
			.fromPath(DefaultServerOAuth2AuthorizationRequestResolver.DEFAULT_AUTHORIZATION_REQUEST_PATTERN)
			.build();

	private final OAuth2ClientProperties properties;
	private final ClientRegistrationRepository repository;

	@GetMapping
	String login(
			@NonNull Model model,
			@NonNull WebRequest request,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout
	) {
		final Set<AuthenticationOption> options = properties.getRegistration()
				.keySet()
				.stream()
				.map(repository::findByRegistrationId)
				.map(AuthenticationOption::from)
				.collect(Collectors.toUnmodifiableSet());

		model.addAttribute("options", options)
				.addAttribute("logout", logout != null)
				.addAttribute("error", error != null ? extractError(request) : null);

		return "login";
	}

	@Nullable
	static OAuth2Error extractError(@NonNull WebRequest request) {
		return OAuth2ErrorResolver.getInstance().resolve(request);
	}

	/**
	 * Record used by the template to render the authentication option based on the {@link ClientRegistration}.
	 *
	 * @param id   unique option identifier, usually the {@link ClientRegistration#getRegistrationId()}
	 * @param name display name of the option, usually the {@link ClientRegistration#getClientName()}
	 * @param url  location where the authentication would start
	 */
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
