package com.konfigyr.identity.authorization;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static com.konfigyr.identity.authorization.DefaultAuthorizationService.*;
import static org.assertj.core.api.Assertions.*;

@TestProfile
@Transactional
@SpringBootTest
@ExtendWith(PublishedEventsExtension.class)
@ImportTestcontainers(TestContainers.class)
class AuthorizationServiceTest {

	@Autowired
	AuthorizationService authorizationService;

	@Autowired
	RegisteredClientRepository registeredClientRepository;

	OAuth2Authorization.Builder builder;

	@BeforeEach
	void setup() {
		final var client = registeredClientRepository.findById("konfigyr");

		assertThat(client)
				.describedAs("The `konfigyr` OAuth client needs to be registered")
				.isNotNull();

		builder = OAuth2Authorization.withRegisteredClient(client)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizedScopes(Set.of("openid", "profile"))
				.principalName("john.doe@konfigyr.com");
	}

	@Test
	@DisplayName("should store authorization with OAuth authorization code token and state")
	void shouldStoreAuthorizationCode(AssertablePublishedEvents events) {
		OAuth2Authorization authorization = builder
				.attribute(OAuth2ParameterNames.STATE, "state")
				.attribute(OidcParameterNames.NONCE, "nonce")
				.token(authorizationCode("authorization-code-value"))
				.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		assertThat(authorizationService.findById(authorization.getId()))
				.isEqualTo(authorization)
				.isEqualTo(authorizationService.findByToken("authorization-code-value", null))
				.isEqualTo(authorizationService.findByToken("authorization-code-value", AUTHORIZATION_CODE_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("state", null))
				.isEqualTo(authorizationService.findByToken("state", AUTHORIZATION_STATE_TOKEN_TYPE));

		events.assertThat()
				.contains(AuthorizationEvent.Stored.class)
				.matching(AuthorizationEvent::id, authorization.getId());
	}

	@Test
	@DisplayName("should update existing authorization")
	void shouldUpdateExistingAuthorization(AssertablePublishedEvents events) {
		OAuth2Authorization initial = builder
				.attribute(OAuth2ParameterNames.STATE, "state")
				.attribute(OidcParameterNames.NONCE, "nonce")
				.token(authorizationCode("authorization-code-value"))
				.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(initial));

		assertThat(authorizationService.findById(initial.getId()))
				.isEqualTo(initial);

		final var token = idToken("updated-token");

		OAuth2Authorization updated = OAuth2Authorization.from(initial)
				.token(token, attrs -> attrs.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, token.getClaims()))
				.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(updated));

		assertThat(authorizationService.findById(initial.getId()))
				.isEqualTo(updated)
				.isNotEqualTo(initial);

		events.assertThat()
				.contains(AuthorizationEvent.Stored.class)
				.matching(AuthorizationEvent::id, initial.getId());
	}

	@Test
	@DisplayName("should store authorization with authorization code, access, openid and refresh token")
	void shouldStoreAccessToken(AssertablePublishedEvents events) {
		final var id = idToken("id-token");

		OAuth2Authorization authorization = builder
				.token(authorizationCode("authorization-code"))
				.token(accessToken("access-token", "openid"))
				.token(refreshToken("refresh-token"))
				.token(id, attrs -> attrs.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, id.getClaims()))
				.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		assertThat(authorizationService.findById(authorization.getId()))
				.isEqualTo(authorization)
				.isEqualTo(authorizationService.findByToken("authorization-code", AUTHORIZATION_CODE_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("access-token", OAuth2TokenType.ACCESS_TOKEN))
				.isEqualTo(authorizationService.findByToken("refresh-token", OAuth2TokenType.REFRESH_TOKEN))
				.isEqualTo(authorizationService.findByToken("id-token", OIDC_TOKEN_TYPE));

		// make sure that we do not retrieve by access token value...
		assertThat(authorizationService.findByToken("access-token", OAuth2TokenType.REFRESH_TOKEN))
				.isEqualTo(authorizationService.findByToken("access-token", OIDC_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("access-token", AUTHORIZATION_STATE_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("access-token", AUTHORIZATION_CODE_TOKEN_TYPE))
				.isNull();

		// make sure that we do not retrieve by refresh token value...
		assertThat(authorizationService.findByToken("refresh-token", OAuth2TokenType.ACCESS_TOKEN))
				.isEqualTo(authorizationService.findByToken("refresh-token", OIDC_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("refresh-token", AUTHORIZATION_STATE_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("refresh-token", AUTHORIZATION_CODE_TOKEN_TYPE))
				.isNull();

		// make sure that we do not retrieve by id token value...
		assertThat(authorizationService.findByToken("id-token", OAuth2TokenType.ACCESS_TOKEN))
				.isEqualTo(authorizationService.findByToken("id-token", OAuth2TokenType.REFRESH_TOKEN))
				.isEqualTo(authorizationService.findByToken("id-token", AUTHORIZATION_STATE_TOKEN_TYPE))
				.isEqualTo(authorizationService.findByToken("id-token", AUTHORIZATION_CODE_TOKEN_TYPE))
				.isNull();

		events.assertThat()
				.contains(AuthorizationEvent.Stored.class)
				.matching(AuthorizationEvent::id, authorization.getId());
	}

	@Test
	@DisplayName("should remove authorization with identifier")
	void shouldRemoveAuthorization(AssertablePublishedEvents events) {
		OAuth2Authorization authorization = builder
				.token(accessToken("access-token-to-be-removed"))
				.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		assertThat(authorizationService.findById(authorization.getId()))
				.isEqualTo(authorization);

		assertThatNoException().isThrownBy(() -> authorizationService.remove(authorization));

		assertThat(authorizationService.findById(authorization.getId()))
				.isNull();

		events.assertThat()
				.contains(AuthorizationEvent.Revoked.class)
				.matching(AuthorizationEvent::id, authorization.getId());
	}

	@Test
	@DisplayName("should cleanup expired authorizations")
	void shouldCleanupExpiredAuthorizations(AssertablePublishedEvents events) {
		// create expired token
		assertThatNoException().isThrownBy(() -> authorizationService.save(
				builder.id("expired-authorization")
						.token(authorizationCode("expired-code", Duration.ofHours(1)))
						.token(accessToken("expired-token", Duration.ofMinutes(30)))
						.token(refreshToken("expired-token", Duration.ofMinutes(15)))
						.build()
		));

		// create active token
		assertThatNoException().isThrownBy(() -> authorizationService.save(
				builder.id("active-authorization")
						.token(authorizationCode("expired-code", Duration.ofMinutes(15)))
						.token(accessToken("expired-token", Duration.ofMinutes(10)))
						.token(refreshToken("active-token"))
						.build()
		));

		assertThatNoException().isThrownBy(() -> ((DefaultAuthorizationService) authorizationService).cleanup());

		assertThat(authorizationService.findById("active-authorization"))
				.isNotNull();

		assertThat(authorizationService.findById("expired-authorization"))
				.isNull();

		events.assertThat()
				.contains(AuthorizationEvent.Revoked.class)
				.matching(AuthorizationEvent::id, "expired-authorization");
	}

	@Test
	@DisplayName("should not cleanup pending authorizations")
	void shouldNotCleanupPendingAuthorizations(AssertablePublishedEvents events) {
		final var authorization = builder.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		assertThatNoException().isThrownBy(() -> ((DefaultAuthorizationService) authorizationService).cleanup());

		assertThat(authorizationService.findById(authorization.getId()))
				.isNotNull();

		assertThat(events.eventOfTypeWasPublished(AuthorizationEvent.Revoked.class))
				.isFalse();
	}

	@Test
	@DisplayName("should not cleanup non-expired authorizations")
	void shouldNotCleanupActiveAuthorizations(AssertablePublishedEvents events) {
		final var authorization = builder
				.token(authorizationCode("active-code", Duration.ofMinutes(5)))
				.build();

		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		assertThatNoException().isThrownBy(() -> ((DefaultAuthorizationService) authorizationService).cleanup());

		assertThat(authorizationService.findById(authorization.getId()))
				.isNotNull();

		assertThat(events.eventOfTypeWasPublished(AuthorizationEvent.Revoked.class))
				.isFalse();
	}

	@Test
	@DisplayName("should remove authorizations when consent was revoked")
	void shouldRemoveAuthorizationWhenConsentIsRevoked(AssertablePublishedEvents events) {
		assertThatNoException().isThrownBy(() -> {
			// #1 - konfigyr -> john.doe@konfigyr.com
			authorizationService.save(builder.id("first").build());
			// #2 - konfigyr -> john.doe@konfigyr.com
			authorizationService.save(builder.id("second").build());
			// #3 - konfigyr -> jane.doe@konfigyr.com
			authorizationService.save(builder.id("third").principalName("jane.doe@konfigyr.com").build());
		});

		// should remove first and second...
		assertThatNoException().isThrownBy(() -> ((DefaultAuthorizationService) authorizationService).remove(
				"konfigyr", "john.doe@konfigyr.com"
		));

		assertThat(authorizationService.findById("first")).isNull();
		assertThat(authorizationService.findById("second")).isNull();
		assertThat(authorizationService.findById("third")).isNotNull();

		events.assertThat()
				.contains(AuthorizationEvent.Revoked.class)
				.matching(AuthorizationEvent::id, "first");

		events.assertThat()
				.contains(AuthorizationEvent.Revoked.class)
				.matching(AuthorizationEvent::id, "second");
	}

	@Test
	@DisplayName("should not remove authorizations when consent was revoked for different registered client")
	void shouldNotRemoveAuthorizationsForDifferentClient(AssertablePublishedEvents events) {
		final var authorization = builder.build();
		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		// should not remove authorization
		assertThatNoException().isThrownBy(() -> ((DefaultAuthorizationService) authorizationService).onConsentRevoked(
				new AuthorizationConsentEvent.Revoked(consentFor("other-client", "john.doe@konfigyr.com", "profile"))
		));

		assertThat(authorizationService.findById(authorization.getId()))
				.isNotNull();

		assertThat(events.eventOfTypeWasPublished(AuthorizationEvent.Revoked.class))
				.isFalse();
	}

	@Test
	@DisplayName("should not remove authorizations when consent was revoked for different principal")
	void shouldNotRemoveAuthorizationsForDifferentPrincipal(AssertablePublishedEvents events) {
		final var authorization = builder.build();
		assertThatNoException().isThrownBy(() -> authorizationService.save(authorization));

		// should not remove authorization
		assertThatNoException().isThrownBy(() -> ((DefaultAuthorizationService) authorizationService).onConsentRevoked(
				new AuthorizationConsentEvent.Revoked(consentFor("konfigyr", "jane.doe@konfigyr.com", "profile"))
		));

		assertThat(authorizationService.findById(authorization.getId()))
				.isNotNull();

		assertThat(events.eventOfTypeWasPublished(AuthorizationEvent.Revoked.class))
				.isFalse();
	}

	@Test
	@DisplayName("should store, lookup and remove authorization consent")
	void shouldManageConsent(AssertablePublishedEvents events) {
		OAuth2AuthorizationConsent consent = consentFor("konfigyr", "john.doe@konfigyr.com", "profile");

		assertThatNoException().isThrownBy(() -> authorizationService.save(consent));

		assertThat(authorizationService.findById(consent.getRegisteredClientId(), consent.getPrincipalName()))
				.isEqualTo(consent);

		assertThatNoException().isThrownBy(() -> authorizationService.save(OAuth2AuthorizationConsent.from(consent)
				.scope("email")
				.build()));

		assertThat(authorizationService.findById(consent.getRegisteredClientId(), consent.getPrincipalName()))
				.isNotEqualTo(consent)
				.returns(consent.getRegisteredClientId(), OAuth2AuthorizationConsent::getRegisteredClientId)
				.returns(consent.getPrincipalName(), OAuth2AuthorizationConsent::getPrincipalName)
				.returns(Set.of("profile", "email"), OAuth2AuthorizationConsent::getScopes);

		assertThatNoException().isThrownBy(() -> authorizationService.remove(consent));

		assertThat(authorizationService.findById(consent.getRegisteredClientId(), consent.getPrincipalName()))
				.isNull();

		events.assertThat()
				.contains(AuthorizationConsentEvent.Granted.class)
				.matching(event -> event.consent().getPrincipalName().equals(consent.getPrincipalName()))
				.matching(event -> event.consent().getRegisteredClientId().equals(consent.getRegisteredClientId()))
				.matching(event -> event.consent().getScopes().equals(Set.of("profile")));

		events.assertThat()
				.contains(AuthorizationConsentEvent.Granted.class)
				.matching(event -> event.consent().getPrincipalName().equals(consent.getPrincipalName()))
				.matching(event -> event.consent().getRegisteredClientId().equals(consent.getRegisteredClientId()))
				.matching(event -> event.consent().getScopes().equals(Set.of("profile", "email")));

		events.assertThat()
				.contains(AuthorizationConsentEvent.Revoked.class)
				.matching(AuthorizationConsentEvent::consent, consent);
	}

	@Test
	@DisplayName("should verify that authorization is not null when saving or removing")
	void shouldAssertAuthorization() {
		assertThatThrownBy(() -> authorizationService.save((OAuth2Authorization) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization cannot be null");

		assertThatThrownBy(() -> authorizationService.remove((OAuth2Authorization) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization cannot be null");
	}

	@Test
	@DisplayName("should verify that authorization consent is not null when saving or removing")
	void shouldAssertAuthorizationConsent() {
		assertThatThrownBy(() -> authorizationService.save((OAuth2AuthorizationConsent) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization consent cannot be null");

		assertThatThrownBy(() -> authorizationService.remove((OAuth2AuthorizationConsent) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization consent cannot be null");
	}

	@Test
	@DisplayName("should verify that identifier is not blank when retrieving authorization")
	void shouldAssertAuthorizationIdentifier() {
		assertThatThrownBy(() -> authorizationService.findById(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization identifier cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization identifier cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById(" "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization identifier cannot be empty");
	}

	@Test
	@DisplayName("should verify that token is not blank when retrieving authorization")
	void shouldAssertTokenValue() {
		assertThatThrownBy(() -> authorizationService.findByToken(null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization token value cannot be empty");

		assertThatThrownBy(() -> authorizationService.findByToken("", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization token value cannot be empty");

		assertThatThrownBy(() -> authorizationService.findByToken(" ", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth2 Authorization token value cannot be empty");
	}

	@Test
	@DisplayName("should verify that client identifier and principal name are not blank when retrieving consent")
	void shouldAssertAuthorizationConsentLookupArguments() {
		assertThatThrownBy(() -> authorizationService.findById(null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Registered client identifier cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById("", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Registered client identifier cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById(" ", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Registered client identifier cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById("konfigyr", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Principal name cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById("konfigyr", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Principal name cannot be empty");

		assertThatThrownBy(() -> authorizationService.findById("konfigyr", " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Principal name cannot be empty");
	}

	static OAuth2AuthorizationCode authorizationCode(String code) {
		return authorizationCode(code, Duration.ZERO);
	}

	static OAuth2AuthorizationCode authorizationCode(String code, Duration skew) {
		final var timestamp = Instant.now().minus(skew);
		return new OAuth2AuthorizationCode(code, timestamp, timestamp.plusSeconds(600));
	}

	static OAuth2AccessToken accessToken(String code, String... scopes) {
		return accessToken(code, Duration.ZERO, scopes);
	}

	static OAuth2AccessToken accessToken(String code, Duration skew, String... scopes) {
		final var timestamp = Instant.now().minus(skew);
		return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, code, timestamp,
				timestamp.plusSeconds(600), Set.of(scopes));
	}

	static OAuth2RefreshToken refreshToken(String code) {
		return refreshToken(code, Duration.ZERO);
	}

	static OAuth2RefreshToken refreshToken(String code, Duration skew) {
		final var timestamp = Instant.now().minus(skew);
		return new OAuth2RefreshToken(code, timestamp, timestamp.plusSeconds(600));
	}

	static OidcIdToken idToken(String code) {
		return idToken(code, Duration.ZERO);
	}

	static OidcIdToken idToken(String code, Duration skew) {
		final var timestamp = Instant.now().minus(skew);
		return new OidcIdToken(code, timestamp, timestamp.plusSeconds(600), Map.of(
				IdTokenClaimNames.SUB, "john.doe@konfigyr.com"
		));
	}

	static OAuth2AuthorizationConsent consentFor(String client, String principal, String... scopes) {
		final var builder = OAuth2AuthorizationConsent.withId(client, principal);
		Arrays.stream(scopes).forEach(builder::scope);
		return builder.build();
	}

}
