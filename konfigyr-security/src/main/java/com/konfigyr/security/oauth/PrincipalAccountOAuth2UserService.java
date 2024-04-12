package com.konfigyr.security.oauth;

import com.konfigyr.account.AccountRegistration;
import com.konfigyr.security.PrincipalService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link OAuth2UserService} that would try to resolve the
 * {@link OAuth2User} using the {@link DefaultOAuth2UserService} that would fetch all user
 * attributes from OAuth Authorization server.
 * <p>
 * Once the {@link OAuth2User user attributes} are resolved, it would try to load the
 * {@link com.konfigyr.security.AccountPrincipal}, if one already exists, or it would attempt
 * to register a new one.
 * <p>
 * When registering a new {@link com.konfigyr.security.AccountPrincipal} this service would
 * attempt to create an {@link AccountRegistration} using the resolved {@link OAuth2User} from
 * the OAuth Authorization server.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class PrincipalAccountOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final PrincipalService principalService;
	private final OAuth2UserService<OAuth2UserRequest, ? extends OAuth2User> delegate;

	/**
	 * Creates the {@link PrincipalAccountOAuth2UserService} instance using the {@link PrincipalService}
	 * to retrieve or register new {@link com.konfigyr.account.Account accounts} for resolved {@link OAuth2User}.
	 * <p>
	 * The {@link RestTemplateBuilder} would be used to configure the delegating {@link OAuth2UserService} that
	 * would attempt to fetch and resolved the {@link OAuth2User} attributes.
	 *
	 * @param principalService principal service to resolve accounts, can't be {@literal null}
	 * @param restTemplateBuilder rest template builder to create {@link OAuth2UserService} delegate, can't be {@literal null}
	 */
	public PrincipalAccountOAuth2UserService(PrincipalService principalService, RestTemplateBuilder restTemplateBuilder) {
		this(principalService, createDefaultDelegate(restTemplateBuilder));
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
		final OAuth2User user = delegate.loadUser(request);

		if (user == null) {
			return null;
		}

		return principalService.lookup(user, () -> createAccountRegistration(request, user));
	}

	private AccountRegistration createAccountRegistration(OAuth2UserRequest request, OAuth2User user) {
		final ClientRegistration clientRegistration = request.getClientRegistration();

		final AccountRegistration accountRegistration = OAuth2UserConverters.get(clientRegistration)
				.convert(user);

		Assert.notNull(accountRegistration, "Failed to create account registration for " +
				"OAuth client registration with id: " + clientRegistration.getRegistrationId());

		return accountRegistration;
	}

	private static OAuth2UserService<OAuth2UserRequest, ? extends OAuth2User> createDefaultDelegate(
			RestTemplateBuilder builder) {
		final var operations = builder.errorHandler(new OAuth2ErrorResponseErrorHandler()).build();

		final var delegate = new DefaultOAuth2UserService();
		delegate.setRestOperations(operations);

		return delegate;
	}

}
