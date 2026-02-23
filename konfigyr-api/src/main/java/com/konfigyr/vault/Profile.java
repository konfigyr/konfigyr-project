package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import lombok.Builder;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Profiles represent logical configuration environments for a {@link com.konfigyr.namespace.Service}.
 * <p>
 * Conceptually, a {@link Profile} models an operational context such as {@code development}, {@code staging},
 * or {@code production}. It is the authoritative source of configuration values that are ultimately served
 * to applications at runtime.
 * <p>
 * Users can choose which {@link ProfilePolicy} their {@link Profile}s should use depending on their internal
 * process and security policies. Protected profiles require that {@code ChangeSet}s must go through a review
 * and an approval process, represented by the {@code ChangeRequest}, before changes can be applied.
 * Unprotected {@link Profile}s allow users to apply tiehr {@code ChangeSet}s directly without going
 * through a review process.
 * <p>
 * When working with {@link Profile}s, it is important to understand the following principles:
 * <ul>
 *     <li>
 *         A {@link Profile} has exactly one authoritative configuration state at any time. This state
 *         is being consumed by applications at runtime.
 *     </li>
 *     <li>
 *         Profiles themselves are not versioned or mutated directly. All changes made to the {@link Profile}
 *         occur indirectly via {@code ChangeSet}s, or {@code ChangeRequest}s if a profile is protected.
 *     </li>
 *     <li>
 *         Historical evolution of a {@link Profile} can be derived from applied {@code ChangeSet}s
 *         and recorded as ChangeHistory entries.
 *     </li>
 * </ul>
 * <p>
 * From a UI and UX perspective, profiles are what users select when choosing <strong>where</strong> they
 * are working, but they should <strong>never</strong> be allowed to edit the profile directly. When selecting
 * a profile, a user should be able to see:
 * <ul>
 *     <li>
 *         The currently active {@code ChangeSet} or the current authoritative state of the {@link Profile}
 *         which they can edit by creating a new draft {@code ChangeSet} as soon as a change is made.
 *     </li>
 *     <li>
 *         Validation rules and metadata for each configuration property that is present in the
 *         active {@code ChangeSet} or the current authoritative state of the {@link Profile}.
 *     </li>
 *     <li>
 *         Approval requirements and how their changes can be applied to the {@link Profile}.
 *     </li>
 * </ul>
 * <p>
 * Profiles can be regarded as a governance boundary, not a version control abstraction. Users should
 * never be exposed to branches, commits, or merges.
 *
 * @param id unique entity identifier of the profile, can't be {@literal null}.
 * @param service unique identifier of the {@link Service} this profile belongs to, can't be {@literal null}.
 * @param slug unique profile identifier derived from its name, can't be {@literal null}.
 * @param name human-readable profile name, can't be {@literal null}.
 * @param description short description of the profile, can be {@literal null}.
 * @param policy access policy for this profile, can't be {@literal null}.
 * @param position the display order of the profile within the UI. Can't be negative or zero.
 * @param createdAt when was this profile created, can be {@literal null}.
 * @param updatedAt when was this profile last updated, can be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProfilePolicy
 */
@Entity
@Builder
public record Profile(
		@Identity EntityId id,
		@Association(aggregateType = Service.class) EntityId service,
		String slug,
		String name,
		ProfilePolicy policy,
		String description,
		int position,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) implements Comparable<Profile>, Serializable {

	@Serial
	private static final long serialVersionUID = 2657983964397225835L;

	@Override
	public int compareTo(@NonNull Profile o) {
		return Integer.compare(position, o.position);
	}
}
