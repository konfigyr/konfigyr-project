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

	private final String groupId;
	private final Owner from;
	private final Owner to;

	public ArtifactOwnershipTransferAlreadyRequestedException(@NonNull String groupId, @NonNull Owner from, @NonNull Owner to) {
		super(HttpStatus.BAD_REQUEST, "A pending artifact ownership transfer for groupId '%s' from '%s' to '%s' already exists"
				.formatted(groupId, from.slug(), to.slug()));
		this.groupId = groupId;
		this.from = from;
		this.to = to;
	}

	public String getGroupId() {
		return groupId;
	}

	public Owner getFrom() {
		return from;
	}

	public Owner getTo() {
		return to;
	}
}
