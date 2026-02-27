package com.konfigyr.vault;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a single atomic modification to a configuration property.
 * <p>
 * A {@link PropertyChange} describes the intended mutation of a single property within a configuration state.
 * It does not represent the full configuration, but rather a delta to be applied.
 * <p>
 * The following operations are supported:
 * <ul>
 *     <li>{@link Operation#CREATE} – A new property should be added.</li>
 *     <li>{@link Operation#MODIFY} – An existing property should be updated.</li>
 *     <li>{@link Operation#REMOVE} – An existing property should be removed.</li>
 * </ul>
 *
 * @param name the property name, must not be {@literal null}.
 * @param operation the operation to perform, must not be {@literal null}.
 * @param value the new value (if applicable), may be {@literal null} only for the {@link Operation#REMOVE} operation.
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PropertyChanges
 */
@NullMarked
@ValueObject
public record PropertyChange(String name, Operation operation, @Nullable String value) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5957767294740708563L;

	/**
	 * Enumeration that defines which operation should be performed by this property change.
	 */
	public enum Operation {
		/**
		 * Creates a new property.
		 */
		CREATE,

		/**
		 * Modifies an existing property.
		 */
		MODIFY,

		/**
		 * Removes an existing property.
		 */
		REMOVE
	}

	/**
	 * Creates a new {@link PropertyChange} instance that should add a new property with
	 * the given name and value to the {@link Vault} state.
	 *
	 * @param name the configuration property name, cannot be {@literal null} or empty.
	 * @param value the configuration property value, cannot be {@literal null}.
	 * @return property change that would add the property to the state, never {@literal null}.
	 */
	public static PropertyChange create(String name, String value) {
		return new PropertyChange(name, Operation.CREATE, value);
	}

	/**
	 * Creates a new {@link PropertyChange} instance that should modify an existing property with
	 * the given name and value to the {@link Vault} state.
	 *
	 * @param name the configuration property name, cannot be {@literal null} or empty.
	 * @param value the configuration property value, cannot be {@literal null}.
	 * @return property change that would modify the property to the state, never {@literal null}.
	 */
	public static PropertyChange modify(String name, String value) {
		return new PropertyChange(name, Operation.MODIFY, value);
	}

	/**
	 * Creates a new {@link PropertyChange} instance that should remove an existing property with
	 * the given name.
	 *
	 * @param name the configuration property name, cannot be {@literal null} or empty.
	 * @return property change that would remove the property to the state, never {@literal null}.
	 */
	public static PropertyChange remove(String name) {
		return new PropertyChange(name, Operation.REMOVE, null);
	}

}
