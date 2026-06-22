package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link GroupVerification} does not exist.
 *
 * @author Vitalii Kushnir
 **/
public class GroupVerificationNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 6141517088056362808L;

	public GroupVerificationNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}

	public GroupVerificationNotFoundException(HttpStatus status, String message) {
		super(status, message);
	}

}
