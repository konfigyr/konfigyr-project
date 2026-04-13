package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * A change request represents a formal request to apply {@link PropertyChanges} to a {@link Profile}.
 * <p>
 * Conceptually, a {@link ChangeRequest} is a governance artifact, it separates <i>making changes</i>
 * from <i>authorizing changes</i> by providing a stable object for review, discussion, and approval.
 * For each submitted set of property changes a single {@link ChangeRequest} is created, and once
 * approved, the changes can be applied to the target {@link Profile}, thus producing a new
 * authoritative configuration state.
 * <p>
 * Change requests intentionally resemble, but do not expose, concepts like pull requests or merge
 * requests. They exist to enforce governance without leaking version control mechanics into the user
 * experience.
 * <p>
 * From a UI/UX perspective, the change requests are the primary object reviewers interact with. In a
 * perfect world, the reviewers should never edit configuration directly. They are limited to giving
 * their approval or rejecting the proposed changes.
 * <p>
 * Change requests can be identified using a human-friendly, sequential identifier of a change request
 * within the scope of a single {@link Service}. This approach intentionally mirrors the behavior of
 * systems like GitHub, where identifiers are not globally unique but are instead scoped to a parent
 * resource (in this case, a Service).
 * <p>
 * This number must have the following characteristics:
 * <ul>
 *     <li>
 *         <b>Scoped uniqueness</b>: The {@code number} is guaranteed to be unique only within
 *         the associated {@link Service}. Two different services may have identical numbers.
 *     </li>
 *     <li>
 *         <b>Stable identity</b>: Once assigned, the number <b>must not</b> change.
 *     </li>
 *     <li>
 *         <b>Human-facing</b>: This value is intended for display, URLs, and external references,
 *         not for internal database identity.
 *     </li>
 * </ul>
 *
 * @param id the unique entity identifer of the change request, can't be {@literal null}
 * @param service the service that is the owner of the change request, can't be {@literal null}
 * @param profile the target profile where the changes would be merged, can't be {@literal null}
 * @param number the change request number sequence that unique per service, can't be {@literal null}
 * @param state the state of the change request, can't be {@literal null}
 * @param mergeStatus the merge status of the change request, can't be {@literal null}
 * @param subject the subject of the change request, can't be {@literal null}
 * @param description additional explanation of the changes, can be {@literal null}
 * @param count the count of proposed changes for this change request, can't be {@literal null}
 * @param createdBy the name of the principal that created the change request, can't be {@literal null}
 * @param createdAt time when this change request was created
 * @param updatedAt time when this change request was last updated
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Entity
@NullMarked
public record ChangeRequest(
		@Identity EntityId id,
		@Association Service service,
		@Association Profile profile,
		long number,
		ChangeRequestState state,
		ChangeRequestMergeStatus mergeStatus,
		String subject,
		@Nullable String description,
		int count,
		String createdBy,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5873479330085482840L;

	/**
	 * Search criteria that can be used to filter change requests that are made
	 * against the specific {@link Profile}.
	 */
	public static final SearchQuery.Criteria<String> PROFILE_CRITERIA =
			SearchQuery.criteria("profile", String.class);

	/**
	 * Search criteria that can be used to filter change requests based on their
	 * {@link ChangeRequestState state}.
	 */
	public static final SearchQuery.Criteria<ChangeRequestState> STATE_CRITERIA =
			SearchQuery.criteria("state", ChangeRequestState.class);

}
