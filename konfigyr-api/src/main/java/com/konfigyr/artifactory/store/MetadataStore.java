package com.konfigyr.artifactory.store;

import com.konfigyr.artifactory.ArtifactCoordinates;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface MetadataStore {

	@NonNull
	Optional<Resource> get(@NonNull ArtifactCoordinates coordinates);

	@NonNull
	Resource save(@NonNull ArtifactCoordinates coordinates, @NonNull InputStreamSource content);

	void remove(@NonNull ArtifactCoordinates coordinates);

}
