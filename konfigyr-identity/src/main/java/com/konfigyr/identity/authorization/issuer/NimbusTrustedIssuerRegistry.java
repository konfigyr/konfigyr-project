package com.konfigyr.identity.authorization.issuer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.konfigyr.entity.EntityId;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadataClaimNames;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Nimbus-backed {@link TrustedIssuerRegistry} that resolves and verifies JWT subject
 * tokens using a two-level caching strategy to minimize remote JWKS fetches while
 * keeping signing keys up to date.
 *
 * <h2>Caching layers</h2>
 * <p>
 * <b>Outer Caffeine cache</b>: one live {@link JWKSource} per
 * {@link TrustedIssuerRegistration#id()}, held for a configurable inactivity TTL
 * (default: 7 days). A cache miss triggers JWKS URI resolution and constructs a new
 * {@link JWKSource} via {@link JWKSourceBuilder}. When an entry is evicted,
 * {@link JWKSourceCloser} calls {@link java.io.Closeable#close()} on the source,
 * releasing any thread pools or connections held by the Nimbus chain.
 * <p>
 * <b>Nimbus-internal cache</b>: the {@link JWKSource} produced by
 * {@link JWKSourceBuilder} keeps the parsed JWK set in memory with a short TTL
 * (default: 5 minutes). On expiry, it triggers a background key refresh so that callers
 * are never blocked waiting for the network. Rate-limiting ensures at most one refresh
 * request per second to the remote JWKS endpoint; the built-in retry handles transient
 * network failures transparently.
 * <p>
 * <b>OIDC discovery</b>: for registrations without an explicit
 * {@link TrustedIssuerRegistration#jwksUri()}, a single HTTP call to the issuer's
 * {@code /.well-known/openid-configuration} resolves the JWKS URI. Because this happens
 * only on an outer-cache miss, the round trip is amortized over the full outer TTL;
 * at most once per issuer per eviction cycle.
 *
 * <h2>Request timeline example</h2>
 * <pre>
 * t=0s    get(namespace, issuerUri) called
 *           Caffeine MISS → discoverJwkSetUri, buildSource [OIDC discovery HTTP call
 *                           only if jwksUri is null], JWKSource stored in Caffeine
 *
 * t=0s    trustedIssuer.verify(token) called
 *           Nimbus MISS → fetches JWKS endpoint [HTTP], parses and caches JWK set
 *           Signature, iss, exp and aud claims validated → JWT returned
 *
 * t=30s   get(namespace, issuerUri) called
 *           Caffeine HIT → same JWKSource returned, no HTTP call
 *         trustedIssuer.verify(token) called
 *           Nimbus HIT → no HTTP call, claims validated → JWT returned
 *
 * t=5min  Nimbus internal cache expires
 *           Next verify() submits a background key refresh [HTTP]
 *           Caller is not blocked — stale keys serve requests until refresh completes
 *
 * t=7days Caffeine entry evicted (inactivity TTL exceeded)
 *           JWKSourceCloser.close() called → Nimbus session torn down
 *
 * t=7days get(namespace, issuerUri) called
 *           Caffeine MISS → new JWKSource built, JWKS re-fetched on next verify()
 * </pre>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
final class NimbusTrustedIssuerRegistry implements TrustedIssuerRegistry, MeterBinder {

	private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
			new ParameterizedTypeReference<>() { /* noop */ };

	private final TrustedIssuerRepository repository;
	private final RestOperations operations;
	private final Cache<String, JWKSource<SecurityContext>> cache;

	/**
	 * Creates a new registry with the given Caffeine configuration specification.
	 *
	 * @param repository    the repository used to look up trusted issuer registrations
	 * @param operations    the HTTP client used for OIDC discovery when no explicit JWKS URI is configured
	 * @param specification Caffeine cache specification controlling the size and expiry of the JWK source
	 *                      cache, e.g. {@code "maximumSize=1000,expireAfterAccess=7d"}
	 */
	NimbusTrustedIssuerRegistry(
			TrustedIssuerRepository repository,
			RestOperations operations,
			String specification
	) {
		this.repository = repository;
		this.operations = operations;
		this.cache = Caffeine.from(specification)
				.removalListener(new JWKSourceCloser())
				.recordStats()
				.build();
	}

	/**
	 * Looks up the {@link TrustedIssuerRegistration} for the given namespace and issuer URI,
	 * builds or retrieves a cached {@link JWKSource} for that registration, and returns a
	 * {@link TrustedIssuer} ready to verify subject tokens.
	 *
	 * @param namespace the namespace on whose behalf the lookup is performed
	 * @param issuerUri the OIDC issuer URI to resolve
	 * @return a {@link TrustedIssuer} backed by a live JWK source, never {@code null}
	 * @throws OAuth2AuthenticationException with {@code invalid_client} if no trusted issuer
	 *         is registered for the given namespace and issuer URI combination
	 */
	@Override
	public TrustedIssuer get(EntityId namespace, String issuerUri) {
		final TrustedIssuerRegistration issuer = repository.lookup(namespace, issuerUri);

		if (issuer == null) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_CLIENT,
					"Can't find any trusted issuer for this OAuth2 Client that matches this issuer URI: %s".formatted(issuerUri),
					ERROR_URI
			));
		}

		if (log.isDebugEnabled()) {
			log.debug("Resolving trusted issuer for: [issuer_id={}, issuer_url={}]", issuer.id(), issuer.issuerUri());
		}

		final JWKSource<SecurityContext> source = cache.get(issuer.id(), ignore -> buildSource(issuer));
		return new TrustedIssuer(issuer, buildDecoder(issuer, source));
	}

	/**
	 * Binds JWK source cache metrics to the given {@link MeterRegistry} under the name
	 * {@code identity.trusted-issuers}, exposing hit rate, load count, and eviction
	 * statistics.
	 *
	 * @param meterRegistry the registry to bind metrics to
	 */
	@Override
	public void bindTo(MeterRegistry meterRegistry) {
		CaffeineCacheMetrics.monitor(meterRegistry, cache, "identity.trusted-issuers");
	}

	private JWKSource<SecurityContext> buildSource(TrustedIssuerRegistration issuer) {
		final String keysetUri = discoverJwkSetUri(issuer);

		final URL url;

		try {
			url = URI.create(keysetUri).toURL();
		} catch (MalformedURLException ex) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_CLIENT,
					"Failed to parse JWKS URI of '%s' for Issuer: %s".formatted(keysetUri, issuer.issuerUri()),
					ERROR_URI
			), ex);
		}

		if (log.isDebugEnabled()) {
			log.debug("Creating new JWK Source for: [issuer={}, jwks_uri={}]", issuer.issuerUri(), keysetUri);
		}

		return JWKSourceBuilder.create(url)
				.cache(true)
				.rateLimited(true)
				.retrying(true)
				.build();
	}

	private String discoverJwkSetUri(TrustedIssuerRegistration issuer) {
		if (StringUtils.hasText(issuer.jwksUri())) {
			return issuer.jwksUri();
		}

		if (StringUtils.hasText(issuer.issuerUri())) {
			return discoverJwkSetUri(issuer.issuerUri());
		}

		throw new OAuth2AuthenticationException(new OAuth2Error(
				OAuth2ErrorCodes.INVALID_CLIENT,
				"No JWKS URI or issuer URI found",
				ERROR_URI
		));
	}

	private String discoverJwkSetUri(String issuerUri) {
		final UriComponents endpoint = generateOidcDiscoveryUri(issuerUri);
		final RequestEntity<Void> request = RequestEntity.get(endpoint.toUri())
				.build();

		if (log.isDebugEnabled()) {
			log.debug("Attempting to discover JWKS URI for: [issuer={}, discovery_uri={}]", issuerUri, endpoint);
		}

		final Map<String, Object> config;

		try {
			config = operations.exchange(request, MAP_TYPE).getBody();
		} catch (Exception ex) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_CLIENT,
					"Failed to fetch OIDC discovery document from: " + endpoint,
					ERROR_URI
			), ex);
		}

		if (CollectionUtils.isEmpty(config)) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_CLIENT,
					"No OIDC discovery document found at: " + endpoint,
					ERROR_URI
			));
		}

		if (log.isDebugEnabled()) {
			log.debug("Discovered OIDC discovery document for '{}' issuer: {}", issuerUri, config);
		}

		final Object value = config.get(OAuth2AuthorizationServerMetadataClaimNames.JWKS_URI);

		if (value instanceof String uri && StringUtils.hasText(uri)) {
			return uri;
		}

		throw new OAuth2AuthenticationException(new OAuth2Error(
				OAuth2ErrorCodes.INVALID_CLIENT,
				"No 'jwks_uri' claim found in OIDC discovery document at: " + endpoint,
				ERROR_URI
		));
	}

	private static JwtDecoder buildDecoder(TrustedIssuerRegistration issuer, JWKSource<SecurityContext> source) {
		final NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(source).build();

		final List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(JwtValidators.createDefaultWithIssuer(issuer.issuerUri()));

		if (!CollectionUtils.isEmpty(issuer.allowedAudiences())) {
			validators.add(new AnyAudienceValidator(issuer.allowedAudiences()));
		}

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
		return decoder;
	}

	/**
	 * Returns the underlying JWK source cache. Exposed for testing.
	 *
	 * @return the JWK source cache
	 */
	Cache<String, JWKSource<SecurityContext>> sourceCache() {
		return cache;
	}

	private static UriComponents generateOidcDiscoveryUri(String issuer) {
		final UriComponents uri = UriComponentsBuilder.fromUriString(issuer).build();

		return UriComponentsBuilder.newInstance().uriComponents(uri)
				.replacePath(uri.getPath())
				.pathSegment(".well-known", "openid-configuration")
				.build();
	}

	/**
	 * {@link OAuth2TokenValidator} that passes if the JWT {@code aud} claim contains
	 * at least one value from the configured set of allowed audiences.
	 */
	private static final class AnyAudienceValidator implements OAuth2TokenValidator<Jwt> {

		private final JwtClaimValidator<Collection<String>> validator;

		private AnyAudienceValidator(Collection<String> audiences) {
			Assert.notNull(audiences, "audiences cannot be null");
			this.validator = new JwtClaimValidator<>(JwtClaimNames.AUD,
					values -> audiences.stream().anyMatch(values::contains));
		}

		@Override
		public OAuth2TokenValidatorResult validate(Jwt token) {
			return this.validator.validate(token);
		}

	}

	/**
	 * Caffeine {@link RemovalListener} that calls {@link Closeable#close()} on evicted
	 * {@link JWKSource} instances, releasing any thread pools or connections held by
	 * the Nimbus key-set source chain.
	 */
	private static final class JWKSourceCloser implements RemovalListener<String, JWKSource<SecurityContext>> {

		@Override
		public void onRemoval(@Nullable String issuer, @Nullable JWKSource<SecurityContext> source, RemovalCause cause) {
			log.debug("Attempting to close the evicted JWK source for issuer: {}", issuer);

			if (source instanceof Closeable closeable) {
				try {
					closeable.close();
				} catch (IOException ex) {
					log.warn("Failed to close JWK source for issuer: {}", issuer, ex);
				}
			}
		}
	}

}
