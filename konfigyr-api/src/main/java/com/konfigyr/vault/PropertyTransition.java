package com.konfigyr.vault;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a discrete state change of a specific property over time or during an event.
 * <p>
 * This record acts as a point-in-time diff, capturing the state of a configuration property value
 * before and after a modification occurred. It is designed to populate audit logs, history tables,
 * or timeline visualizations in a user interface.
 * <p>
 * The associated {@link PropertyValue}s are maintained in a sealed state, ensuring that the underlying
 * data is encrypted and can be safely transported over the network or stored at rest without exposing
 * sensitive information.
 *
 * @param name name of the property being transitioned, can't be {@literal null} or empty.
 * @param type the type of transition, or operation, that occurred, can't be {@literal null}.
 * @param from value of the property <i>prior</i> to the transition taking place.
 *             It may be {@literal null} if the property was just created.
 * @param to   value of the property <i>resulting</i> from the transition.
 *             It may be {@literal null} if the property was deleted or cleared.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PropertyValue
 */
@NullMarked
@ValueObject
public record PropertyTransition(
		String name,
		PropertyTransitionType type,
		@Nullable PropertyValue from,
		@Nullable PropertyValue to
) implements Comparable<PropertyTransition>, Serializable {

	@Serial
	private static final long serialVersionUID = 5437834396574001056L;

	/**
	 * Creates a new {@link PropertyTransition} instance with the name and value of the property that was added.
	 *
	 * @param name the name of the property that was added, cannot be {@literal null} or empty.
	 * @param value the new value of the property, cannot be {@literal null} or empty.
	 * @return the property transition record, never {@literal null}.
	 */
	public static PropertyTransition added(String name, PropertyValue value) {
		return new PropertyTransition(name, PropertyTransitionType.ADDED, null, value);
	}

	/**
	 * Creates a new {@link PropertyTransition} instance that describes when the property update operation
	 * with both previous and new value states.
	 *
	 * @param name the name of the property that was updated, cannot be {@literal null} or empty.
	 * @param from the previous value of the property, cannot be {@literal null} or empty.
	 * @param to the new value of the property, cannot be {@literal null} or empty.
	 * @return the property transition record, never {@literal null}.
	 */
	public static PropertyTransition updated(String name, PropertyValue from, PropertyValue to) {
		return new PropertyTransition(name, PropertyTransitionType.UPDATED, from, to);
	}

	/**
	 * Creates a new {@link PropertyTransition} instance with the name and value of the property that was removed.
	 *
	 * @param name the name of the property that was removed, cannot be {@literal null} or empty.
	 * @param value the value of the property at the time when it was removed, cannot be {@literal null} or empty.
	 * @return the property transition record, never {@literal null}.
	 */
	public static PropertyTransition removed(String name, PropertyValue value) {
		return new PropertyTransition(name, PropertyTransitionType.REMOVED, value, null);
	}

	@Override
	public int compareTo(PropertyTransition o) {
		return Objects.compare(name, o.name, String::compareTo);
	}

}
