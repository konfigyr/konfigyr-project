package com.konfigyr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncodersTest {

	@Test
	@DisplayName("should create Argon password encoder")
	void createArgonEncoder() {
		assertThat(PasswordEncoders.argon())
				.isInstanceOf(Argon2PasswordEncoder.class)
				.isSameAs(PasswordEncoders.argon());
	}

	@Test
	@DisplayName("should create delegating password encoder using Argon as default encoding algorithm")
	void createDelegatingEncoder() {
		final var encoder = PasswordEncoders.get();

		assertThat(encoder.encode("some password"))
				.isNotBlank()
				.startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$")
				.matches(encoded -> encoder.matches("some password", encoded));
	}

	@Test
	@DisplayName("should be able to match BCrypt passwords generated with default Spring password encoder")
	void matchPasswordsWithBcrypt() {
		final var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		assertThat(encoder.encode("some password"))
				.isNotBlank()
				.matches(encoded -> PasswordEncoders.get().matches("some password", encoded));
	}

	@Test
	@DisplayName("should be able to match Argon passwords using default Spring password encoder")
	void matchPasswordsWithArgon() {
		assertThat(PasswordEncoders.get().encode("some password"))
				.isNotBlank()
				.matches(encoded -> PasswordEncoderFactories.createDelegatingPasswordEncoder()
						.matches("some password", encoded)
				);
	}

}
