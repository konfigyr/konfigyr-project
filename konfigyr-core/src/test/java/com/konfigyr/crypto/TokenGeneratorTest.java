package com.konfigyr.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenGeneratorTest {

	@Test
	@DisplayName("should generate tokens using default random part length")
	void shouldGenerateDefaultTokens() {
		final var generator = TokenGenerator.getInstance();

		assertThat(generator.generateKey())
				.hasSize(32)
				.isAlphanumeric()
				.isHexadecimal()
				.isPrintable()
				.isLowerCase()
				.isNotEqualTo(generator.generateKey());
	}

	@Test
	@DisplayName("should validate random part length")
	void shouldValidateRandomPartLength() {
		assertThatThrownBy(() -> TokenGenerator.getInstance(4))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Key length must be greater than or equal to 8");

		assertThatThrownBy(() -> TokenGenerator.getInstance(512))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Key length must be less than or equal to 256");
	}

	@ValueSource(ints = { 8, 16, 32, 64, 128, 256 })
	@ParameterizedTest(name = "should generate tokens using {0} random part length")
	@DisplayName("should generate tokens using custom random part length")
	void shouldGenerateCustomTokens(int length) {
		final var generator = TokenGenerator.getInstance(length);

		assertThat(generator.generateKey())
				.hasSize(16 + (length * 2))
				.isAlphanumeric()
				.isHexadecimal()
				.isPrintable()
				.isLowerCase()
				.isNotEqualTo(generator.generateKey());
	}

	@SuppressWarnings("unchecked")
	@ValueSource(ints = { 8, 16, 32 })
	@DisplayName("should generate unique tokens")
	@ParameterizedTest(name = "should generate unique tokens using {0} random part length")
	void sanity(int length) throws Exception {
		final var generator = TokenGenerator.getInstance(length);

		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			final CompletableFuture<String>[] futures = IntStream.range(0, 1000)
					.mapToObj(i -> CompletableFuture.supplyAsync(generator::generateKey, executor))
					.toArray(CompletableFuture[]::new);

			CompletableFuture.allOf(futures).get(5, TimeUnit.MINUTES);

			assertThat(futures)
					.hasSize(1000)
					.extracting(CompletableFuture::get)
					.doesNotHaveDuplicates();
		}
	}

}
