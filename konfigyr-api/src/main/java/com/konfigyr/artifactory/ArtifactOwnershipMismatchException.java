package com.konfigyr.artifactory;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a namespace attempts to publish a new version for, or change the
 * {@link ArtifactVisibility} of, an artifact that is owned by a different namespace.
 * <p>
 * Ownership is a {@code groupId}/{@code artifactId} level concern, not a version level one, so
 * this exception carries the artifact coordinates without a version.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactOwnershipMismatchException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -1523069864217405721L;

	/**
	 * Maven {@code groupId} coordinate of the artifact owned by a different namespace.
	 */
	private final String groupId;

	/**
	 * Maven {@code artifactId} coordinate of the artifact owned by a different namespace.
	 */
	private final String artifactId;

	/**
	 * The namespace that attempted to publish or change the visibility of the artifact.
	 */
	private final Owner owner;

	/**
	 * Create a new instance when the given {@link Owner} does not match the namespace that
	 * already owns the artifact identified by the supplied {@code groupId} and {@code artifactId}.
	 *
	 * @param groupId the {@code groupId} coordinate of the artifact owned by a different namespace, can't be {@literal null}
	 * @param artifactId the {@code artifactId} coordinate of the artifact owned by a different namespace, can't be {@literal null}
	 * @param owner the namespace that attempted to publish or change the visibility of the artifact, can't be {@literal null}
	 */
	public ArtifactOwnershipMismatchException(@NonNull String groupId, @NonNull String artifactId, @NonNull Owner owner) {
		super(HttpStatus.CONFLICT, "Artifact '%s:%s' is owned by a '%s' namespace".formatted(groupId, artifactId, owner.slug()));
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.owner = owner;
	}

	/**
	 * Create a new instance when the given {@link Owner} does not match the namespace that
	 * already owns the artifact identified by the supplied {@link ArtifactCoordinates}.
	 *
	 * @param coordinates the coordinates of the artifact owned by a different namespace, can't be {@literal null}
	 * @param owner the namespace that attempted to publish or change the visibility of the artifact, can't be {@literal null}
	 */
	public ArtifactOwnershipMismatchException(@NonNull ArtifactCoordinates coordinates, @NonNull Owner owner) {
		this(coordinates.groupId(), coordinates.artifactId(), owner);
	}

	/**
	 * Returns the {@code groupId} coordinate of the artifact owned by a different namespace.
	 *
	 * @return the {@code groupId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Returns the {@code artifactId} coordinate of the artifact owned by a different namespace.
	 *
	 * @return the {@code artifactId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Returns the namespace that attempted to publish or change the visibility of the artifact.
	 *
	 * @return the namespace owner, never {@literal null}
	 */
	@NonNull
	public Owner getOwner() {
		return owner;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { groupId, artifactId, owner.slug() };
	}

}
