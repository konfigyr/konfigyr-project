package com.konfigyr.artifactory.ownership;

import java.io.Serial;

public class GroupIdAlreadyClaimedException extends GroupVerificationException {

	@Serial
	private static final long serialVersionUID = 8131982906551778580L;

	public GroupIdAlreadyClaimedException(String groupId) {
		super("Could not claim. The groupId " + groupId + "already owned by another namespace.");
	}

	public GroupIdAlreadyClaimedException(String groupId, Throwable cause) {
		super("Could not claim. The groupId " + groupId + "already owned by another namespace.", cause);
	}

}
