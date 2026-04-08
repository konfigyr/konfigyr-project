package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * A change history entry represents an immutable, authoritative record of a configuration change applied
 * to a {@link Profile}. They are used for auditing, incident investigation, or compliance reporting
 * as they provide answers to questions like:
 * <ul>
 *    <li>What changed?</li>
 *    <li>When did it change?</li>
 *    <li>Who authorized it?</li>
 * </ul>
 * <p>
 * {@link ChangeHistory} is the primary audit log of the system. History entries are immutable, append-only,
 * and each entry corresponds to exactly one applied changeset.
 *
 * @param id unique identifier of the change history entry, can't be {@literal null}.
 * @param revision revision number of the changeset that was applied, can't be {@literal null}.
 * @param subject human-readable subject of the change, can't be {@literal null}.
 * @param description human-readable summary of the change, can't be {@literal null}.
 * @param count the number of property changes that were applied; can't be negative.
 * @param appliedBy name of the user that applied the change, can't be {@literal null}.
 * @param appliedAt timestamp when the change was applied, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record ChangeHistory(
		EntityId id,
		String revision,
		String subject,
		String description,
		int count,
		String appliedBy,
		OffsetDateTime appliedAt
) implements Comparable<ChangeHistory>, Serializable {

	@Override
	public int compareTo(ChangeHistory o) {
		return appliedAt.compareTo(o.appliedAt);
	}

}
