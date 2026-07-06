package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.*;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;

@RequiredArgsConstructor
class DefaultServiceManifests implements ServiceManifests {

	private static final String VERSION = "latest";

	private final DSLContext context;
	private final Artifactory artifactory;

	@NonNull
	@Override
	@Transactional(label = "service-manifest.open")
	public ServiceRelease open(@NonNull Service service, @NonNull Collection<ServiceReleaseCandidate> artifacts) {
		final Long releaseId = upsertRelease(service);
		final Map<ArtifactCoordinates, ExistingArtifact> existing = loadExistingArtifacts(releaseId);

		final Set<ArtifactCoordinates> coordinates = new LinkedHashSet<>();
		for (ServiceReleaseCandidate candidate : artifacts) {
			coordinates.add(ArtifactCoordinates.of(candidate));
		}

		final Set<ArtifactCoordinates> indexed = artifactory.existing(coordinates);
		final List<ServiceReleaseEntry> entries = new ArrayList<>(artifacts.size());

		var insert = context.insertInto(SERVICE_ARTIFACTS,
				SERVICE_ARTIFACTS.RELEASE_ID, SERVICE_ARTIFACTS.GROUP_ID, SERVICE_ARTIFACTS.ARTIFACT_ID,
				SERVICE_ARTIFACTS.VERSION, SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM);

		for (ServiceReleaseCandidate candidate : artifacts) {
			final ArtifactCoordinates coordinate = ArtifactCoordinates.of(candidate);
			final ExistingArtifact current = existing.get(coordinate);

			final ArtifactUploadStatus status;
			final ArtifactSource source;
			final String checksum;

			if (current != null && current.checksum() != null && current.checksum().equals(candidate.checksum())) {
				status = ArtifactUploadStatus.SKIP;
				source = current.source();
				checksum = current.checksum();
			} else if (current != null && current.checksum() != null) {
				status = ArtifactUploadStatus.UPLOAD_REQUIRED;
				source = ArtifactSource.LOCAL;
				checksum = null;
			} else if (indexed.contains(coordinate)) {
				status = ArtifactUploadStatus.SKIP;
				source = ArtifactSource.ARTIFACTORY;
				checksum = null;
			} else {
				status = ArtifactUploadStatus.UPLOAD_REQUIRED;
				source = ArtifactSource.LOCAL;
				checksum = null;
			}

			entries.add(ServiceReleaseEntry.of(coordinate.groupId(), coordinate.artifactId(), coordinate.version().get(), status));
			insert = insert.values(releaseId, coordinate.groupId(), coordinate.artifactId(), coordinate.version().get(), source.name(), checksum);
		}

		if (!coordinates.isEmpty()) {
			insert.onConflict(SERVICE_ARTIFACTS.RELEASE_ID, SERVICE_ARTIFACTS.GROUP_ID, SERVICE_ARTIFACTS.ARTIFACT_ID, SERVICE_ARTIFACTS.VERSION)
					.doUpdate()
					.set(SERVICE_ARTIFACTS.SOURCE, DSL.excluded(SERVICE_ARTIFACTS.SOURCE))
					.set(SERVICE_ARTIFACTS.CHECKSUM, DSL.excluded(SERVICE_ARTIFACTS.CHECKSUM))
					.execute();
		}

		pruneStaleArtifacts(releaseId, coordinates);

		return ServiceRelease.builder()
				.id(EntityId.from(releaseId).serialize())
				.state(ReleaseState.PENDING)
				.artifacts(entries)
				.build();
	}

	@NonNull
	private Long upsertRelease(@NonNull Service service) {
		final Long releaseId = context.insertInto(SERVICE_RELEASES)
				.set(
						SettableRecord.of(context, SERVICE_RELEASES)
								.set(SERVICE_RELEASES.ID, EntityId.generate().map(EntityId::get))
								.set(SERVICE_RELEASES.SERVICE_ID, service.id().get())
								.set(SERVICE_RELEASES.VERSION, VERSION)
								.set(SERVICE_RELEASES.STATE, ReleaseState.PENDING.name())
								.set(SERVICE_RELEASES.CREATED_AT, OffsetDateTime.now())
								.get()
				)
				.onConflictOnConstraint(Keys.UNIQUE_NAMESPACE_SERVICE_VERSION)
				.doUpdate()
				.set(SERVICE_RELEASES.STATE, ReleaseState.PENDING.name())
				.set(SERVICE_RELEASES.CREATED_AT, OffsetDateTime.now())
				.returning(SERVICE_RELEASES.ID)
				.fetchOne(SERVICE_RELEASES.ID);

		Assert.state(releaseId != null, "Failed to resolve the release identifier for: " + service);

		return releaseId;
	}

	@NonNull
	private Map<ArtifactCoordinates, ExistingArtifact> loadExistingArtifacts(@NonNull Long releaseId) {
		return context.select(SERVICE_ARTIFACTS.GROUP_ID, SERVICE_ARTIFACTS.ARTIFACT_ID, SERVICE_ARTIFACTS.VERSION,
						SERVICE_ARTIFACTS.SOURCE, SERVICE_ARTIFACTS.CHECKSUM)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
				.fetchMap(
						record -> ArtifactCoordinates.of(
								record.get(SERVICE_ARTIFACTS.GROUP_ID), record.get(SERVICE_ARTIFACTS.ARTIFACT_ID), record.get(SERVICE_ARTIFACTS.VERSION)),
						record -> new ExistingArtifact(
								record.get(SERVICE_ARTIFACTS.CHECKSUM),
								ArtifactSource.valueOf(record.get(SERVICE_ARTIFACTS.SOURCE)))
				);
	}

	private void pruneStaleArtifacts(@NonNull Long releaseId, @NonNull Collection<ArtifactCoordinates> coordinates) {
		if (coordinates.isEmpty()) {
			context.deleteFrom(SERVICE_ARTIFACTS)
					.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
					.execute();
			return;
		}

		final String[] groupIds = coordinates.stream().map(ArtifactCoordinates::groupId).toArray(String[]::new);
		final String[] artifactIds = coordinates.stream().map(ArtifactCoordinates::artifactId).toArray(String[]::new);
		final String[] versions = coordinates.stream().map(coordinate -> coordinate.version().get()).toArray(String[]::new);

		context.deleteFrom(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
				.and(DSL.row(SERVICE_ARTIFACTS.GROUP_ID, SERVICE_ARTIFACTS.ARTIFACT_ID, SERVICE_ARTIFACTS.VERSION)
						.notIn(DSL.select(DSL.field("g", String.class), DSL.field("a", String.class), DSL.field("v", String.class))
								.from("unnest({0}::text[], {1}::text[], {2}::text[]) AS t(g, a, v)", groupIds, artifactIds, versions)))
				.execute();
	}

	@Override
	public void upload(@NonNull Service service, @NonNull EntityId releaseId, @NonNull ArtifactMetadata metadata) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@NonNull
	@Override
	public ServiceRelease complete(@NonNull Service service, @NonNull EntityId releaseId) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private record ExistingArtifact(String checksum, ArtifactSource source) {
	}

}
