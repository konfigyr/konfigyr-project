package com.konfigyr.artifactory;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a namespace attempts to publish a new version for, or change the
 * {@link ArtifactVisibility} of, an artifact that is owned by a different namespace.
 * <p>
 * Ownership is a {@code groupId}/{@code artifactId} level concern, not a version level one, so
 * this exception carries the artifact key without a version.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactOwnershipMismatchException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -1523069864217405721L;

	/**
	 * The {@code groupId}/{@code artifactId} identity of the artifact owned by a different namespace.
	 */
	private final ArtifactKey key;

	/**
	 * The namespace that attempted to publish or change the visibility of the artifact.
	 */
	private final Owner owner;

	/**
	 * Create a new instance when the given {@link Owner} does not match the namespace that already owns
	 * the artifact identified by the supplied {@link ArtifactKey}.
	 * <p>
	 * Accepts any {@link ArtifactKey}, including an {@link ArtifactCoordinates} instance.
	 *
	 * @param key the {@code groupId}/{@code artifactId} identity of the artifact owned by a different
	 *        namespace, can't be {@literal null}
	 * @param owner the namespace that attempted to publish or change the visibility of the artifact,
	 *        can't be {@literal null}
	 */
	public ArtifactOwnershipMismatchException(@NonNull ArtifactKey key, @NonNull Owner owner) {
		// uses the static ArtifactKey.format(groupId, artifactId) rather than key.format(): if key is an
		// ArtifactCoordinates, its overridden format() renders the version too, which this message must not leak
		super(HttpStatus.CONFLICT, "Artifact '%s' is owned by a '%s' namespace"
				.formatted(ArtifactKey.format(key.groupId(), key.artifactId()), owner.slug()));
		this.key = key;
		this.owner = owner;
	}

	/**
	 * Returns the {@link ArtifactKey} of the artifact owned by a different namespace.
	 *
	 * @return the {@link ArtifactKey}, never {@literal null}
	 */
	@NonNull
	public ArtifactKey getKey() {
		return key;
	}

	/**
	 * Returns the {@code groupId} coordinate of the artifact owned by a different namespace.
	 *
	 * @return the {@code groupId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getGroupId() {
		return key.groupId();
	}

	/**
	 * Returns the {@code artifactId} coordinate of the artifact owned by a different namespace.
	 *
	 * @return the {@code artifactId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getArtifactId() {
		return key.artifactId();
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
		return new Object[] { ArtifactKey.format(key.groupId(), key.artifactId()), owner.slug() };
	}

}
