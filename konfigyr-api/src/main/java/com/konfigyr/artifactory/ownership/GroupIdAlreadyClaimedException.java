package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a group identifier is already claimed by another namespace.
 *
 * @author Vitalii Kushnir
 * @since 1.0.0
 **/
public class GroupIdAlreadyClaimedException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 8131982906551778580L;

	public GroupIdAlreadyClaimedException(String groupId) {
		super(HttpStatus.BAD_REQUEST, "Could not claim. The groupId " + groupId + " already owned by another namespace.");
	}

}
