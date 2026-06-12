package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring configuration that registers the trusted issuer infrastructure beans. All
 * implementation classes in this package are package-private; this configuration is
 * the single place that creates and wires them.
 * <p>
 * The two beans registered here are:
 * <ul>
 *   <li>{@link TrustedIssuerRepository} — a {@link CompositeTrustedIssuerRepository}
 *       pre-seeded with the {@link WellKnownTrustedIssuers} static registry. Additional
 *       {@link TrustedIssuerRepository} implementations registered as beans elsewhere in
 *       the application context are composed into the same composite automatically.</li>
 *   <li>{@link TrustedIssuerRegistry} — a {@link NimbusTrustedIssuerRegistry} backed by a
 *       Caffeine cache whose size and TTL are controlled via
 *       {@code konfigyr.identity.authorization.trusted-issuers.cache.spec}.</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
class TrustedIssuerConfiguration {

	private static final String NAMESPACE_TRUSTED_ISSUER_REPOSITORY = "namespaceTrustedIssuerRepository";

	@Bean(name = NAMESPACE_TRUSTED_ISSUER_REPOSITORY, defaultCandidate = false)
	NamespaceTrustedIssuerRepository namespaceTrustedIssuerRepository(DSLContext context, JsonMapper jsonMapper) {
		return new NamespaceTrustedIssuerRepository(context, jsonMapper);
	}

	@Bean
	TrustedIssuerRepository trustedIssuerRepository(
			@Qualifier(NAMESPACE_TRUSTED_ISSUER_REPOSITORY) TrustedIssuerRepository namespaceRepository
	) {
		return new CompositeTrustedIssuerRepository(WellKnownTrustedIssuers.getInstance(), namespaceRepository);
	}

	@Bean
	NimbusTrustedIssuerRegistry trustedIssuerRegistry(
			TrustedIssuerRepository trustedIssuerRepository,
			RestTemplateBuilder restTemplateBuilder,
			AuthorizationProperties properties
	) {
		final String spec = properties.getTrustedIssuers().getCache().getSpec();
		Assert.hasText(spec, "Trusted issuer cache specification must not be blank");
		return new NimbusTrustedIssuerRegistry(trustedIssuerRepository, restTemplateBuilder.build(), spec);
	}

}
