package com.konfigyr.security.oauth;

import com.konfigyr.account.Account;
import com.konfigyr.security.AccountPrincipal;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestAccounts;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.*;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2AuthorizedClientServiceTest extends AbstractIntegrationTest {

	static final String CLIENT_REGISTRATION = "konfigyr-test";

	@Autowired
	ClientRegistrationRepository registrationRepository;

	@Autowired
	OAuth2AuthorizedClientService authorizedClientService;

	@Test
	@DisplayName("should create, retrieve and remove OAuth2 authorized clients for accounts")
	void shouldManageAuthorizedClients() {
		final var accessToken = createAccessToken();
		final var refreshToken = createRefreshToken();
		final var account = TestAccounts.john().build();
		final var authentication = authentication(account);

		final var client = new OAuth2AuthorizedClient(
				registrationRepository.findByRegistrationId(CLIENT_REGISTRATION),
				authentication.getName(),
				accessToken,
				refreshToken
		);

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNull();

		assertThatNoException()
				.isThrownBy(() -> authorizedClientService.saveAuthorizedClient(client, authentication));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNotNull()
				.isInstanceOf(OAuth2AuthorizedClient.class)
				.asInstanceOf(InstanceOfAssertFactories.type(OAuth2AuthorizedClient.class))
				.returns(client.getClientRegistration(), OAuth2AuthorizedClient::getClientRegistration)
				.returns(account.id().serialize(), OAuth2AuthorizedClient::getPrincipalName)
				.satisfies(
						it -> assertThat(it.getAccessToken())
								.returns(accessToken.getTokenType(), OAuth2AccessToken::getTokenType)
								.returns(accessToken.getTokenValue(), AbstractOAuth2Token::getTokenValue)
								.returns(accessToken.getScopes(), OAuth2AccessToken::getScopes)
								.satisfies(t -> assertThat(t.getIssuedAt())
										.isNotNull()
										.isCloseTo(accessToken.getIssuedAt(), within(1, ChronoUnit.SECONDS)))
								.satisfies(t -> assertThat(t.getExpiresAt())
										.isNotNull()
										.isCloseTo(accessToken.getExpiresAt(), within(1, ChronoUnit.SECONDS)))
				)
				.satisfies(
						it -> assertThat(it.getRefreshToken())
								.returns(refreshToken.getTokenValue(), AbstractOAuth2Token::getTokenValue)
								.satisfies(t -> assertThat(t.getIssuedAt())
										.isNotNull()
										.isCloseTo(refreshToken.getIssuedAt(), within(1, ChronoUnit.SECONDS)))
								.satisfies(t -> assertThat(t.getExpiresAt())
										.isNotNull()
										.isCloseTo(refreshToken.getExpiresAt(), within(1, ChronoUnit.SECONDS)))
				);

		assertThatNoException()
				.isThrownBy(() -> authorizedClientService.removeAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNull();
	}

	@Test
	@DisplayName("should create, retrieve and remove OAuth2 authorized clients without refresh token")
	void shouldManageAuthorizedClientsWithoutRefreshToken() {
		final var accessToken = createAccessToken();
		final var account = TestAccounts.john().build();
		final var authentication = authentication(account);

		final var client = new OAuth2AuthorizedClient(
				registrationRepository.findByRegistrationId(CLIENT_REGISTRATION),
				authentication.getName(),
				accessToken
		);

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNull();

		assertThatNoException()
				.isThrownBy(() -> authorizedClientService.saveAuthorizedClient(client, authentication));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNotNull()
				.isInstanceOf(OAuth2AuthorizedClient.class)
				.asInstanceOf(InstanceOfAssertFactories.type(OAuth2AuthorizedClient.class))
				.returns(client.getClientRegistration(), OAuth2AuthorizedClient::getClientRegistration)
				.returns(account.id().serialize(), OAuth2AuthorizedClient::getPrincipalName)
				.returns(null, OAuth2AuthorizedClient::getRefreshToken)
				.satisfies(
						it -> assertThat(it.getAccessToken())
								.returns(accessToken.getTokenType(), OAuth2AccessToken::getTokenType)
								.returns(accessToken.getTokenValue(), AbstractOAuth2Token::getTokenValue)
								.returns(accessToken.getScopes(), OAuth2AccessToken::getScopes)
								.satisfies(t -> assertThat(t.getIssuedAt())
										.isNotNull()
										.isCloseTo(accessToken.getIssuedAt(), within(1, ChronoUnit.SECONDS)))
								.satisfies(t -> assertThat(t.getExpiresAt())
										.isNotNull()
										.isCloseTo(accessToken.getExpiresAt(), within(1, ChronoUnit.SECONDS)))
				);

		assertThatNoException()
				.isThrownBy(() -> authorizedClientService.removeAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNull();
	}

	@Test
	@DisplayName("should fail to load OAuth2 authorized clients when principal name is invalid")
	void shouldFailToLoadAuthorizedClientForInvalidPrincipalName() {
		assertThatThrownBy(() -> authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, "principal"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid external entity identifier value");
	}

	@Test
	@DisplayName("should fail to load OAuth2 authorized clients when client registration is unknown")
	void shouldFailToLoadAuthorizedClientForUnknownRegistration() {
		final Account account = TestAccounts.jane().build();

		assertThatThrownBy(() -> authorizedClientService.loadAuthorizedClient("test-client", account.id().serialize()))
				.isInstanceOf(OAuth2AuthenticationException.class)
				.hasNoCause()
				.extracting("error.errorCode")
				.isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT);
	}

	@Test
	@DisplayName("should fail to save OAuth2 authorized clients when principal name is invalid")
	void shouldFailToSaveAuthorizedClientForInvalidPrincipalName() {
		final var client = new OAuth2AuthorizedClient(
				registrationRepository.findByRegistrationId(CLIENT_REGISTRATION),
				"principal",
				createAccessToken()
		);

		final var authentication = new AnonymousAuthenticationToken("hash", client.getPrincipalName(),
				AuthorityUtils.createAuthorityList("anonymous"));

		assertThatThrownBy(() -> authorizedClientService.saveAuthorizedClient(client, authentication))
				.isInstanceOf(InternalAuthenticationServiceException.class)
				.hasMessageContaining("Failed to resolve user account identifier from OAuth authentication");
	}

	@Test
	@DisplayName("should fail to remove OAuth2 authorized clients when principal name is invalid")
	void shouldFailToRemoveAuthorizedClientForInvalidPrincipalName() {
		assertThatThrownBy(() -> authorizedClientService.removeAuthorizedClient(CLIENT_REGISTRATION, "principal"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid external entity identifier value");
	}

	static OAuth2AccessToken createAccessToken() {
		return OAuth2AccessTokens.createAccessToken("access-token");
	}

	static OAuth2RefreshToken createRefreshToken() {
		return OAuth2AccessTokens.createRefreshToken("refresh-token");
	}

	static @NonNull Authentication authentication(@NonNull Account account) {
		final var oauth = AccountPrincipal.from(account);
		return new OAuth2AuthenticationToken(oauth, oauth.getAuthorities(), CLIENT_REGISTRATION);
	}

}
