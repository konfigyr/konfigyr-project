package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

public class GroupVerificationNotFoundException extends GroupVerificationException {

	@Serial
	private static final long serialVersionUID = 6141517088056362808L;

	public GroupVerificationNotFoundException(@NonNull Owner owner, @NonNull String groupId) {
		super("Could not find a verification for groupId '" + groupId + "' owned by namespace " + owner.slug());
	}

	public GroupVerificationNotFoundException(@NonNull EntityId id) {
		super("Could not find a verification with the following identifier: " + id.serialize());
	}

}
