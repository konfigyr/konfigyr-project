package com.konfigyr.identity.authorization.jwk;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;

import static org.assertj.core.api.Assertions.*;

@TestProfile
@Transactional
@SpringBootTest
@ImportTestcontainers(TestContainers.class)
class KeyRepositoryTest {

	@Autowired
	KeyRepository repository;

	@Test
	@DisplayName("should create key")
	void shouldCreateKey() {
		final var key = repository.create(KeyAlgorithm.RS256, Period.ofDays(30));

		assertThatObject(key)
				.isNotNull()
				.isInstanceOf(RSAKey.class)
				.asInstanceOf(InstanceOfAssertFactories.type(RSAKey.class))
				.returns(KeyType.RSA, RSAKey::getKeyType)
				.returns(KeyUse.SIGNATURE, RSAKey::getKeyUse)
				.returns(JWSAlgorithm.RS256, RSAKey::getAlgorithm)
				.satisfies(it -> assertThat(it.getKeyID())
						.isNotNull()
						.isAlphanumeric()
						.isPrintable()
				)
				.satisfies(it -> assertThat(it.getKeyOperations())
						.isNotNull()
						.containsExactlyInAnyOrder(KeyOperation.SIGN, KeyOperation.VERIFY)
				)
				.satisfies(it -> assertThat(it.getIssueTime())
						.isNotNull()
						.isCloseTo(Instant.now(), Duration.ofSeconds(1).toMillis())
				)
				.satisfies(it -> assertThat(it.getNotBeforeTime())
						.isNotNull()
						.isCloseTo(Instant.now(), Duration.ofSeconds(1).toMillis())
				)
				.satisfies(it -> assertThat(it.getExpirationTime())
						.isNotNull()
						.isCloseTo(Instant.now().plus(Duration.ofDays(30)), Duration.ofSeconds(1).toMillis())
				);

		assertThat(repository.get())
				.contains(key);

		assertThat(repository.get(key.getKeyID()))
				.isPresent()
				.hasValue(key);
	}

	@Test
	@DisplayName("should retrieve keys")
	void shouldRetrieveKeys() {
		assertThat(repository.get())
				.isNotEmpty()
				.hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("should delete key")
	void shouldDeleteKey() {
		final var key = repository.create(KeyAlgorithm.ECDH_ES_A128KW, Period.ofDays(30));

		assertThatNoException().isThrownBy(() -> repository.delete(key.getKeyID()));

		assertThat(repository.get(key.getKeyID()))
				.isEmpty();
	}

}
