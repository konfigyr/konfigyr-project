package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.ArtifactCoordinates;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when artifact metadata is uploaded via {@link ServiceManifests#upload} for
 * {@link ArtifactCoordinates coordinates} that were never declared for the release through a prior
 * {@link ServiceManifests#open} call.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class UndeclaredArtifactException extends ServiceManifestException {

	@Serial
	private static final long serialVersionUID = -1092134450472819547L;

	private final ArtifactCoordinates coordinates;

	/**
	 * Create new instance of the {@link UndeclaredArtifactException} for the given {@link ArtifactCoordinates}.
	 *
	 * @param coordinates the coordinates that were not declared for the release, can't be {@literal null}
	 */
	public UndeclaredArtifactException(@NonNull ArtifactCoordinates coordinates) {
		super(HttpStatus.NOT_FOUND, "Artifact was not declared for this release: " + coordinates.format());
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
