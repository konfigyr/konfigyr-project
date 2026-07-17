package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryException;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when an {@link ArtifactOwnershipTransfer} is accepted, rejected or cancelled while
 * it is no longer in the {@link TransferState#PENDING} state.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactOwnershipTransferAlreadyResolvedException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 6289104537019084415L;

	/**
	 * The identifier of the transfer that has already been resolved.
	 */
	private final EntityId id;

	/**
	 * The current state of the transfer that has already been resolved.
	 */
	private final TransferState state;

	/**
	 * Create a new instance when the given {@link ArtifactOwnershipTransfer} is no longer
	 * {@link TransferState#PENDING}.
	 *
	 * @param transfer the transfer that has already been resolved, can't be {@literal null}
	 */
	public ArtifactOwnershipTransferAlreadyResolvedException(@NonNull ArtifactOwnershipTransfer transfer) {
		super(HttpStatus.CONFLICT, "Artifact ownership transfer '%s' has already been resolved with state '%s'"
				.formatted(transfer.id(), transfer.state()));
		this.id = transfer.id();
		this.state = transfer.state();
	}

	/**
	 * Returns the identifier of the transfer that has already been resolved.
	 *
	 * @return the transfer identifier, never {@literal null}
	 */
	@NonNull
	public EntityId getId() {
		return id;
	}

	/**
	 * Returns the current state of the transfer that has already been resolved.
	 *
	 * @return the transfer state, never {@literal null}
	 */
	@NonNull
	public TransferState getState() {
		return state;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { id, state };
	}
}
