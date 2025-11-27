package com.konfigyr.identity.authorization.jwk;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class SigningJwkSelectorTest {

	SigningJwkSelector selector = SigningJwkSelector.getInstance();

	@Test
	@DisplayName("should return null when no keys are defined")
	void emptyKeyCollection() {
		assertThat(selector.select(Collections.emptyList()))
				.isNull();
	}

	@Test
	@DisplayName("should return null when keys are not created for signing JWTs")
	void unsupportedKeyCollection() throws Exception {
		final var jwk = generate();

		assertThat(selector.select(Collections.singleton(jwk)))
				.isNull();
	}

	@Test
	@DisplayName("should return first key that supports signing JWTs")
	void supportedKeyCollection() throws Exception {
		final var signer = generate(KeyOperation.SIGN, KeyOperation.VERIFY);
		final var verifier = generate(KeyOperation.VERIFY);
		final var ignored = generate(KeyOperation.SIGN);

		assertThat(selector.select(List.of(signer, verifier, ignored)))
				.isSameAs(signer);
	}

	static JWK generate(KeyOperation... operations) throws Exception {
		return new OctetSequenceKeyGenerator(128)
				.keyOperations(Set.of(operations))
				.generate();
	}

}
