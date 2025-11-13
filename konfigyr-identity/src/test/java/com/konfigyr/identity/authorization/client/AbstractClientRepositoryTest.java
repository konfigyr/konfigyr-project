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

abstract class AbstractClientRepositoryTest {

	static Consumer<RegisteredClient> assertClientId(String id, Duration issuedBefore) {
		return assertClientId(id, id, issuedBefore);
	}

	static Consumer<RegisteredClient> assertClientId(String id, String clientId, Duration issuedBefore) {
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

	static Consumer<RegisteredClient> assertClientSecret(String secret, Duration expiry) {
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

	static Consumer<RegisteredClient> assertScopes(String... scopes) {
		return client -> assertThat(client.getScopes())
				.as("Registered client should have following scopes: %s", Arrays.toString(scopes))
				.containsExactlyInAnyOrder(scopes);
	}

	static Consumer<RegisteredClient> assertAuthorizationGrantTypes(AuthorizationGrantType... types) {
		return client -> assertThat(client.getAuthorizationGrantTypes())
				.containsExactlyInAnyOrder(types);
	}

	static Consumer<RegisteredClient> assertClientAuthenticationMethods() {
		return client -> assertThat(client.getClientAuthenticationMethods())
				.containsExactlyInAnyOrder(
						ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
						ClientAuthenticationMethod.CLIENT_SECRET_POST
				);
	}

	static Consumer<RegisteredClient> assertTokenSettings() {
		return client -> assertThat(client.getTokenSettings())
				.isNotNull()
				.returns(OAuth2TokenFormat.SELF_CONTAINED, TokenSettings::getAccessTokenFormat)
				.returns(Duration.ofMinutes(15), TokenSettings::getAccessTokenTimeToLive)
				.returns(Duration.ofDays(7), TokenSettings::getRefreshTokenTimeToLive)
				.returns(Duration.ofMinutes(5), TokenSettings::getDeviceCodeTimeToLive)
				.returns(SignatureAlgorithm.RS256, TokenSettings::getIdTokenSignatureAlgorithm)
				.returns(true, TokenSettings::isReuseRefreshTokens);
	}

	static Consumer<RegisteredClient> assertClientSettings() {
		return client -> assertThat(client.getClientSettings())
				.isNotNull()
				.returns(null, ClientSettings::getJwkSetUrl)
				.returns(null, ClientSettings::getX509CertificateSubjectDN)
				.returns(null, ClientSettings::getTokenEndpointAuthenticationSigningAlgorithm)
				.returns(true, ClientSettings::isRequireAuthorizationConsent)
				.returns(true, ClientSettings::isRequireProofKey);
	}

	static Consumer<RegisteredClient> assertRedirectUris(String... uris) {
		return client -> {
			assertThat(client.getRedirectUris())
					.as("Registered client should have following redirect URIs: %s", Arrays.toString(uris))
					.containsExactlyInAnyOrder(uris);
		};
	}

	static Consumer<RegisteredClient> assertLogoutRedirectUris(String... uris) {
		return client -> {
			assertThat(client.getPostLogoutRedirectUris())
					.as("Registered client should have following post-logout redirect URIs: %s", Arrays.toString(uris))
					.containsExactlyInAnyOrder(uris);
		};
	}

}
