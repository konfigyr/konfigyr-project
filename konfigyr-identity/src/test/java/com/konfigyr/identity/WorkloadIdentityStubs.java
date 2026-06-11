package com.konfigyr.identity;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadataClaimNames;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

/**
 * Class that provides Wiremock stubs for the Workload Identity that is used to test
 * the Workload OAuth2 client type integration.
 */
final public class WorkloadIdentityStubs {

	private static final String METADATA_PATH = ".well-known/openid-configuration";
	private static final String KEYSET_PATH = "jwks.json";

	/**
	 * The Workload Identity issuer URI identifier.
	 */
	private final UriComponents issuer;

	/**
	 * The RSA key used to sign the Workload Identity JWT.
	 */
	private final RSAKey key;

	public WorkloadIdentityStubs(String hostname, String id) {
		this.issuer = UriComponentsBuilder.fromUriString(hostname).path(id).build();

		try {
			this.key = new RSAKeyGenerator(2048)
					.keyID(UUID.randomUUID().toString())
					.keyUse(KeyUse.SIGNATURE)
					.algorithm(JWSAlgorithm.RS256)
					.generate();
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to generate RSA key for mocking workload identity", e);
		}
	}

	public String issuerUri() {
		return issuer.toUriString();
	}

	public String metadataUri() {
		return createPath(METADATA_PATH).toUriString();
	}

	public String keysetUri() {
		return createPath(KEYSET_PATH).toUriString();
	}

	public StubMapping createMetadataStub() {
		final var json = JsonMapper.shared().writeValueAsString(
				Map.of(
						OAuth2AuthorizationServerMetadataClaimNames.ISSUER, issuerUri(),
						OAuth2AuthorizationServerMetadataClaimNames.JWKS_URI, keysetUri()
				)
		);

		return createStubMappingFor(METADATA_PATH, json);
	}

	public StubMapping createKeysetStub() {
		final var json = new JWKSet(key.toPublicJWK()).toString(true);
		return createStubMappingFor(KEYSET_PATH, json);
	}

	public String issue(JWTClaimsSet.Builder claims) {
		final var jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
				claims.issuer(issuerUri()).build()
		);

		try {
			jwt.sign(new RSASSASigner(key));
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to sign JWT", e);
		}

		return jwt.serialize();
	}

	private UriComponentsBuilder createPath(String... segments) {
		return UriComponentsBuilder.newInstance()
				.uriComponents(issuer)
				.pathSegment(segments);
	}

	private StubMapping createStubMappingFor(String path, String response) {
		final var url = UriComponentsBuilder.fromPath("/")
				.path(issuer.getPath())
				.pathSegment(path)
				.toUriString();

		return WireMock.get(WireMock.urlPathEqualTo(url))
				.willReturn(WireMock.aResponse().withStatus(200).withBody(response))
				.build();
	}

}
