package com.konfigyr.vault;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Represents a historical record of a configuration property mutation and models a temporal change
 * event rather than a structural difference.
 * <p>
 * A {@link PropertyHistory} entry describes a single state transition of a configuration property, including:
 * <ul>
 *     <li>The type of change that occurred</li>
 *     <li>The author responsible for the change</li>
 *     <li>The previous property value (if applicable)</li>
 *     <li>The new property value (if applicable)</li>
 *     <li>The timestamp of the change</li>
 * </ul>
 * <p>
 * The property mutation type is one of:
 * <ul>
 *     <li>
 *         {@link Action#ADDED} – The property did not previously exist, meaning that the {@code oldValue}
 *         is {@code null} and {@code newValue} is non-null.
 *     </li>
 *     <li>
 *         {@link Action#UPDATED} – The property value changed where both {@code oldValue} and
 *         {@code newValue} can not be {@code null}.
 *     </li>
 *     <li>
 *         {@link Action#REMOVED} – The property was removed. The value of the {@code oldValue} is
 *         never {@code null} and is equal to the current value of the property and the
 *         {@code newValue} is {@code null}</li>
 * </ul>
 * <p>
 * If configuration values may contain sensitive data, callers must ensure that exposing {@code oldValue}
 * or {@code newValue} complies with the system's data protection policy.
 *
 * @param action the type of property mutation, never {@literal null}
 * @param author the information about the principal who performed the change, never {@literal null}
 * @param newValue the value after the change, may be {@literal null} for removals
 * @param oldValue the value before the change, may be {@literal null} for additions
 * @param timestamp the time at which the change occurred, never {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record PropertyHistory(
		@NonNull Action action,
		@NonNull String author,
		@Nullable String newValue,
		@Nullable String oldValue,
		@NonNull OffsetDateTime timestamp
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6954957897221334983L;

	/**
	 * Describes the type of property mutation represented by a {@link PropertyHistory} record.
	 */
	public enum Action {

		/**
		 * Indicates that a new property was introduced.
		 */
		ADDED,

		/**
		 * Indicates that an existing property value was modified.
		 */
		UPDATED,

		/**
		 * Indicates that an existing property was removed.
		 */
		REMOVED
	}

	/**
	 * Creates a new {@link PropertyHistory} instance that describes when the property was added.
	 *
	 * @param author the author of the change, cannot be {@literal null} or empty.
	 * @param value the new value of the property, cannot be {@literal null} or empty.
	 * @param timestamp the time at which the addition occurred, cannot be {@literal null}.
	 * @return the property history record, never {@literal null}.
	 */
	public static PropertyHistory added(String author, String value, OffsetDateTime timestamp) {
		return new PropertyHistory(Action.ADDED, author, value, null, timestamp);
	}

	/**
	 * Creates a new {@link PropertyHistory} instance that describes when the property was updated.
	 *
	 * @param author the author of the change, cannot be {@literal null} or empty.
	 * @param newValue the new value of the property, cannot be {@literal null} or empty.
	 * @param oldValue the old value of the property, cannot be {@literal null} or empty.
	 * @param timestamp the time at which the update occurred, cannot be {@literal null}.
	 * @return the property history record, never {@literal null}.
	 */
	public static PropertyHistory updated(String author, String newValue, String oldValue, OffsetDateTime timestamp) {
		return new PropertyHistory(Action.UPDATED, author, newValue, oldValue, timestamp);
	}

	/**
	 * Creates a new {@link PropertyHistory} instance that describes when the property was removed.
	 *
	 * @param author the author of the change, cannot be {@literal null} or empty.
	 * @param value the value of the property at the time when it was removed, cannot be {@literal null} or empty.
	 * @param timestamp the time at which the removal occurred, cannot be {@literal null}.
	 * @return the property history record, never {@literal null}.
	 */
	public static PropertyHistory removed(String author, String value, OffsetDateTime timestamp) {
		return new PropertyHistory(Action.REMOVED, author, null, value, timestamp);
	}

}
