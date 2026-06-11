package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity tests for {@link WellKnownTrustedIssuers}: verify that every expected
 * entry is present with the correct metadata and that entries not in the list
 * return {@code null}. These tests are intentionally light, the class is a
 * static registry, not logic under test.
 */
class WellKnownTrustedIssuersTest {

	static final EntityId NAMESPACE = EntityId.from(1L);

	final TrustedIssuerRepository repository = WellKnownTrustedIssuers.getInstance();

	@Test
	@DisplayName("getInstance() always returns the same singleton")
	void singletonContract() {
		assertThat(WellKnownTrustedIssuers.getInstance())
				.isSameAs(WellKnownTrustedIssuers.getInstance());
	}

	@Test
	@DisplayName("GitHub Actions issuer is registered")
	void githubActionsIssuerIsPresent() {
		final var issuer = repository.lookup(NAMESPACE, "https://token.actions.githubusercontent.com");

		assertThat(issuer)
				.isNotNull()
				.returns("https://token.actions.githubusercontent.com", TrustedIssuerRegistration::issuerUri)
				.returns("GitHub Actions", TrustedIssuerRegistration::name)
				.returns(null, TrustedIssuerRegistration::jwksUri)
				.satisfies(it -> assertThat(it.allowedAudiences()).isEmpty());
	}

	@Test
	@DisplayName("GitLab issuer is registered")
	void gitlabIssuerIsPresent() {
		final var issuer = repository.lookup(NAMESPACE, "https://gitlab.com");

		assertThat(issuer)
				.isNotNull()
				.returns("https://gitlab.com", TrustedIssuerRegistration::issuerUri)
				.returns("GitLab", TrustedIssuerRegistration::name)
				.returns(null, TrustedIssuerRegistration::jwksUri)
				.satisfies(it -> assertThat(it.allowedAudiences()).isEmpty());
	}

	@ParameterizedTest
	@DisplayName("unknown issuer URIs return null")
	@ValueSource(strings = {
			"https://token.actions.githubusercontent.com/",  // trailing slash
			"http://token.actions.githubusercontent.com",    // wrong scheme
			"https://gitlab.example.com",                    // self-hosted
			"https://jenkins.corp.example.com/oidc",         // Jenkins
			"https://unknown.example.com",
			""
	})
	void unknownIssuerReturnsNull(String unknownUri) {
		assertThat(repository.lookup(NAMESPACE, unknownUri)).isNull();
	}

	@Test
	@DisplayName("namespace parameter is ignored, all namespaces see the same entries")
	void namespaceIsIgnored() {
		assertThat(repository.lookup(NAMESPACE, "https://gitlab.com"))
				.isSameAs(repository.lookup(EntityId.from(100L), "https://gitlab.com"));
	}

}
