package com.konfigyr.identity.authorization.client;

import com.konfigyr.security.PasswordEncoders;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

interface ClientRepositoryTestSupport {

	default Consumer<RegisteredClient> assertClientId(String id, String clientId, Duration issuedBefore) {
		return client -> {
			assertThat(client.getId())
					.as("Registered client registration identifier should be: %s", id)
					.isEqualTo(id);

			assertThat(client.getClientId())
					.as("Registered client_id should be: %s", clientId)
					.isEqualTo(clientId);

			if (issuedBefore != null) {
				final var issued = Instant.now().minus(issuedBefore);

				assertThat(client.getClientIdIssuedAt())
						.as("Registered client_id should be issued at: %s", issued)
						.isCloseTo(issued, within(1, ChronoUnit.HOURS));
			} else {
				assertThat(client.getClientIdIssuedAt())
						.isNull();
			}
		};
	}

	default Consumer<RegisteredClient> assertNoClientSecret() {
		return client -> {
			assertThat(client.getClientSecret())
					.as("Registered client_secret should not be set")
					.isNull();

			assertThat(client.getClientSecretExpiresAt())
					.as("Registered client_secret expiry date should not be set")
					.isNull();
		};
	}

	default Consumer<RegisteredClient> assertClientSecret(String secret, Duration expiry) {
		return client -> {
			assertThat(client.getClientSecret())
					.as("Registered client_secret should match: %s", secret)
					.matches(it -> PasswordEncoders.get().matches(secret, it));

			if (expiry != null) {
				final var expires = Instant.now().plus(expiry);

				assertThat(client.getClientSecretExpiresAt())
						.as("Registered client_secret should expire at: %s", expires)
						.isCloseTo(expires, within(1, ChronoUnit.HOURS));
			} else {
				assertThat(client.getClientSecretExpiresAt())
						.isNull();
			}
		};
	}

	default Consumer<RegisteredClient> assertScopes(String... scopes) {
		return client -> assertThat(client.getScopes())
				.as("Registered client should have following scopes: %s", Arrays.toString(scopes))
				.containsExactlyInAnyOrder(scopes);
	}

	default Consumer<RegisteredClient> assertAuthorizationGrantTypes(AuthorizationGrantType... types) {
		return client -> assertThat(client.getAuthorizationGrantTypes())
				.containsExactlyInAnyOrder(types);
	}

	default Consumer<RegisteredClient> assertClientAuthenticationMethods() {
		return assertClientAuthenticationMethods(
				ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
				ClientAuthenticationMethod.CLIENT_SECRET_POST
		);
	}

	default Consumer<RegisteredClient> assertClientAuthenticationMethods(ClientAuthenticationMethod... methods) {
		return client -> assertThat(client.getClientAuthenticationMethods())
				.containsExactlyInAnyOrder(methods);
	}

	default Consumer<RegisteredClient> assertTokenSettings() {
		return assertTokenSettings(true);
	}

	default Consumer<RegisteredClient> assertTokenSettings(boolean reuseRefreshTokens) {
		return client -> assertThat(client.getTokenSettings())
				.isNotNull()
				.returns(OAuth2TokenFormat.SELF_CONTAINED, TokenSettings::getAccessTokenFormat)
				.returns(Duration.ofMinutes(15), TokenSettings::getAccessTokenTimeToLive)
				.returns(Duration.ofDays(7), TokenSettings::getRefreshTokenTimeToLive)
				.returns(Duration.ofMinutes(5), TokenSettings::getDeviceCodeTimeToLive)
				.returns(SignatureAlgorithm.PS256, TokenSettings::getIdTokenSignatureAlgorithm)
				.returns(reuseRefreshTokens, TokenSettings::isReuseRefreshTokens);
	}

	default Consumer<RegisteredClient> assertClientSettings(boolean requireAuthorizationConsent) {
		return client -> assertThat(client.getClientSettings())
				.isNotNull()
				.returns(null, ClientSettings::getJwkSetUrl)
				.returns(null, ClientSettings::getX509CertificateSubjectDN)
				.returns(null, ClientSettings::getTokenEndpointAuthenticationSigningAlgorithm)
				.returns(requireAuthorizationConsent, ClientSettings::isRequireAuthorizationConsent)
				.returns(true, ClientSettings::isRequireProofKey);
	}

	default Consumer<RegisteredClient> assertRedirectUris(String... uris) {
		return client -> assertThat(client.getRedirectUris())
				.as("Registered client should have following redirect URIs: %s", Arrays.toString(uris))
				.containsExactlyInAnyOrder(uris);
	}

	default Consumer<RegisteredClient> assertLogoutRedirectUris(String... uris) {
		return client -> assertThat(client.getPostLogoutRedirectUris())
				.as("Registered client should have following post-logout redirect URIs: %s", Arrays.toString(uris))
				.containsExactlyInAnyOrder(uris);
	}

}
