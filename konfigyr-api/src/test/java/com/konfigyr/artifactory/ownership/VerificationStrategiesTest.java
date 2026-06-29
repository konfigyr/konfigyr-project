package com.konfigyr.artifactory.ownership;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificationStrategiesTest {

	@Test
	@DisplayName("should resolve strategy by verification method")
	void shouldResolveStrategyByMethod() {
		final VerificationStrategy dns = strategy(VerificationMethod.DNS);
		final VerificationStrategy sourceCode = strategy(VerificationMethod.SOURCE_CODE);

		final VerificationStrategies strategies = new VerificationStrategies(List.of(dns, sourceCode));

		assertThat(strategies.get(VerificationMethod.DNS)).isSameAs(dns);
		assertThat(strategies.get(VerificationMethod.SOURCE_CODE)).isSameAs(sourceCode);
	}

	@Test
	@DisplayName("should throw when two strategies are registered for the same method")
	void shouldThrowOnDuplicateMethod() {
		final List<VerificationStrategy> duplicates = List.of(
				strategy(VerificationMethod.DNS),
				strategy(VerificationMethod.DNS));

		assertThatThrownBy(() -> new VerificationStrategies(duplicates))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicate VerificationStrategy registered for %s verification method", VerificationMethod.DNS);
	}

	@Test
	@DisplayName("should throw when no strategy is registered for the requested method")
	void shouldThrowForUnregisteredMethod() {
		final VerificationStrategies strategies = new VerificationStrategies(
				List.of(strategy(VerificationMethod.DNS)));

		assertThatThrownBy(() -> strategies.get(VerificationMethod.SOURCE_CODE))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No verification strategy implementation found for %s verification method", VerificationMethod.SOURCE_CODE);
	}

	private static VerificationStrategy strategy(VerificationMethod method) {
		final VerificationStrategy mock = mock(VerificationStrategy.class);
		when(mock.method()).thenReturn(method);
		return mock;
	}

}
