
package com.konfigyr.security.login;

import com.konfigyr.security.SecurityRequestMatchers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.WebAttributes;
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
 * Controller that renders the Spring Security login page with available authentication options.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Controller
@RequiredArgsConstructor
public class LoginSecurityController {

	private static final UriComponents REQUEST_PATTERN = UriComponentsBuilder
			.fromPath(DefaultServerOAuth2AuthorizationRequestResolver.DEFAULT_AUTHORIZATION_REQUEST_PATTERN)
			.build();

	private final OAuth2ClientProperties properties;
	private final ClientRegistrationRepository repository;

	@GetMapping(SecurityRequestMatchers.LOGIN_PAGE)
	String login(
			@NonNull Model model,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout,
			HttpServletRequest request
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

	static OAuth2Error extractError(@NonNull HttpServletRequest request) {
		final HttpSession session = request.getSession(false);
		Object attribute = null;

		if (session != null) {
			attribute = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}

		if (attribute == null) {
			attribute = request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}

		if (attribute instanceof OAuth2AuthenticationException ex) {
			return ex.getError();
		}

		if (attribute instanceof OAuth2AuthorizationException ex) {
			return ex.getError();
		}

		return new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "Unexpected server occurred while logging you in.", null);
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
