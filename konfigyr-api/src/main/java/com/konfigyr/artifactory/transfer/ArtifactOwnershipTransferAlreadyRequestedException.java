package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryException;
import com.konfigyr.artifactory.Owner;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link TransferState#PENDING} {@link ArtifactOwnershipTransfer} already exists
 * for the same {@code groupId}, {@code from} and {@code to} namespace combination.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactOwnershipTransferAlreadyRequestedException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 2861784550772490358L;

	/**
	 * Maven {@code groupId} coordinate for which a pending transfer request already exists.
	 */
	private final String groupId;

	/**
	 * The namespace that currently owns the artifacts targeted by the duplicate request.
	 */
	private final Owner from;

	/**
	 * The namespace that submitted the duplicate request.
	 */
	private final Owner to;

	/**
	 * Create a new instance when a {@link TransferState#PENDING} transfer already exists for the given
	 * {@code groupId}, {@code from} and {@code to} namespace combination.
	 *
	 * @param groupId the {@code groupId} coordinate for which a pending request already exists, can't be {@literal null}
	 * @param from the namespace that currently owns the artifacts targeted by the duplicate request, can't be {@literal null}
	 * @param to the namespace that submitted the duplicate request, can't be {@literal null}
	 */
	public ArtifactOwnershipTransferAlreadyRequestedException(@NonNull String groupId, @NonNull Owner from, @NonNull Owner to) {
		super(HttpStatus.BAD_REQUEST, "A pending artifact ownership transfer for groupId '%s' from '%s' to '%s' already exists"
				.formatted(groupId, from.slug(), to.slug()));
		this.groupId = groupId;
		this.from = from;
		this.to = to;
	}

	/**
	 * Returns the {@code groupId} coordinate for which a pending transfer request already exists.
	 *
	 * @return the {@code groupId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Returns the namespace that currently owns the artifacts targeted by the duplicate request.
	 *
	 * @return the current owner, never {@literal null}
	 */
	@NonNull
	public Owner getFrom() {
		return from;
	}

	/**
	 * Returns the namespace that submitted the duplicate request.
	 *
	 * @return the requesting namespace, never {@literal null}
	 */
	@NonNull
	public Owner getTo() {
		return to;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { groupId, from.slug(), to.slug() };
	}
}
