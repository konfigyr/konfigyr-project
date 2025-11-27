package com.konfigyr.feature;

import lombok.EqualsAndHashCode;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Value object that contains all the available {@link FeatureDefinition feature definitions} that were
 * registered by different parts of the system via {@link FeatureDefinitionConfigurer}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see FeatureDefinitionConfigurer
 */
@ValueObject
@EqualsAndHashCode
public final class FeatureDefinitions implements Iterable<FeatureDefinition<?>>, Serializable {

	private final Map<String, FeatureDefinition<?>> definitions;

	FeatureDefinitions(FeatureDefinition<?>... definitions) {
		this(Arrays.asList(definitions));
	}

	FeatureDefinitions(Collection<FeatureDefinition<?>> definitions) {
		Assert.notNull(definitions, "Features definitions must not be null");

		final Map<String, FeatureDefinition<?>> bucket = new TreeMap<>();

		for (FeatureDefinition<?> definition : definitions) {
			if (bucket.containsKey(definition.name())) {
				throw new IllegalArgumentException("You have attempted to register a Feature Definition that " +
						"is already registered: " + definition);
			}
			bucket.put(definition.name(), definition);
		}

		this.definitions = Collections.unmodifiableMap(bucket);
	}

	/**
	 * Retrieve the {@link FeatureDefinition} that matches the specified feature name from this registry.
	 *
	 * @param name name of the feature definition to be retrieved, can't be {@literal null}
	 * @param <T> the feature value type
	 * @return {@link Optional} containing the matching definition or empty.
	 */
	@NonNull
	@SuppressWarnings("unchecked")
	public <T extends FeatureValue> Optional<FeatureDefinition<T>> get(@NonNull String name) {
		return Optional.ofNullable((FeatureDefinition<T>) definitions.get(name));
	}

	/**
	 * Checks if this registry contains any {@link FeatureDefinition} with a matching name.
	 *
	 * @param name name of the feature definition to be checked, can't be {@literal null}
	 * @return {@code true} when there is a matching definition.
	 */
	public boolean has(@NonNull String name) {
		return definitions.containsKey(name);
	}

	/**
	 * Checks if this registry contains this {@link FeatureDefinition}.
	 *
	 * @param definition feature definition to be checked, can't be {@literal null}
	 * @return {@code true} when there is a matching definition.
	 */
	public boolean has(@NonNull FeatureDefinition<?> definition) {
		return has(definition.name());
	}

	/**
	 * Creates a stream of {@link FeatureDefinition feature definitions} that are part of this registry.
	 *
	 * @return feature definition stream, never {@literal null}
	 */
	@NonNull
	public Stream<FeatureDefinition<?>> stream() {
		return definitions.values().stream();
	}

	@NonNull
	@Override
	public Iterator<FeatureDefinition<?>> iterator() {
		return definitions.values().iterator();
	}

	@Override
	public String toString() {
		return definitions.keySet().stream().collect(Collectors.joining(", ", "FeatureDefinitions(", ")"));
	}
}
