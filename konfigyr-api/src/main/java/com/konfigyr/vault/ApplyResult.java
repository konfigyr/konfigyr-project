package com.konfigyr.vault;

import com.konfigyr.security.AuthenticatedPrincipal;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Set;

/**
 * Result of a successful direct apply operation on a {@link Profile} that was performed using the
 * responsible {@link Vault} instance.
 * <p>
 * Indicates that the provided {@link PropertyChanges} were successfully merged into the target
 * profile repository state branch.
 *
 * @param revision resulting revision on the profile branch, can't be {@literal null}
 * @param previousRevision the revision of the profile branch before the applied changes, can be {@literal null}
 * @param subject a short summary of the change, must not be {@code null}
 * @param description an optional detailed explanation, may be {@code null}
 * @param changes changes that were applied, can't be {@literal null}
 * @param author the author of the changes that were applied, can't be {@literal null}
 * @param timestamp the timestamp when the changes were applied, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record ApplyResult(
		String revision,
		@Nullable String previousRevision,
		String subject,
		@Nullable String description,
		Set<PropertyTransition> changes,
		AuthenticatedPrincipal author,
		OffsetDateTime timestamp
) implements Iterable<PropertyTransition>, Serializable {

	@Serial
	private static final long serialVersionUID = 658462896042779024L;

	@Override
	public Iterator<PropertyTransition> iterator() {
		return changes.iterator();
	}
}
