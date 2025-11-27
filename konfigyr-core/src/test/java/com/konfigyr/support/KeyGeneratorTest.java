package com.konfigyr.support;

import org.apache.commons.codec.CodecPolicy;
import org.apache.commons.codec.binary.Base16;
import org.assertj.core.internal.StandardComparisonStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class KeyGeneratorTest {

	final KeyGenerator generator = KeyGenerator.getInstance();

	@Test
	@DisplayName("should generate Base16 encoded key and 16 characters long")
	void shouldGenerateKey() {
		final Base16 base16 = new Base16(false, CodecPolicy.STRICT);
		final String key = generator.generateKey();

		assertThat(key)
				.isNotBlank()
				.hasSize(16)
				.isAlphanumeric()
				.isPrintable();

		assertThatNoException().isThrownBy(() -> base16.decode(key));
	}

	@Test
	@DisplayName("should generate unique keys")
	void shouldGenerateUniqueKey() {
		final var executor = new SimpleAsyncTaskExecutorBuilder()
				.concurrencyLimit(5)
				.virtualThreads(true)
				.build();

		final List<String> generated = new CopyOnWriteArrayList<>();
		final List<CompletableFuture<Void>> tasks = new ArrayList<>();

		final Runnable task = () -> generated.add(generator.generateKey());

		for (int i = 0; i < 1000; i++) {
			tasks.add(CompletableFuture.runAsync(task, executor));
		}

		final var future = CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));

		assertThatNoException()
				.as("should generate identifiers within 300 milliseconds")
				.isThrownBy(() -> future.get(300, TimeUnit.MILLISECONDS));

		assertThat(generated)
				.as("should generate 1000 unique keys")
				.hasSizeGreaterThanOrEqualTo(1000);

		assertThat(StandardComparisonStrategy.instance().duplicatesFrom(generated))
				.as("should not encounter any collisions")
				.isEmpty();

		executor.close();
	}

}
