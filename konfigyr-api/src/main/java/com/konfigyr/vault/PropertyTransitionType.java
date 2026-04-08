package com.konfigyr.vault;

/**
 * Enumeration of different mutation states of a {@link PropertyValue} during a lifecycle event.
 * <p>
 * This enumeration defines the exact nature of a {@link PropertyTransition}, allowing UI layers, audit
 * processors, and downstream consumers to understand at a glance how a property moved from its previous
 * state to its current state.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PropertyTransition
 */
public enum PropertyTransitionType {

	/**
	 * Indicates that a new property was introduced where none previously existed.
	 * <p>
	 * In a standard {@link PropertyTransition}, this typically implies that the {@code from} value was
	 * logically {@code null} or empty, and the {@code to} value now holds the newly created state.
	 */
	ADDED,

	/**
	 * Indicates that an existing property value was modified.
	 * <p>
	 * This represents a state change where both the {@code from} and {@code to} values in a
	 * {@link PropertyTransition} are present but distinct. This is the most common state for standard
	 * auditing of user edits or automated system updates.
	 */
	UPDATED,

	/**
	 * Indicates that an existing property was removed or purged from the system.
	 * <p>
	 * In a standard {@link PropertyTransition}, this typically implies that the {@code from} value
	 * held the terminal state of the data, and the {@code to} value has now been rendered logically
	 * {@code null} or cleared.
	 */
	REMOVED

}
