package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a verification challenge cannot be found for the requested verification.
 *
 * @author Vitalii Kushnir
 **/
public class VerificationChallengeNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 6141517088056362808L;

	public VerificationChallengeNotFoundException(Owner owner, String groupId) {
		super(HttpStatus.NOT_FOUND, "Could not find a verification for groupId '" + groupId + "' owned by namespace " + owner.slug());
	}

	public VerificationChallengeNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}

}
