package com.konfigyr.security;

import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.KeyGenerator;
import com.konfigyr.test.TestPrincipals;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class SecurityIntegrationTest extends AbstractControllerTest {

	@Test
	@DisplayName("should validate the OAuth Access token")
	void validate() {
		final String token = generateAccessToken(claims -> claims
				.issuer(wiremock.baseUrl())
				.subject(TestPrincipals.john().getName())
		);

		assertThatRequest(token)
				.hasStatusOk();
	}

	@Test
	@DisplayName("should fail to validate the OAuth Access token when signatures do not match")
	void invalidSignature() {
		final String token = generateAccessToken(KeyGenerator.getInstance().generate(), claims -> claims
				.issuer(wiremock.baseUrl())
				.subject(TestPrincipals.john().getName())
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to retrieve namespaces when subject is incorrect")
	void invalidSubject() {
		final String token = generateAccessToken(claims -> claims
				.issuer(wiremock.baseUrl())
				.subject("some subject")
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to validate the OAuth Access token when issuer is incorrect")
	void invalidIssuer() {
		final String token = generateAccessToken(claims -> claims
				.issuer("https://id.konfigyr.com")
				.subject(TestPrincipals.john().getName())
		);

		assertThatRequest(token)
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@DisplayName("should fail to validate the OAuth Access token when it's expired")
	void expiredAccessToken() {
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

	static String generateAccessToken(@NonNull Consumer<JWTClaimsSet.Builder> customizer) {
		return generateAccessToken(KeyGenerator.getInstance().get(), customizer);
	}

	static String generateAccessToken(@NonNull JWK key, @NonNull Consumer<JWTClaimsSet.Builder> customizer) {
		final var builder = new JWTClaimsSet.Builder();
		customizer.accept(builder);

		return KeyGenerator.getInstance().sign(key, builder.build()).serialize();
	}

}
