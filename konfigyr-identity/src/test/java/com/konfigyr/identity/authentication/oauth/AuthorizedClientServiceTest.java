package com.konfigyr.identity.authentication.oauth;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.OAuthAccountIdentityUser;
import com.konfigyr.test.OAuth2AccessTokens;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import com.konfigyr.test.assertions.OAuth2AccessTokenAssert;
import com.konfigyr.test.assertions.OAuth2TokenAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestProfile
@SpringBootTest
@ImportTestcontainers(TestContainers.class)
class AuthorizedClientServiceTest {

	static final String CLIENT_REGISTRATION = "oauth-test";

	@Autowired
	ClientRegistrationRepository registrationRepository;

	@Autowired
	OAuth2AuthorizedClientService authorizedClientService;

	@Test
	@Transactional
	@DisplayName("should create, retrieve and remove OAuth2 authorized clients for accounts")
	void shouldManageAuthorizedClients() {
		final var accessToken = createAccessToken();
		final var refreshToken = createRefreshToken();
		final var account = AccountIdentities.john().build();
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
				.returns(account.getName(), OAuth2AuthorizedClient::getPrincipalName)
				.satisfies(it -> OAuth2AccessTokenAssert.assertThat(it.getAccessToken())
						.isNotNull()
						.isBearerToken()
						.hasValue(accessToken.getTokenValue())
						.hasScopes(accessToken.getScopes())
						.issuedAt(accessToken.getIssuedAt(), within(1, ChronoUnit.SECONDS))
						.expiresAt(accessToken.getExpiresAt(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> OAuth2TokenAssert.assertThat(it.getRefreshToken())
						.isNotNull()
						.hasValue(refreshToken.getTokenValue())
						.issuedAt(refreshToken.getIssuedAt(), within(1, ChronoUnit.SECONDS))
						.expiresAt(refreshToken.getExpiresAt(), within(1, ChronoUnit.SECONDS))
				);

		assertThatNoException()
				.isThrownBy(() -> authorizedClientService.removeAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNull();
	}

	@Test
	@Transactional
	@DisplayName("should create, retrieve and remove OAuth2 authorized clients without refresh token")
	void shouldManageAuthorizedClientsWithoutRefreshToken() {
		final var accessToken = createAccessToken("profile", "email");
		final var account = AccountIdentities.john().build();
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
				.returns(account.getName(), OAuth2AuthorizedClient::getPrincipalName)
				.returns(null, OAuth2AuthorizedClient::getRefreshToken)
				.satisfies(it -> OAuth2AccessTokenAssert.assertThat(it.getAccessToken())
						.isNotNull()
						.isBearerToken()
						.hasValue(accessToken.getTokenValue())
						.hasScopes(accessToken.getScopes())
						.issuedAt(accessToken.getIssuedAt(), within(1, ChronoUnit.SECONDS))
						.expiresAt(accessToken.getExpiresAt(), within(1, ChronoUnit.SECONDS))
				);

		assertThatNoException()
				.isThrownBy(() -> authorizedClientService.removeAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, authentication.getName()))
				.isNull();
	}

	@Test
	@Transactional
	@DisplayName("should update existing OAuth2 authorized client")
	void shouldUpdateExistingOAuth2AuthorizedClient() {
		final var account = AccountIdentities.john().build();
		final var initialAccessToken = createAccessToken();
		final var updatedAccessToken = createAccessToken("profile", "email");

		assertThatNoException().isThrownBy(() -> authorizedClientService.saveAuthorizedClient(new OAuth2AuthorizedClient(
				registrationRepository.findByRegistrationId(CLIENT_REGISTRATION),
				account.getName(),
				initialAccessToken
		), authentication(account)));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, account.getName()))
				.isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(OAuth2AuthorizedClient.class))
				.satisfies(it -> assertThat(it.getRefreshToken())
						.isNull()
				)
				.satisfies(it -> OAuth2AccessTokenAssert.assertThat(it.getAccessToken())
						.isNotNull()
						.hasValue(initialAccessToken.getTokenValue())
						.hasScopes(initialAccessToken.getScopes())
						.issuedAt(initialAccessToken.getIssuedAt(), within(100, ChronoUnit.MILLIS))
						.expiresAt(initialAccessToken.getExpiresAt(), within(100, ChronoUnit.MILLIS))
				);

		assertThatNoException().isThrownBy(() -> authorizedClientService.saveAuthorizedClient(new OAuth2AuthorizedClient(
				registrationRepository.findByRegistrationId(CLIENT_REGISTRATION),
				account.getName(),
				updatedAccessToken,
				createRefreshToken()
		), authentication(account)));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, account.getName()))
				.isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(OAuth2AuthorizedClient.class))
				.satisfies(it -> assertThat(it.getRefreshToken())
						.isNotNull()
				)
				.satisfies(it -> OAuth2AccessTokenAssert.assertThat(it.getAccessToken())
						.isNotNull()
						.hasValue(updatedAccessToken.getTokenValue())
						.hasScopes(updatedAccessToken.getScopes())
						.issuedAt(updatedAccessToken.getIssuedAt(), within(100, ChronoUnit.MILLIS))
						.expiresAt(updatedAccessToken.getExpiresAt(), within(100, ChronoUnit.MILLIS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should load OAuth2 authorized client that matches both client registration and principal name")
	void shouldLoadMatchingAuthorizedClient() {
		final var account = AccountIdentities.john().build();

		assertThatNoException().isThrownBy(() -> authorizedClientService.saveAuthorizedClient(new OAuth2AuthorizedClient(
				registrationRepository.findByRegistrationId(CLIENT_REGISTRATION),
				account.getName(),
				createAccessToken("profile")
		), authentication(account)));

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, account.getName()))
				.isNotNull();

		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, EntityId.from(3746).serialize()))
				.isNull();
	}

	@Test
	@DisplayName("should return null OAuth2 authorized client when principal name is invalid")
	void shouldFailToLoadAuthorizedClientForInvalidPrincipalName() {
		assertThatObject(authorizedClientService.loadAuthorizedClient(CLIENT_REGISTRATION, "principal"))
				.isNull();
	}

	@Test
	@DisplayName("should fail to load OAuth2 authorized clients when client registration is unknown")
	void shouldFailToLoadAuthorizedClientForUnknownRegistration() {
		final var account = AccountIdentities.jane().build();

		assertThatThrownBy(() -> authorizedClientService.loadAuthorizedClient("test-client", account.getName()))
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

	static OAuth2AccessToken createAccessToken(String... scopes) {
		return OAuth2AccessTokens.createAccessToken("access-token", scopes);
	}

	static OAuth2RefreshToken createRefreshToken() {
		return OAuth2AccessTokens.createRefreshToken("refresh-token");
	}

	static @NonNull Authentication authentication(@NonNull AccountIdentity identity) {
		final var user = new DefaultOAuth2User(identity.getAuthorities(), Map.of(
				StandardClaimNames.SUB, identity.getEmail(),
				StandardClaimNames.EMAIL, identity.getEmail(),
				StandardClaimNames.NAME, identity.getDisplayName()
		), StandardClaimNames.EMAIL);

		return new OAuth2AuthenticationToken(new OAuthAccountIdentityUser(identity, user),
				identity.getAuthorities(), CLIENT_REGISTRATION);
	}

}
