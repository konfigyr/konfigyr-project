package com.konfigyr.vault;

/**
 * Represents the review state of a {@link ChangeRequest}, the change request always progresses
 * forward and never re-enters a prior state.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum ChangeRequestState {

	/**
	 * The {@link ChangeRequest} has been opened, and it's awaiting action from reviewers.
	 * <p>
	 * The proposed changes should be first reviewed and approved by the reviewers before
	 * they are merged into the target {@link Profile}.
	 */
	OPEN,

	/**
	 * The proposed changes have been merged and applied to the target {@link Profile}.
	 */
	MERGED,

	/**
	 * The proposed changes have been rejected, and the change request is discarded. No configuration
	 * changes were applied, and the underlying changes would be archived.
	 */
	DISCARDED

}
