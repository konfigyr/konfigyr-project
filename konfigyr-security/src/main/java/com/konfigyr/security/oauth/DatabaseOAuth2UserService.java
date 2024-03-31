package com.konfigyr.security.oauth;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountRegistration;
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
 * Once the attributes are resolved, it would try to load the user account from the
 * database or create a new one if needed. The returned {@link OAuth2User} implementation
 * would be decorated with attributes from the loaded or created {@link Account}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class DatabaseOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final AccountManager accountManager;
	private final OAuth2UserService<OAuth2UserRequest, ? extends OAuth2User> delegate;

	public DatabaseOAuth2UserService(AccountManager accountManager, RestTemplateBuilder restTemplateBuilder) {
		this(accountManager, createDefaultDelegate(restTemplateBuilder));
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
		final OAuth2User user = delegate.loadUser(request);

		if (user == null) {
			return null;
		}

		final Account account = accountManager.findByEmail(user.getName())
			.orElseGet(() -> createUserAccount(request.getClientRegistration(), user));

		Assert.notNull(account.id(), "User account identifier can not be null");
		Assert.hasText(account.email(), "User account needs to have an email address set");

		return new OAuth2UserAccount(account);
	}

	private Account createUserAccount(ClientRegistration clientRegistration, OAuth2User user) {
		final AccountRegistration accountRegistration = OAuth2UserConverters.get(clientRegistration)
				.convert(user);

		Assert.notNull(accountRegistration, "Failed to create account registration for " +
				"OAuth client registration with id: " + clientRegistration.getRegistrationId());

		return accountManager.create(accountRegistration);
	}

	private static OAuth2UserService<OAuth2UserRequest, ? extends OAuth2User> createDefaultDelegate(
			RestTemplateBuilder builder) {
		final var operations = builder.errorHandler(new OAuth2ErrorResponseErrorHandler()).build();

		final var delegate = new DefaultOAuth2UserService();
		delegate.setRestOperations(operations);

		return delegate;
	}

}
