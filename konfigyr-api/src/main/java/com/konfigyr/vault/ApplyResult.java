package com.konfigyr.vault;

import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Result of a successful direct apply operation on a {@link Profile} that was performed using the
 * responsible {@link Vault} instance.
 * <p>
 * Indicates that the provided {@link PropertyChanges} were successfully merged into the target
 * profile repository state branch.
 *
 * @param revision resulting revision on the profile branch, can't be {@literal null}
 * @param changes changes that were applied, can't be {@literal null}
 * @param timestamp the timestamp when the changes were applied, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public record ApplyResult(
		String revision,
		Map<String, PropertyHistory> changes,
		OffsetDateTime timestamp
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 658462896042779024L;

}
