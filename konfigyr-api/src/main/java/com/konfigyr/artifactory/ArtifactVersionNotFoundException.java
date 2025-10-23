package com.konfigyr.artifactory;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Exception that is thrown when an {@link VersionedArtifact artifact version} is not present in the
 * {@code Artifactory} Domain.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactVersionNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -496851064572095308L;

	private final ArtifactCoordinates coordinates;

	public ArtifactVersionNotFoundException(ArtifactCoordinates coordinates) {
		this("Can not find artifact version with following coordinates: " + coordinates.format(), coordinates);
	}

	public ArtifactVersionNotFoundException(String message, ArtifactCoordinates coordinates) {
		super(HttpStatus.NOT_FOUND, message);
		this.coordinates = coordinates;
	}

	@NonNull
	public ArtifactCoordinates getCoordinates() {
		return coordinates;
	}
}
