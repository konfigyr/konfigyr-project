package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.AbstractIntegrationTest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class NamespaceTrustedIssuerRepositoryTest extends AbstractIntegrationTest {

	static final EntityId KONFIGYR_NAMESPACE = EntityId.from(2L);
	static final EntityId PERSONAL_NAMESPACE = EntityId.from(1L);

	@Autowired
	DSLContext context;

	NamespaceTrustedIssuerRepository repository;

	@BeforeEach
	void setup() {
		repository = new NamespaceTrustedIssuerRepository(context, JsonMapper.shared());
	}

	@Test
	@DisplayName("should resolve active namespace issuer by namespace and issuer URI")
	void shouldResolveActiveIssuer() {
		final var registration = repository.lookup(KONFIGYR_NAMESPACE, "https://ci.konfigyr.com");

		assertThat(registration)
				.isNotNull()
				.returns("Konfigyr CI", TrustedIssuerRegistration::name)
				.returns("https://ci.konfigyr.com", TrustedIssuerRegistration::issuerUri)
				.returns("https://ci.konfigyr.com/jwks.json", TrustedIssuerRegistration::jwksUri)
				.satisfies(it -> assertThat(it.allowedAudiences())
						.containsExactly("konfigyr-api")
				);
	}

	@Test
	@DisplayName("should resolve active namespace issuer with no jwks_uri and empty audiences")
	void shouldResolveIssuerWithoutJwksUri() {
		final var registration = repository.lookup(KONFIGYR_NAMESPACE, "https://ci-staging.konfigyr.com");

		assertThat(registration)
				.isNotNull()
				.returns("Konfigyr staging CI", TrustedIssuerRegistration::name)
				.returns("https://ci-staging.konfigyr.com", TrustedIssuerRegistration::issuerUri)
				.returns(null, TrustedIssuerRegistration::jwksUri)
				.satisfies(it -> assertThat(it.allowedAudiences()).isEmpty());
	}

	@Test
	@DisplayName("should return null for inactive issuer")
	void shouldReturnNullForInactiveIssuer() {
		assertThat(repository.lookup(KONFIGYR_NAMESPACE, "https://disabled.konfigyr.com")).isNull();
	}

	@Test
	@DisplayName("should return null when issuer URI does not match any registered issuer")
	void shouldReturnNullForUnknownIssuerUri() {
		assertThat(repository.lookup(KONFIGYR_NAMESPACE, "https://unknown.example.com")).isNull();
	}

	@Test
	@DisplayName("should return null when issuer is registered under a different namespace")
	void shouldReturnNullForWrongNamespace() {
		assertThat(repository.lookup(PERSONAL_NAMESPACE, "https://ci.konfigyr.com")).isNull();
	}

	@Test
	@DisplayName("should resolve issuer registered under a different namespace")
	void shouldResolveIssuerForCorrectNamespace() {
		assertThat(repository.lookup(PERSONAL_NAMESPACE, "https://ci.john-doe.example.com"))
				.isNotNull()
				.returns("Personal CI", TrustedIssuerRegistration::name);
	}

}
