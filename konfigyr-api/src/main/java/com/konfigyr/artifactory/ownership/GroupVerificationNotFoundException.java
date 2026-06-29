package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link GroupVerification} cannot be found for the requested namespace and groupId.
 *
 * @author Vitalii Kushnir
 * @since 1.0.0
 */
public class GroupVerificationNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 6141517088056362808L;

	public GroupVerificationNotFoundException(@NonNull Owner owner, String groupId) {
		super(HttpStatus.NOT_FOUND, "Could not find a verification for groupId '" + groupId + "' owned by namespace " + owner.slug());
	}

}
