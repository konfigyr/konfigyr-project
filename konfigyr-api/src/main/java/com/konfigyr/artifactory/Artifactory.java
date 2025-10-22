package com.konfigyr.artifactory;

import org.jmolecules.event.annotation.DomainEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface Artifactory {

	@NonNull
	Optional<VersionedArtifact> get(@NonNull ArtifactCoordinates coordinates);

	@NonNull
	List<PropertyDefinition> properties(@NonNull ArtifactCoordinates coordinates);

	boolean exists(@NonNull ArtifactCoordinates coordinates);

	@DomainEventPublisher(publishes = "artifactory.artifact-version.release")
	void release(@NonNull ArtifactCoordinates coordinates, @NonNull Resource metadata);

}
