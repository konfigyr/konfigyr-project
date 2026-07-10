package com.konfigyr.identity.authentication;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.konfigyr.identity.TestClients;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class OidcTokenDecoderFactoryTest {

	static final String REGISTRATION_ID = "test-issuer";
	static final String CLIENT_ID = "konfigyr";
	static final String SUBJECT = "konfigyr-user";

	@RegisterExtension
	static WireMockExtension wiremock = WireMockExtension.newInstance()
			.options(WireMockConfiguration.wireMockConfig().dynamicPort())
			.configureStaticDsl(true)
			.build();

	RSAKey rsaKey;
	ECKey ecKey;

	@BeforeEach
	void setup() throws JOSEException {
		rsaKey = new RSAKeyGenerator(2048)
				.keyID(UUID.randomUUID().toString())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.RS256)
				.generate();

		ecKey = new ECKeyGenerator(Curve.P_256)
				.keyID(UUID.randomUUID().toString())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.ES256)
				.generate();

		wiremock.stubFor(get(urlPathEqualTo("/jwks.json"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBody(new JWKSet(List.of(rsaKey.toPublicJWK(), ecKey.toPublicJWK())).toString(true))
				));
	}

	@Test
	@DisplayName("should verify a token signed with RS256")
	void verifiesRs256SignedToken() {
		final var decoder = factory(Duration.ofSeconds(60)).createDecoder(clientRegistration());

		final var jwt = decoder.decode(sign(rsaKey, JWSAlgorithm.RS256, Instant.now().plusSeconds(300)));

		assertThat(jwt.getClaimAsString(JwtClaimNames.SUB)).isEqualTo(SUBJECT);
	}

	@Test
	@DisplayName("should verify a token signed with ES256 instead of assuming a hardcoded RS256 algorithm")
	void verifiesEs256SignedToken() {
		final var decoder = factory(Duration.ofSeconds(60)).createDecoder(clientRegistration());

		final var jwt = decoder.decode(sign(ecKey, JWSAlgorithm.ES256, Instant.now().plusSeconds(300)));

		assertThat(jwt.getClaimAsString(JwtClaimNames.SUB)).isEqualTo(SUBJECT);
	}

	@Test
	@DisplayName("should reject a token signed with an algorithm that is not present in the JWK Set")
	void rejectsTokenSignedWithUndiscoveredAlgorithm() throws JOSEException {
		final var otherKey = new RSAKeyGenerator(2048)
				.keyID(UUID.randomUUID().toString())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.RS256)
				.generate();

		final var decoder = factory(Duration.ofSeconds(60)).createDecoder(clientRegistration());

		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> decoder.decode(sign(otherKey, JWSAlgorithm.RS256, Instant.now().plusSeconds(300))));
	}

	@Test
	@DisplayName("should delegate to the default decoder factory when no JWKS URI is configured")
	void fallsBackWhenJwkSetUriIsMissing() {
		final var registration = registration().jwkSetUri(null).build();

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> factory(Duration.ofSeconds(60)).createDecoder(registration))
				.satisfies(ex -> assertThat(ex.getError().getErrorCode()).isEqualTo("missing_signature_verifier"));
	}

	@Test
	@DisplayName("should reject an expired token once outside the configured clock skew")
	void rejectsExpiredTokenOutsideClockSkew() {
		final var decoder = factory(Duration.ofSeconds(5)).createDecoder(clientRegistration());

		final var token = sign(rsaKey, JWSAlgorithm.RS256, Instant.now().minusSeconds(30));

		assertThatExceptionOfType(JwtException.class).isThrownBy(() -> decoder.decode(token));
	}

	@Test
	@DisplayName("should accept a token that is expired within the configured clock skew")
	void acceptsExpiredTokenWithinClockSkew() {
		final var decoder = factory(Duration.ofSeconds(60)).createDecoder(clientRegistration());

		final var token = sign(rsaKey, JWSAlgorithm.RS256, Instant.now().minusSeconds(30));

		assertThatNoException().isThrownBy(() -> decoder.decode(token));
	}

	private OidcTokenDecoderFactory factory(Duration clockSkew) {
		return new OidcTokenDecoderFactory(new RestTemplate(), clockSkew);
	}

	private ClientRegistration clientRegistration() {
		return registration().build();
	}

	private ClientRegistration.Builder registration() {
		return TestClients.clientRegistration(REGISTRATION_ID)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(wiremock.baseUrl() + "/login/oauth2/code/" + REGISTRATION_ID)
				.authorizationUri(wiremock.baseUrl() + "/authorize")
				.issuerUri(wiremock.baseUrl())
				.jwkSetUri(wiremock.baseUrl() + "/jwks.json");
	}

	private String sign(JWK key, JWSAlgorithm algorithm, Instant expiresAt) {
		final var header = new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build();
		final var claims = new JWTClaimsSet.Builder()
				.issuer(wiremock.baseUrl())
				.subject(SUBJECT)
				.audience(CLIENT_ID)
				.issueTime(Date.from(expiresAt.minusSeconds(300)))
				.expirationTime(Date.from(expiresAt))
				.build();

		final var jwt = new SignedJWT(header, claims);

		try {
			jwt.sign(key instanceof RSAKey rsa ? new RSASSASigner(rsa) : new ECDSASigner((ECKey) key));
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to sign JWT", e);
		}

		return jwt.serialize();
	}

}
