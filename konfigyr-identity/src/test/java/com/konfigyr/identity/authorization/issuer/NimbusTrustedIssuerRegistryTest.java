package com.konfigyr.identity.authorization.issuer;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.WorkloadIdentityStubs;
import com.nimbusds.jwt.JWTClaimsSet;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.client.RestTemplate;

import java.io.Closeable;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class NimbusTrustedIssuerRegistryTest {

	@RegisterExtension
	static WireMockExtension wiremock = WireMockExtension.newInstance()
			.options(WireMockConfiguration.wireMockConfig().dynamicPort())
			.configureStaticDsl(true)
			.build();

	static final EntityId NAMESPACE = EntityId.from(1L);

	@Mock
	TrustedIssuerRepository repository;

	WorkloadIdentityStubs stubs;
	NimbusTrustedIssuerRegistry registry;

	@BeforeEach
	void setup() {
		stubs = new WorkloadIdentityStubs(wiremock.baseUrl(), "test-oidc-issuer");
		wiremock.addStubMapping(stubs.createMetadataStub());
		wiremock.addStubMapping(stubs.createKeysetStub());

		registry = new NimbusTrustedIssuerRegistry(repository, new RestTemplate(), "expireAfterWrite=1h");
	}

	@Test
	@DisplayName("should return TrustedIssuer containing the resolved registration")
	void returnsIssuerWithMatchingRegistration() {
		final var registration = registration(stubs.keysetUri());
		doReturn(registration).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		final var issuer = registry.get(NAMESPACE, stubs.issuerUri());

		assertThat(issuer)
				.isNotNull()
				.returns(registration, TrustedIssuer::registration);
	}

	@Test
	@DisplayName("should verify a valid JWT when an explicit JWKS URI is configured")
	void verifiesValidTokenWithExplicitJwksUri() {
		doReturn(registration(stubs.keysetUri())).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		final var claims = registry.get(NAMESPACE, stubs.issuerUri())
				.verify(signToken("repo:acme/api:ref:refs/heads/main", Instant.now().plusSeconds(300)));

		assertThat(claims.getSubject()).isEqualTo("repo:acme/api:ref:refs/heads/main");
	}

	@Test
	@DisplayName("should resolve the JWKS URI via OIDC discovery when not explicitly configured")
	void resolvesJwksUriViaOidcDiscovery() {
		doReturn(registration(null)).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		final var claims = registry.get(NAMESPACE, stubs.issuerUri())
				.verify(signToken("repo:acme/api:ref:refs/heads/main", Instant.now().plusSeconds(300)));

		assertThat(claims.getSubject()).isEqualTo("repo:acme/api:ref:refs/heads/main");
	}

	@Test
	@DisplayName("should reuse the cached JWK source across multiple calls for the same issuer")
	void cachesJwkSourceAcrossCalls() {
		doReturn(registration(stubs.keysetUri())).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatNoException().isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri()));
		assertThatNoException().isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri()));

		assertThat(registry.sourceCache().stats())
				.returns(1L, CacheStats::loadCount)
				.returns(1L, CacheStats::hitCount);
	}

	@Test
	@DisplayName("should throw invalid_client when no trusted issuer is registered for the namespace and issuer URI")
	void throwsWhenIssuerNotFound() {
		doReturn(null).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri()))
				.extracting(OAuth2AuthenticationException::getError, InstanceOfAssertFactories.type(OAuth2Error.class))
				.returns(OAuth2ErrorCodes.INVALID_CLIENT, OAuth2Error::getErrorCode)
				.returns(
						"Can't find any trusted issuer for this OAuth2 Client that matches this issuer URI: %s".formatted(stubs.issuerUri()),
						OAuth2Error::getDescription
				);
	}

	@Test
	@DisplayName("should throw invalid_client when the OIDC discovery endpoint is unavailable")
	void throwsWhenDiscoveryEndpointUnavailable() {
		wiremock.stubFor(get(urlPathEqualTo(URI.create(stubs.metadataUri()).getPath()))
				.willReturn(serverError()));

		doReturn(registration(null)).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri()))
				.extracting(OAuth2AuthenticationException::getError, InstanceOfAssertFactories.type(OAuth2Error.class))
				.returns(OAuth2ErrorCodes.INVALID_CLIENT, OAuth2Error::getErrorCode);
	}

	@Test
	@DisplayName("should throw when the JWKS endpoint returns an error response during token verification")
	void throwsWhenJwksEndpointFails() {
		wiremock.stubFor(get(urlPathEqualTo(URI.create(stubs.keysetUri()).getPath()))
				.willReturn(serverError()));

		doReturn(registration(stubs.keysetUri())).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri())
						.verify(signToken("repo:acme/api:ref:refs/heads/main", Instant.now().plusSeconds(300))));
	}

	@Test
	@DisplayName("should reject a JWT whose audience does not match any configured allowed audience")
	void rejectsTokenWithMismatchedAudience() {
		doReturn(registration(stubs.keysetUri(), "konfigyr-api")).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri())
						.verify(signToken("repo:acme/api:ref:refs/heads/main", Instant.now().plusSeconds(300), "wrong-audience")));
	}

	@Test
	@DisplayName("should accept a JWT whose audience matches at least one of the configured allowed audiences")
	void acceptsTokenWithMatchingAudience() {
		doReturn(registration(stubs.keysetUri(), "konfigyr-api", "konfigyr-frontend"))
				.when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatNoException()
				.isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri())
						.verify(signToken("repo:acme/api:ref:refs/heads/main", Instant.now().plusSeconds(300), "konfigyr-api")));
	}

	@Test
	@DisplayName("should reject a JWT whose issuer claim does not match the registered issuer URI")
	void rejectsTokenWithWrongIssuer() {
		doReturn(registration(stubs.keysetUri())).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		final var tokenFromOtherIssuer = new WorkloadIdentityStubs(wiremock.baseUrl(), "other-issuer")
				.issue(new JWTClaimsSet.Builder()
						.subject("repo:acme/api:ref:refs/heads/main")
						.expirationTime(Date.from(Instant.now().plusSeconds(300))));

		assertThatExceptionOfType(JwtException.class)
				.isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri()).verify(tokenFromOtherIssuer));
	}

	@Test
	@DisplayName("should close the evicted JWK source and rebuild it on the next request")
	void closesJwkSourceOnEvictionAndRebuildsOnNextRequest() {
		final var registration = registration(stubs.keysetUri());
		doReturn(registration).when(repository).lookup(NAMESPACE, stubs.issuerUri());

		assertThatNoException().isThrownBy(() -> registry.get(NAMESPACE, stubs.issuerUri()));

		final var source = registry.sourceCache().getIfPresent(registration.id());
		assertThat(source).isNotNull().isInstanceOf(Closeable.class);

		// invalidate() schedules the removal; cleanUp() flushes it and fires JWKSourceCloser
		registry.sourceCache().invalidate(registration.id());
		registry.sourceCache().cleanUp();

		assertThat(registry.sourceCache().getIfPresent(registration.id())).isNull();

		registry.get(NAMESPACE, stubs.issuerUri());
		assertThat(registry.sourceCache().stats())
				.returns(2L, CacheStats::loadCount);
	}

	@Test
	@DisplayName("should bind JWK source cache metrics to the provided meter registry")
	void bindsCacheMetrics() {
		final var meterRegistry = new SimpleMeterRegistry();

		registry.bindTo(meterRegistry);

		assertThat(meterRegistry.find("cache.gets").meters()).isNotEmpty();
	}

	private TrustedIssuerRegistration registration(String jwksUri, String... audiences) {
		return TrustedIssuerRegistration.withId("test-issuer")
				.issuerUri(stubs.issuerUri())
				.name("Test Issuer")
				.jwksUri(jwksUri)
				.allowedAudiences(audiences)
				.build();
	}

	private String signToken(String subject, Instant expiry, String... audiences) {
		final var claims = new JWTClaimsSet.Builder()
				.subject(subject)
				.expirationTime(Date.from(expiry));

		if (audiences.length > 0) {
			claims.audience(List.of(audiences));
		}

		return stubs.issue(claims);
	}

}
