package com.konfigyr.vault;

/**
 * Represents the detailed, derived merge status of a {@link ChangeRequest}.
 * <p>
 * This status explains whether a change request can be merged and, if not, provides the
 * primary reason for blocking the merge.
 *
 * <p>
 * Bellow are the important characteristics of the merge status:
 * <ul>
 *     <li>
 *         <b>Derived, not stored</b>: This value must be computed from the current system state
 *         (Git repository, reviews, and change request metadata). It must not be treated as a
 *         source of truth.
 *     </li>
 *     <li>
 *         <b>Single dominant reason</b>: Although multiple conditions may prevent merging, this
 *         status represents the most relevant or highest-priority blocking reason.
 *     </li>
 *     <li>
 *         <b>Transient</b>: The value may change at any time as external conditions evolve
 *         (e.g., new commits on the target branch).
 *     </li>
 * </ul>
 *
 * <p>
 * Implementations should evaluate conditions in a deterministic order, returning the first
 * matching blocking condition before falling back to {@link #MERGEABLE}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum ChangeRequestMergeStatus {

	/**
	 * The Change Request is not open and therefore cannot be merged.
	 */
	NOT_OPEN,

	/**
	 * The change request is based on an outdated base commit.
	 * <p>
	 * This occurs when the target profile branch has advanced since the change request was created.
	 * <p>
	 * In our initial immutable model, this requires closing the change request and creating a new one.
	 * Providing amending commits to the existing change request is not yet supported.
	 */
	OUTDATED,

	/**
	 * The change request cannot be merged cleanly due to conflicts with the target branch.
	 * <p>
	 * This is determined using Git merge checks, and it is triggered by a change on the target profile
	 * branch. When in this state, the change request is considered blocked and cannot be merged.
	 */
	CONFLICTING,

	/**
	 * The change request has not received the required approvals.
	 * <p>
	 * This is derived from review events. At least one approval (and no blocking change requests) is
	 * typically required, depending on the policy.
	 */
	NOT_APPROVED,

	/**
	 * A reviewer has explicitly requested changes.
	 * <p>
	 * This status takes precedence over {@link #NOT_APPROVED} as it represents an explicit rejection
	 * rather than absence of approval.
	 */
	CHANGES_REQUESTED,

	/**
	 * The change request is currently being evaluated (e.g., Git checks in progress).
	 * <p>
	 * This is typically a transient state and may be used when mergeability has not yet been determined.
	 */
	CHECKING,

	/**
	 * The change request satisfies all conditions and can be merged.
	 */
	MERGEABLE

}
