package com.konfigyr.artifactory.store;

import com.konfigyr.artifactory.ArtifactCoordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RequiredArgsConstructor
public class FileSystemMetadataStore implements MetadataStore {

	private final Path root;

	@NonNull
	@Override
	public Optional<Resource> get(@NonNull ArtifactCoordinates coordinates) {
		final Path location = createArtifactLocation(coordinates);

		if (!Files.exists(location)) {
			return Optional.empty();
		}

		final Resource resource = new FileSystemResource(location);
		return Optional.of(resource);
	}

	@NonNull
	@Override
	public Resource save(@NonNull ArtifactCoordinates coordinates, @NonNull InputStreamSource content) {
		final Path location = createArtifactLocation(coordinates);

		try {
			Files.write(location, content.getInputStream().readAllBytes());
		} catch (IOException e) {
			throw new UncheckedIOException("Unexpected error occurred while storing metadata for: " + coordinates.format(), e);
		}

		return new FileSystemResource(location);
	}

	@Override
	public void remove(@NonNull ArtifactCoordinates coordinates) {
		try {
			Files.deleteIfExists(createArtifactLocation(coordinates));
		} catch (IOException e) {
			throw new UncheckedIOException("Unexpected error occurred while removing metadata for: " + coordinates.format(), e);
		}
	}

	private Path createArtifactLocation(@NonNull ArtifactCoordinates coordinates) {
		return root.resolve(coordinates.format() + ".json");
	}
}
