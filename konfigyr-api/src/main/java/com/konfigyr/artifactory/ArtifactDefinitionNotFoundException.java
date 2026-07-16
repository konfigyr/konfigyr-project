package com.konfigyr.artifactory;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception that is thrown when an {@link ArtifactDefinition} is not present in the
 * {@code Artifactory} Domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactDefinitionNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 6236774637610101542L;

	/**
	 * Maven {@code groupId} coordinate of the artifact that could not be found.
	 */
	private final String groupId;

	/**
	 * Maven {@code artifactId} coordinate of the artifact that could not be found.
	 */
	private final String artifactId;

	/**
	 * Create a new instance when no artifact exists for the given {@code groupId} and
	 * {@code artifactId} coordinates.
	 *
	 * @param groupId the artifact {@code groupId} coordinate that could not be found, can't be {@literal null}
	 * @param artifactId the artifact {@code artifactId} coordinate that could not be found, can't be {@literal null}
	 */
	public ArtifactDefinitionNotFoundException(@NonNull String groupId, @NonNull String artifactId) {
		super(HttpStatus.NOT_FOUND, "Can not find artifact with following coordinates: %s:%s".formatted(groupId, artifactId));
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { groupId, artifactId };
	}

	/**
	 * Returns the {@code groupId} coordinate of the artifact that could not be found.
	 *
	 * @return the {@code groupId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Returns the {@code artifactId} coordinate of the artifact that could not be found.
	 *
	 * @return the {@code artifactId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getArtifactId() {
		return artifactId;
	}

}
