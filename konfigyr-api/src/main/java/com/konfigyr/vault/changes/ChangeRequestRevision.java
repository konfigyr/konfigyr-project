package com.konfigyr.vault.changes;

import com.konfigyr.vault.PropertyTransition;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Represents the revision-level view of a {@link com.konfigyr.vault.ChangeRequest} within the
 * repository.
 * <p>
 * {@code ChangeRequestRevision} encapsulates the {@link com.konfigyr.vault.state.StateRepository}
 * related state of a change request, including its base and head revisions, the associated branch,
 * and the property changes that constitute the proposed modification.
 * <p>
 * The {@code baseRevision} identifies the commit from which the change request was created. It
 * represents the state of the target profile at the time of submission and is used for validating
 * whether the change request is outdated during merge evaluation.
 * <p>
 * The {@code headRevision} represents the current commit of the change request branch, containing
 * the applied property changes. This revision is the source of truth for what will be merged into
 * the target {@link com.konfigyr.vault.Profile}.
 * <p>
 * The {@code branch} identifies the repository reference under which the change request is maintained.
 * It is used for all repository-level operations such as merge, discard, and inspection.
 * <p>
 * The {@code changes} describe the logical property transitions that were applied to produce the head
 * revision. While the repository stores the resulting state, this field provides a domain-level
 * representation of the intended modifications.
 *
 * @param baseRevision the revision from which the change request was created
 * @param headRevision the current revision representing the change request state
 * @param branch the repository branch associated with the change request
 * @param changes the property changes applied within the change request
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record ChangeRequestRevision(
		String baseRevision,
		String headRevision,
		String branch,
		List<PropertyTransition> changes
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5362500617858264375L;

}
