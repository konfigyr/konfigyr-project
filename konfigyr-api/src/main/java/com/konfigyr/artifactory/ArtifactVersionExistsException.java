package com.konfigyr.artifactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception that is thrown when an {@link VersionedArtifact artifact version} is already present in the
 * {@code Artifactory} Domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactVersionExistsException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 3815863468321393274L;

	private final ArtifactCoordinates coordinates;

	public ArtifactVersionExistsException(ArtifactCoordinates coordinates) {
		this("Artifact version already exists for following coordinates: " + coordinates.format(), coordinates);
	}

	public ArtifactVersionExistsException(String message, ArtifactCoordinates coordinates) {
		super(HttpStatus.BAD_REQUEST, message);
		this.coordinates = coordinates;
	}

	@Override
	public Object @Nullable [] getDetailMessageArguments() {
		return new Object[] { coordinates.format() };
	}

	@NonNull
	public ArtifactCoordinates getCoordinates() {
		return coordinates;
	}
}
