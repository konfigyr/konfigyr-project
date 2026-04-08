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
 *         {@link PropertyTransitionType#ADDED} – The property did not previously exist, meaning that
 *         the {@code oldValue} is {@code null} and {@code newValue} is non-null.
 *     </li>
 *     <li>
 *         {@link PropertyTransitionType#UPDATED} – The property value changed where both {@code oldValue}
 *         and {@code newValue} can not be {@code null}.
 *     </li>
 *     <li>
 *         {@link PropertyTransitionType#REMOVED} – The property was removed. The value of the {@code oldValue}
 *         is never {@code null} and is equal to the current value of the property and the {@code newValue}
 *         is {@code null}
 *     </li>
 * </ul>
 * <p>
 * If configuration values may contain sensitive data, callers must ensure that exposing {@code oldValue}
 * or {@code newValue} complies with the system's data protection policy.
 *
 * @param revision the revision number of the change, never {@literal null}
 * @param name the name of the property that was changed, never {@literal null}
 * @param action the type of property transition, or operation, never {@literal null}
 * @param from the value before the change, may be {@literal null} for additions
 * @param to the value after the change, may be {@literal null} for removals
 * @param appliedBy the information about the principal who performed the change, never {@literal null}
 * @param appliedAt the time at which the change occurred, never {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record PropertyHistory(
		@NonNull String revision,
		@NonNull String name,
		@NonNull PropertyTransitionType action,
		@Nullable PropertyValue from,
		@Nullable PropertyValue to,
		@NonNull String appliedBy,
		@NonNull OffsetDateTime appliedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6954957897221334983L;

}
