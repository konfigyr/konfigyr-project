package com.konfigyr.identity.authorization.jwk;

import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;

import static org.assertj.core.api.Assertions.*;

class KeyAlgorithmTest {

	final Period expiration = Period.ofDays(30);

	@Test
	@DisplayName("should generate RSA encryption key")
	void generateRsaEncryptionKey() throws Exception {
		assertThatObject(KeyAlgorithm.RSA_OAEP_256.generate(expiration))
				.isNotNull()
				.isInstanceOf(RSAKey.class)
				.asInstanceOf(InstanceOfAssertFactories.type(RSAKey.class))
				.returns(KeyType.RSA, RSAKey::getKeyType)
				.returns(KeyUse.ENCRYPTION, RSAKey::getKeyUse)
				.returns(JWEAlgorithm.RSA_OAEP_256, RSAKey::getAlgorithm)
				.satisfies(it -> assertThat(it.getKeyID())
						.isNotNull()
						.isAlphanumeric()
						.isPrintable()
				)
				.satisfies(it -> assertThat(it.getKeyOperations())
						.isNotNull()
						.containsExactlyInAnyOrder(KeyOperation.ENCRYPT, KeyOperation.DECRYPT)
				)
				.satisfies(it -> assertThat(it.toPublicKey())
						.isNotNull()
						.isInstanceOf(RSAPublicKey.class)
				)
				.satisfies(it -> assertThat(it.toPrivateKey())
						.isNotNull()
						.isInstanceOf(RSAPrivateKey.class)
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
						.isCloseTo(Instant.now().plus(expiration), Duration.ofSeconds(1).toMillis())
				);
	}

	@Test
	@DisplayName("should generate RSA signing key")
	void generateRsaSigningKey() throws Exception {
		assertThatObject(KeyAlgorithm.RS256.generate(expiration))
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
				.satisfies(it -> assertThat(it.toPublicKey())
						.isNotNull()
						.isInstanceOf(RSAPublicKey.class)
				)
				.satisfies(it -> assertThat(it.toPrivateKey())
						.isNotNull()
						.isInstanceOf(RSAPrivateKey.class)
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
						.isCloseTo(Instant.now().plus(expiration), Duration.ofSeconds(1).toMillis())
				);
	}

	@Test
	@DisplayName("should generate EC encryption key")
	void generateECEncryptionKey() throws Exception {
		assertThatObject(KeyAlgorithm.ECDH_ES_A128KW.generate(expiration))
				.isNotNull()
				.isInstanceOf(ECKey.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ECKey.class))
				.returns(KeyType.EC, ECKey::getKeyType)
				.returns(KeyUse.ENCRYPTION, ECKey::getKeyUse)
				.returns(JWEAlgorithm.ECDH_ES_A128KW, ECKey::getAlgorithm)
				.satisfies(it -> assertThat(it.getKeyID())
						.isNotNull()
						.isAlphanumeric()
						.isPrintable()
				)
				.satisfies(it -> assertThat(it.getKeyOperations())
						.isNotNull()
						.containsExactlyInAnyOrder(KeyOperation.ENCRYPT, KeyOperation.DECRYPT)
				)
				.satisfies(it -> assertThat(it.toPublicKey())
						.isNotNull()
						.isInstanceOf(ECPublicKey.class)
				)
				.satisfies(it -> assertThat(it.toPrivateKey())
						.isNotNull()
						.isInstanceOf(ECPrivateKey.class)
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
						.isCloseTo(Instant.now().plus(expiration), Duration.ofSeconds(1).toMillis())
				);
	}

	@Test
	@DisplayName("should generate EC signing key")
	void generateECSigningKey() throws Exception {
		assertThatObject(KeyAlgorithm.ES256.generate(expiration))
				.isNotNull()
				.isInstanceOf(ECKey.class)
				.asInstanceOf(InstanceOfAssertFactories.type(ECKey.class))
				.returns(KeyType.EC, ECKey::getKeyType)
				.returns(KeyUse.SIGNATURE, ECKey::getKeyUse)
				.returns(JWSAlgorithm.ES256, ECKey::getAlgorithm)
				.satisfies(it -> assertThat(it.getKeyID())
						.isNotNull()
						.isAlphanumeric()
						.isPrintable()
				)
				.satisfies(it -> assertThat(it.getKeyOperations())
						.isNotNull()
						.containsExactlyInAnyOrder(KeyOperation.SIGN, KeyOperation.VERIFY)
				)
				.satisfies(it -> assertThat(it.toPublicKey())
						.isNotNull()
						.isInstanceOf(ECPublicKey.class)
				)
				.satisfies(it -> assertThat(it.toPrivateKey())
						.isNotNull()
						.isInstanceOf(ECPrivateKey.class)
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
						.isCloseTo(Instant.now().plus(expiration), Duration.ofSeconds(1).toMillis())
				);
	}

	@Test
	@DisplayName("should fail to generate key with non-positive expiration period")
	void shouldCheckExpirationPeriod() {
		assertThatThrownBy(() -> KeyAlgorithm.ES256.generate(Period.ofDays(10).negated()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JWK expiration period must be positive");

		assertThatThrownBy(() -> KeyAlgorithm.ES256.generate(Period.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JWK expiration period must be positive");
	}

}
