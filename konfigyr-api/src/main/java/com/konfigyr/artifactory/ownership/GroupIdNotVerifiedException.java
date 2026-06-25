package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a namespace does not hold an active verification claim covering the
 * artifact {@code groupId} and is therefore not allowed to publish artifacts for that group.
 *
 * @author Mila Zarkovic
 */
public class GroupIdNotVerifiedException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 8812873434298745634L;

	private final String groupId;
	private final Owner owner;

	/**
	 * Create new instance when a namespace lacks an active claim covering the supplied groupId.
	 *
	 * @param groupId the artifact groupId that is not verified, can't be {@literal null}
	 * @param owner the namespace owner that attempted to publish, can't be {@literal null}
	 */
	public GroupIdNotVerifiedException(@NonNull String groupId, @NonNull Owner owner) {
		super(HttpStatus.BAD_REQUEST, "GroupId '%s' is not verified for publishing".formatted(groupId));
		this.groupId = groupId;
		this.owner = owner;
	}

	@NonNull
	public String getGroupId() {
		return groupId;
	}

	@NonNull
	public Owner getOwner() {
		return owner;
	}

}
