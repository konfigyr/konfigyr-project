package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

public class GroupVerificationNotFoundException extends GroupVerificationException {

	@Serial
	private static final long serialVersionUID = 6141517088056362808L;

	public GroupVerificationNotFoundException(@NonNull Owner owner, @NonNull String groupId) {
		super(HttpStatus.NOT_FOUND, "Could not find a verification for groupId '" + groupId + "' owned by namespace " + owner.slug());
	}

}
