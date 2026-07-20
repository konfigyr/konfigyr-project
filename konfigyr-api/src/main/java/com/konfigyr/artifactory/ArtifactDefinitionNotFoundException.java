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
	 * The {@code groupId}/{@code artifactId} identity of the artifact that could not be found.
	 */
	private final ArtifactKey key;

	/**
	 * Create a new instance when no artifact exists for the given {@link ArtifactKey}.
	 *
	 * @param key the {@code groupId}/{@code artifactId} identity of the artifact that could not be found,
	 *        can't be {@literal null}
	 */
	public ArtifactDefinitionNotFoundException(@NonNull ArtifactKey key) {
		super(HttpStatus.NOT_FOUND, "Can not find artifact with following coordinates: %s".formatted(
				ArtifactKey.format(key.groupId(), key.artifactId())));
		this.key = key;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { ArtifactKey.format(key.groupId(), key.artifactId()) };
	}

	/**
	 * Returns the {@link ArtifactKey} of the artifact that could not be found.
	 *
	 * @return the {@link ArtifactKey}, never {@literal null}
	 */
	@NonNull
	public ArtifactKey getKey() {
		return key;
	}

	/**
	 * Returns the {@code groupId} coordinate of the artifact that could not be found.
	 *
	 * @return the {@code groupId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getGroupId() {
		return key.groupId();
	}

	/**
	 * Returns the {@code artifactId} coordinate of the artifact that could not be found.
	 *
	 * @return the {@code artifactId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getArtifactId() {
		return key.artifactId();
	}

}
