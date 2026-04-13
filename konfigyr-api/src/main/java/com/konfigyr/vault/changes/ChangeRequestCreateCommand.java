package com.konfigyr.vault.changes;

import com.konfigyr.namespace.Service;
import com.konfigyr.vault.ApplyResult;
import com.konfigyr.vault.Profile;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command representing the creation of a new change request.
 * <p>
 * {@link ChangeRequestCreateCommand} encapsulates all data required to register a newly created
 * change request after the underlying {@link com.konfigyr.vault.state.StateRepository} operation
 * has been successfully performed by the {@link com.konfigyr.vault.Vault}. It binds together the
 * domain context (owning service and target profile) with the technical outcome of preparing the
 * change in the repository.
 * <p>
 * The {@code service} identifies the owning service from which the configuration changes originate,
 * while the {@code profile} represents the target profile into which those changes are intended to
 * be merged.
 * <p>
 * The {@code result} contains the outcome of the {@link com.konfigyr.vault.state.StateRepository}
 * apply operation that produced the change request branch. It is expected to include information
 * about the resulting commit(s) and serves as the authoritative source for the initial state of
 * the change request (e.g., base and head revisions).
 * <p>
 * The {@code branch} specifies the name of the branch created in the state repository for this change
 * request. This branch becomes the working reference for all subsequent operations such as review,
 * merge, or discard.
 * <p>
 * This command assumes that all repository-level operations (branch creation, applying changes, etc.)
 * have already been completed successfully. The manager processing this command is responsible for
 * persisting the change request and emitting the corresponding creation event.
 *
 * @param service the service that owns the change request
 * @param profile the target profile the changes are intended for
 * @param result  the result of the Git apply operation that produced the branch
 * @param branch  the name of the created Git branch representing the change request
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record ChangeRequestCreateCommand(
		Service service,
		Profile profile,
		ApplyResult result,
		String branch
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 8217795855153839507L;

}
