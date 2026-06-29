package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Registry of {@link VerificationStrategy} implementations, indexed by {@link VerificationMethod}.
 * <p>
 * Strategies are supplied at construction time via an {@link Iterable}; {@literal null} entries are
 * silently discarded. Each non-null strategy is keyed by its {@link VerificationStrategy#method()}
 * declaration. Registering two strategies for the same {@link VerificationMethod} is a configuration
 * error and causes an {@link IllegalArgumentException} to be thrown at construction time.
 * <p>
 * {@link #get(VerificationMethod)} performs an O(1) map lookup and throws
 * {@link IllegalStateException} when no strategy is registered for the requested method.
 * <p>
 * This type is the single point of strategy lookup used by {@link DefaultGroupVerifications}, keeping
 * that class closed to changes when new {@link VerificationMethod} values are added.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see VerificationStrategy
 * @see VerificationMethod
 */
@NullMarked
final class VerificationStrategies {

	private final Map<VerificationMethod, VerificationStrategy> strategies;

	/**
	 * Creates a registry from the supplied strategies. {@literal null} entries in the iterable are
	 * ignored. Registering two strategies for the same {@link VerificationMethod} throws an
	 * {@link IllegalArgumentException} identifying both conflicting implementations.
	 *
	 * @param strategies the strategies to register; may contain {@literal null} entries
	 * @throws IllegalArgumentException when two verification strategy implementations are declared
	 * for the same group verification method
	 */
	VerificationStrategies(Iterable<@Nullable VerificationStrategy> strategies) {
		this.strategies = StreamSupport.stream(strategies.spliterator(), false)
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableMap(
						VerificationStrategy::method,
						Function.identity(),
						(existing, duplicate) -> {
							throw new IllegalArgumentException(
									"Duplicate VerificationStrategy registered for %s verification method: %s and %s".formatted(
											duplicate.method(),
											ClassUtils.getQualifiedName(existing.getClass()),
											ClassUtils.getQualifiedName(duplicate.getClass())
									));
						}));
	}

	/**
	 * Returns the {@link VerificationStrategy} registered for the given {@link VerificationMethod}.
	 *
	 * @param method the verification method to look up; never {@literal null}
	 * @return the matching strategy; never {@literal null}
	 * @throws IllegalStateException when no strategy is registered for the given method
	 */
	VerificationStrategy get(VerificationMethod method) {
		final VerificationStrategy strategy = strategies.get(method);
		if (strategy == null) {
			throw new IllegalStateException("No verification strategy implementation found for %s verification method".formatted(method));
		}
		return strategy;
	}
}
