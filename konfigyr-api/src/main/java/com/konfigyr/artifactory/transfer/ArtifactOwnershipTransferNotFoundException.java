package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryException;
import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when an {@link ArtifactOwnershipTransfer} cannot be found for the requested
 * namespace and transfer identifier.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactOwnershipTransferNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -3854923014028443771L;

	public ArtifactOwnershipTransferNotFoundException(@NonNull Owner owner, @NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find an artifact ownership transfer '%s' visible to '%s' namespace"
				.formatted(id, owner.slug()));
	}

}
