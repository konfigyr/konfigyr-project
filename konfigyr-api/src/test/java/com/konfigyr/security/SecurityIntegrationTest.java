package com.konfigyr.security;

import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class SecurityIntegrationTest extends AbstractControllerTest {

	static JWK key;

	@BeforeAll
	static void generate() throws JOSEException {
		key = generateSigningKey();
	}

	@BeforeEach
	void setup() {
		stubFor(
				get(urlPathEqualTo("/oauth/jwks"))
						.willReturn(jsonResponse(
								new JWKSet(key).toString(true), 200
						))
		);
	}

	@Test
	@DisplayName("should validate the OAuth Access token")
	void validate() throws Exception {
		final String token = generateAccessToken(claims -> claims
				.issuer(wiremock.baseUrl())
				.subject(TestPrincipals.john().getName())
		);

		assertThatRequest(token)
				.hasStatusOk();
	}

	@Test
	@DisplayName("should fail to validate the OAuth Access token when signatures do not match")
	void invalidSignature() throws Exception {
		final String token = generateAccessToken(generateSigningKey(), claims -> claims
				.issuer(wiremock.baseUrl())
				.subject(TestPrincipals.john().getName())
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to retrieve namespaces when subject is incorrect")
	void invalidSubject() throws Exception {
		final String token = generateAccessToken(claims -> claims
				.issuer(wiremock.baseUrl())
				.subject("some subject")
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to validate the OAuth Access token when issuer is incorrect")
	void invalidIssuer() throws Exception {
		final String token = generateAccessToken(claims -> claims
				.issuer("https://id.konfigyr.com")
				.subject(TestPrincipals.john().getName())
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to validate the OAuth Access token when it's expired")
	void expiredAccessToken() throws Exception {
		final var timestamp = Instant.now();

		final String token = generateAccessToken(claims -> claims
				.issuer(wiremock.baseUrl())
				.subject(TestPrincipals.john().getName())
				.expirationTime(Date.from(timestamp.minusSeconds(600)))
				.notBeforeTime(Date.from(timestamp.minusSeconds(1200)))
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	static MvcTestResultAssert assertThatRequest(String token) {
		return mvc.get().uri("/namespaces")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.exchange()
				.assertThat()
				.apply(log());
	}

	static String generateAccessToken(@NonNull Consumer<JWTClaimsSet.Builder> customizer) throws JOSEException {
		return generateAccessToken(key, customizer);
	}

	static String generateAccessToken(@NonNull JWK key, @NonNull Consumer<JWTClaimsSet.Builder> customizer) throws JOSEException {
		final var factory = new DefaultJWSSignerFactory();
		final var signer = factory.createJWSSigner(key, JWSAlgorithm.RS256);
		final var builder = new JWTClaimsSet.Builder();

		customizer.accept(builder);

		final var token = new JWSObject(
				new JWSHeader(JWSAlgorithm.RS256),
				builder.build().toPayload()
		);

		token.sign(signer);

		return token.serialize();
	}

	static JWK generateSigningKey() throws JOSEException {
		return new RSAKeyGenerator(2048)
				.algorithm(JWSAlgorithm.RS256)
				.keyUse(KeyUse.SIGNATURE)
				.generate();
	}

}
