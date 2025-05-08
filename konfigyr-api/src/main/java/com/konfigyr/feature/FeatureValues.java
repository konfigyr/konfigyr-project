package com.konfigyr.feature;

import lombok.Value;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Container that stores the {@link FeatureValue feature values} for {@link FeatureDefinition feature definitions}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Value
@ValueObject
public class FeatureValues implements Serializable {

	@Serial
	private static final long serialVersionUID = 294304163437354662L;

	private static final FeatureValues EMPTY = new FeatureValues(Collections.emptyMap());

	/**
	 * Method that would create an empty instance of {@link FeatureValues}.
	 *
	 * @return an empty features values instance, never {@literal null}
	 */
	@NonNull
	public static FeatureValues empty() {
		return EMPTY;
	}

	/**
	 * Creates a fluent builder that can be used to register {@link FeatureValue feature values}.
	 *
	 * @return feature values builder, never {@literal null}.
	 */
	@NonNull
	public static Builder builder() {
		return new Builder();
	}

	Map<String, FeatureValue> values;

	private FeatureValues(Map<String, FeatureValue> values) {
		Assert.notNull(values, "Feature values must not be null");
		this.values = Collections.unmodifiableMap(values);
	}

	/**
	 * Retrieves a {@link FeatureValue} from this value container for a matching {@link FeatureDefinition}.
	 * <p>
	 * This method may return an empty {@link Optional} if no value exists for a given definition.
	 *
	 * @param definition feature definition for which value should be extracted, can't be {@literal null}
	 * @param <T> the generic feature value type
	 * @return matching feature definition value or an empty {@link Optional}.
	 * @throws IllegalStateException when the feature value type does not match the one in the definition.
	 */
	@SuppressWarnings("unchecked")
	public <T extends FeatureValue> Optional<T> get(@NonNull FeatureDefinition<T> definition) {
		final FeatureValue value = values.get(definition.name());

		if (value == null) {
			return Optional.empty();
		}

		if (definition.type().isInstance(value)) {
			return Optional.of((T) value);
		}

		throw new IllegalStateException("Feature value of " + value + " does not match the defined type of " +
				definition.type() + " for feature '" + definition.name() + "'.");
	}

	/**
	 * Copies the {@link FeatureValue feature values} from the specified container to this {@link FeatureValues}
	 * instance. The {@link FeatureValue feature value} specified on this container may be overwritten by
	 * this operation if the specified container contains a different value for the feature.
	 *
	 * @param other feature values container to be merged, can't be {@literal null}
	 * @return merged feature values, never {@link null}
	 */
	@NonNull
	public FeatureValues concat(@NonNull FeatureValues other) {
		if (other.isEmpty()) {
			return this;
		}

		return builder()
				.add(this.values)
				.add(other.values)
				.build();
	}

	/**
	 * Checks if this {@link FeatureValues} container is empty.
	 *
	 * @return {@code true} when this container contains feature values, {@code false} otherwise.
	 */
	public boolean isEmpty() {
		return CollectionUtils.isEmpty(values);
	}

	@Override
	public String toString() {
		if (isEmpty()) {
			return "FeatureValues(empty)";
		}

		final StringBuilder builder = new StringBuilder("FeatureValues(");
		final Iterator<Map.Entry<String, FeatureValue>> iterator = values.entrySet().iterator();

		while (iterator.hasNext()) {
			final Map.Entry<String, FeatureValue> entry = iterator.next();

			builder.append(entry.getKey())
					.append("=")
					.append(entry.getValue());

			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}

		return builder.append(')').toString();
	}

	public static final class Builder {
		private final Map<String, FeatureValue> values = new TreeMap<>();

		/**
		 * Register the {@link FeatureValue} that matches the given {@link FeatureDefinition}.
		 *
		 * @param definition feature for which value should be added
		 * @param value feature value to be added
		 * @param <T> feature value type
		 * @return feature values builder, never {@literal null}
		 */
		@NonNull
		public <T extends FeatureValue> Builder add(FeatureDefinition<T> definition, T value) {
			if (definition != null && value != null) {
				return add(definition.name(), value);
			}
			return this;
		}

		/**
		 * Register the {@link FeatureValue} that matches the given {@link FeatureDefinition} name.
		 *
		 * @param name name of the feature for which value should be added
		 * @param value feature value to be added
		 * @return feature values builder, never {@literal null}
		 */
		@NonNull
		public Builder add(String name, FeatureValue value) {
			if (name != null && value != null) {
				values.put(name, value);
			}
			return this;
		}

		/**
		 * Register a map of {@link FeatureValue feature values}.
		 *
		 * @param values feature values map to be added
		 * @return feature values builder, never {@literal null}
		 */
		@NonNull
		public Builder add(Map<String, FeatureValue> values) {
			if (!CollectionUtils.isEmpty(values)) {
				values.forEach(this::add);
			}
			return this;
		}

		/**
		 * Creates the {@link FeatureValues} instance using the values that were added to this builder.
		 *
		 * @return feature values, never {@literal null}.
		 */
		@NonNull
		public FeatureValues build() {
			return values.isEmpty() ? empty() : new FeatureValues(values);
		}
	}

}
