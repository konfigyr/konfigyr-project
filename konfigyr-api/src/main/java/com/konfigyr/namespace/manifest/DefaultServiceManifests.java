package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.*;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.*;

import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;

/**
 * Default {@link ServiceManifests} implementation.
 * <p>
 * A build plugin uses three calls, in order, to publish a service's Spring Boot configuration
 * metadata:
 * <ol>
 *     <li>{@link #open}: tell the server which Maven coordinates this build depends on, and get
 *     back which of them the plugin still needs to upload metadata for.</li>
 *     <li>{@link #upload}: for each coordinate the server asked for, upload the parsed
 *     {@code spring-configuration-metadata.json} contents.</li>
 *     <li>{@link #complete}: tell the server the build is done, so it can check every required
 *     upload happened and make the resulting property catalog available.</li>
 * </ol>
 * All three calls read and write the same two database tables: {@code service_releases} (one row
 * per service, tracking the state of its current build) and {@code service_artifacts} (one row per
 * declared coordinate within that build).
 * <p>
 * Each {@code service_artifacts} row has a {@code source} and a {@code checksum} column, and the
 * two are linked by a rule that the rest of this class depends on:
 * <ul>
 *     <li>{@code source = LOCAL} means the coordinate is the build's own artifact, or one of its
 *     dependencies, whose metadata this service itself uploads via {@link #upload}. Until that
 *     upload happens, {@code checksum} is {@literal null}. That is precisely how the code tells
 *     "declared but not yet uploaded" apart from "already uploaded". Once uploaded, {@code checksum}
 *     holds the SHA-256 of the uploaded bytes, so a later {@link #open} call can tell whether the
 *     plugin's local copy still matches what was last uploaded.</li>
 *     <li>{@code source = ARTIFACTORY} means the coordinate is a third-party dependency whose
 *     metadata already exists in the shared Artifactory registry (see {@link Artifactory}), so this
 *     service never uploads anything for it. Its {@code checksum} column is therefore always
 *     {@literal null}: that column only ever records what <em>this service</em> uploaded, and for an
 *     {@code ARTIFACTORY} row that never happens. The Artifactory does have its own checksum for
 *     the coordinate (see {@link Publication#checksum()}), but that value is only ever held in memory,
 *     inside {@link ExistingArtifact}, for the duration of a single {@link #open} call, it is never
 *     written to {@code service_artifacts.checksum}. See {@link CandidateArtifact#resolveChecksum}
 *     for where this is enforced.</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultServiceManifests implements ServiceManifests {

	private static final String VERSION = "latest";

	private final Marker RELEASE_OPENED = MarkerFactory.getMarker("SERVICE_RELEASE_OPENED");
	private final Marker ARTIFACT_UPLOADED = MarkerFactory.getMarker("SERVICE_ARTIFACT_UPLOADED");

	private final DSLContext context;
	private final Artifactory artifactory;
	private final ArtifactoryConverters converters;

	@Override
	@Transactional(label = "service-manifest.open")
	public ServiceRelease open(Service service, Collection<ServiceReleaseCandidate> artifacts) {
		log.debug("Attempting to open a service manifest for {} with: {}", service, artifacts);

		final Long releaseId = upsertRelease(service);

		// Convert the incoming candidates to their ArtifactCoordinates pairs...
		final List<CandidateArtifact> candidates = artifacts.stream().map(CandidateArtifact::new).toList();

		// Merge in whatever the Artifactory already has indexed for these coordinates, without
		// overriding a real, previously uploaded checksum already recorded for this release: a
		// LOCAL row with a non-null checksum is settled and must win over merely being indexed.
		final Map<ArtifactCoordinates, ExistingArtifact> existing = new LinkedHashMap<>();
		existing.putAll(loadExistingPublications(artifacts));
		existing.putAll(loadExistingServiceArtifacts(releaseId));

		final List<ServiceReleaseEntry> entries = new ArrayList<>(artifacts.size());

		// prepare the bulk insert query for the release///
		var insert = context.insertInto(SERVICE_ARTIFACTS).columns(
				SERVICE_ARTIFACTS.RELEASE_ID,
				SERVICE_ARTIFACTS.GROUP_ID,
				SERVICE_ARTIFACTS.ARTIFACT_ID,
				SERVICE_ARTIFACTS.VERSION,
				SERVICE_ARTIFACTS.SOURCE,
				SERVICE_ARTIFACTS.CHECKSUM
		);

		for (CandidateArtifact candidate : candidates) {
			final ExistingArtifact current = existing.get(candidate.coordinates());

			final ArtifactUploadStatus status;
			final ArtifactSource source;
			final String checksum;

			if (current != null) {
				status = candidate.resolveStatus(current);
				source = current.source();
				checksum = candidate.resolveChecksum(status, current);
			} else {
				status = ArtifactUploadStatus.UPLOAD_REQUIRED;
				source = ArtifactSource.LOCAL;
				checksum = null;
			}

			entries.add(ServiceReleaseEntry.of(candidate.artifact(), status));

			// append the insert row statement for the artifact...
			insert = insert.values(
					releaseId,
					candidate.coordinates().groupId(),
					candidate.coordinates().artifactId(),
					candidate.coordinates().version().get(),
					source.name(),
					checksum
			);
		}

		if (!candidates.isEmpty()) {
			insert.onConflictOnConstraint(Keys.UNIQUE_SERVICE_ARTIFACT)
					.doUpdate()
					.set(SERVICE_ARTIFACTS.SOURCE, DSL.excluded(SERVICE_ARTIFACTS.SOURCE))
					.set(SERVICE_ARTIFACTS.CHECKSUM, DSL.excluded(SERVICE_ARTIFACTS.CHECKSUM))
					.execute();
		}

		pruneStaleArtifacts(releaseId, candidates);

		log.info(RELEASE_OPENED, "Successfully opened release {} for service {}: {} artifact(s) declared",
				releaseId, service.id(), entries.size());

		return ServiceRelease.builder()
				.id(EntityId.from(releaseId).serialize())
				.state(ReleaseState.PENDING)
				.artifacts(entries)
				.build();
	}

	/**
	 * Upserts the single {@code service_releases} row for this service to
	 * {@link ReleaseState#PENDING}, taking over an in-progress or previously completed build rather
	 * than rejecting it, since there is only ever one release row per service.
	 * <p>
	 * The upsert is keyed on the {@code unique_namespace_service_version} constraint with a fixed
	 * {@link #VERSION}. Every build for a service therefore lands on the same row.
	 * <p>
	 * If proper version tracking (multiple retained releases, history, diffs) lands in a future,
	 * this fixed-version upsert is what needs to change first, into an actual insert of a new row per
	 * build. The release id returned from here is already addressable in every URL specifically so
	 * that change doesn't have to touch the request/response protocol.
	 *
	 * @param service the service opening or taking over a build, can't be {@literal null}.
	 * @return the entity identifier of the upserted release row, never {@literal null}.
	 */
	private Long upsertRelease(Service service) {
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

	/**
	 * Loads the {@code service_artifacts} rows already recorded for this release, keyed by their
	 * {@link ArtifactCoordinates}.
	 * <p>
	 * This is the release's current state of record, checked against each incoming artifact in
	 * {@link #open(Service, Collection)} to resolve its {@link ArtifactUploadStatus}: a matching
	 * checksum means the metadata behind this coordinate was already uploaded and can be skipped; a
	 * non-null but different checksum means it was uploaded before but is now stale; a coordinate
	 * missing here entirely has never been seen in this release.
	 *
	 * @param releaseId the release to load existing artifacts for, can't be {@literal null}.
	 * @return existing artifacts for the release keyed by their coordinates, never {@literal null}.
	 */
	private Map<ArtifactCoordinates, ExistingArtifact> loadExistingServiceArtifacts(Long releaseId) {
		return context.select(
						SERVICE_ARTIFACTS.GROUP_ID,
						SERVICE_ARTIFACTS.ARTIFACT_ID,
						SERVICE_ARTIFACTS.VERSION,
						SERVICE_ARTIFACTS.SOURCE,
						SERVICE_ARTIFACTS.CHECKSUM
				)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
				.fetchMap(
						record -> ArtifactCoordinates.of(
								record.get(SERVICE_ARTIFACTS.GROUP_ID),
								record.get(SERVICE_ARTIFACTS.ARTIFACT_ID),
								record.get(SERVICE_ARTIFACTS.VERSION)
						),
						record -> new ExistingArtifact(
								record.get(SERVICE_ARTIFACTS.CHECKSUM),
								record.get(SERVICE_ARTIFACTS.SOURCE)
						)
				);
	}

	/**
	 * Resolves the {@link Publication} already indexed by the {@link Artifactory} for each of the
	 * given candidates, keyed by {@link ArtifactCoordinates}.
	 * <p>
	 * Every match is reported as an {@link ArtifactSource#ARTIFACTORY} entry carrying the
	 * {@link Publication}'s own checksum: since {@code service_artifacts.checksum} is always
	 * {@literal null} for {@code ARTIFACTORY} rows, this is the only place that checksum is actually
	 * available. Candidates with no matching {@link Publication} are simply absent from the result,
	 * since they haven't been indexed yet.
	 *
	 * @param candidates the release candidates to resolve against the Artifactory, can't be {@literal null}.
	 * @return existing publications keyed by their coordinates, never {@literal null}.
	 */
	private Map<ArtifactCoordinates, ExistingArtifact> loadExistingPublications(Collection<ServiceReleaseCandidate> candidates) {
		final Map<ArtifactCoordinates, ExistingArtifact> existing = new LinkedHashMap<>();

		if (candidates.isEmpty()) {
			return existing;
		}

		final Set<ArtifactCoordinates> coordinates = new LinkedHashSet<>(candidates.size());
		for (ServiceReleaseCandidate candidate : candidates) {
			coordinates.add(ArtifactCoordinates.of(candidate));
		}

		for (Publication publication : artifactory.existing(coordinates)) {
			existing.put(ArtifactCoordinates.of(publication), new ExistingArtifact(publication));
		}

		return existing;
	}

	/**
	 * Deletes every {@code service_artifacts} row for this release whose coordinates are not part of
	 * the given candidates.
	 * <p>
	 * This is how a declared artifact drops out of a release: a build that stops depending on a
	 * coordinate it previously declared is expected to have that coordinate's row removed the next
	 * time it resolves a release, rather than leaving it behind indefinitely.
	 *
	 * @param releaseId the release to prune, can't be {@literal null}.
	 * @param candidates the complete, current set of declared candidates, can't be {@literal null}; an
	 *                    empty collection removes every row for this release.
	 */
	private void pruneStaleArtifacts(Long releaseId, Collection<CandidateArtifact> candidates) {
		if (candidates.isEmpty()) {
			context.deleteFrom(SERVICE_ARTIFACTS)
					.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
					.execute();
			return;
		}

		final String[] groupIds = candidates.stream().map(CandidateArtifact::artifact).map(Artifact::groupId).toArray(String[]::new);
		final String[] artifactIds = candidates.stream().map(CandidateArtifact::artifact).map(Artifact::artifactId).toArray(String[]::new);
		final String[] versions = candidates.stream().map(CandidateArtifact::artifact).map(Artifact::version).toArray(String[]::new);

		context.deleteFrom(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
				.and(DSL.row(SERVICE_ARTIFACTS.GROUP_ID, SERVICE_ARTIFACTS.ARTIFACT_ID, SERVICE_ARTIFACTS.VERSION)
						.notIn(DSL.select(DSL.field("g", String.class), DSL.field("a", String.class), DSL.field("v", String.class))
								.from("unnest({0}::text[], {1}::text[], {2}::text[]) AS t(g, a, v)", groupIds, artifactIds, versions)))
				.execute();
	}

	/**
	 * Uploads the configuration property metadata for a single artifact that was previously declared
	 * for this release via {@link #open}.
	 * <p>
	 * The given {@code metadata}'s coordinates must match a {@code LOCAL} {@code service_artifacts} row
	 * already recorded for this release; anything else — a coordinate never declared, or one declared
	 * as {@code ARTIFACTORY} rather than {@code LOCAL} — is rejected with an
	 * {@link UndeclaredArtifactException}. The release must also still be {@link ReleaseState#PENDING},
	 * otherwise a {@link ReleaseNotPendingException} is thrown: once a release is {@code RELEASED}, the
	 * plugin must call {@link #open} again to start a new build before uploading anything.
	 * <p>
	 * {@code metadata.properties()} is exploded into {@code service_configuration_catalog} rows for
	 * this coordinate, replacing whatever rows already exist there for it, so a re-upload of the same
	 * coordinate overwrites rather than duplicates. The matching {@code service_artifacts} row's
	 * {@code checksum} is then updated to {@code metadata.checksum()} — this is what turns it from
	 * "declared" into "uploaded", see this class's own Javadoc for why that column doubles as that
	 * signal.
	 *
	 * @param service the service the release belongs to, can't be {@literal null}.
	 * @param releaseId the entity identifier of the release the upload belongs to, can't be {@literal null}.
	 * @param metadata the uploaded artifact metadata, can't be {@literal null}.
	 * @throws UndeclaredArtifactException if the metadata's coordinates were not declared for this
	 *         release as a {@code LOCAL} artifact.
	 * @throws ReleaseNotPendingException if the release is not currently {@link ReleaseState#PENDING}.
	 */
	@Override
	@Transactional(label = "service-manifest.upload")
	public void upload(Service service, EntityId releaseId, ArtifactMetadata metadata) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(metadata);

		log.debug("Attempting to upload artifact metadata for {} to release {} for service {}",
				coordinates, releaseId, service);

		final ReleaseState state = context.select(SERVICE_RELEASES.STATE)
				.from(SERVICE_ARTIFACTS)
				.join(SERVICE_RELEASES).on(SERVICE_RELEASES.ID.eq(SERVICE_ARTIFACTS.RELEASE_ID))
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()))
				.and(SERVICE_RELEASES.SERVICE_ID.eq(service.id().get()))
				.and(SERVICE_ARTIFACTS.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(coordinates.version().get()))
				.and(SERVICE_ARTIFACTS.SOURCE.eq(ArtifactSource.LOCAL.name()))
				.fetchOptional(SERVICE_RELEASES.STATE)
				.map(ReleaseState::valueOf)
				.orElseThrow(() -> new UndeclaredArtifactException(coordinates));

		if (state != ReleaseState.PENDING) {
			throw new ReleaseNotPendingException(releaseId, state);
		}

		context.deleteFrom(SERVICE_CONFIGURATION_CATALOG)
				.where(SERVICE_CONFIGURATION_CATALOG.RELEASE_ID.eq(releaseId.get()))
				.and(SERVICE_CONFIGURATION_CATALOG.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_CONFIGURATION_CATALOG.VERSION.eq(coordinates.version().get()))
				.execute();

		var insert = context.insertInto(SERVICE_CONFIGURATION_CATALOG).columns(
				SERVICE_CONFIGURATION_CATALOG.SERVICE_ID,
				SERVICE_CONFIGURATION_CATALOG.RELEASE_ID,
				SERVICE_CONFIGURATION_CATALOG.GROUP_ID,
				SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID,
				SERVICE_CONFIGURATION_CATALOG.VERSION,
				SERVICE_CONFIGURATION_CATALOG.NAME,
				SERVICE_CONFIGURATION_CATALOG.TYPE_NAME,
				SERVICE_CONFIGURATION_CATALOG.SCHEMA,
				SERVICE_CONFIGURATION_CATALOG.DEFAULT_VALUE,
				SERVICE_CONFIGURATION_CATALOG.DESCRIPTION,
				SERVICE_CONFIGURATION_CATALOG.DEPRECATION
		);

		for (PropertyDescriptor property : metadata.properties()) {
			insert = insert.values(
					service.id().get(),
					releaseId.get(),
					coordinates.groupId(),
					coordinates.artifactId(),
					coordinates.version().get(),
					property.name(),
					property.typeName(),
					converters.schema().to(property.schema()),
					property.defaultValue(),
					property.description(),
					converters.deprecation().to(property.deprecation())
			);
		}

		if (!metadata.properties().isEmpty()) {
			insert.execute();
		}

		context.update(SERVICE_ARTIFACTS)
				.set(SERVICE_ARTIFACTS.CHECKSUM, metadata.checksum())
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()))
				.and(SERVICE_ARTIFACTS.GROUP_ID.eq(coordinates.groupId()))
				.and(SERVICE_ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()))
				.and(SERVICE_ARTIFACTS.VERSION.eq(coordinates.version().get()))
				.execute();

		log.info(ARTIFACT_UPLOADED, "Successfully uploaded artifact metadata for {} to release {}", coordinates, releaseId);
	}

	/**
	 * Not yet implemented. Notes for whoever picks this up:
	 * <ul>
	 *     <li>First check whether any {@code service_artifacts} row for this release still has
	 *     {@code source = LOCAL AND checksum IS NULL} — meaning something declared via {@link #open}
	 *     was never actually uploaded via {@link #upload}. If so, this release cannot complete: set
	 *     {@code service_releases.state} to {@link ReleaseState#FAILED} and return a
	 *     {@link ServiceRelease} whose {@link ServiceRelease#errors()} lists those coordinates.</li>
	 *     <li>Otherwise, build the property catalog for this release in
	 *     {@code service_configuration_catalog}. The {@code LOCAL} coordinates were already exploded
	 *     into the catalog by {@link #upload}; what is still missing here is the {@code ARTIFACTORY}
	 *     coordinates, whose property descriptors live in the Artifactory's own tables instead of
	 *     having been uploaded directly. {@code ServiceCatalogWorker} (in the sibling
	 *     {@code com.konfigyr.namespace.catalog} package) already contains the exact join needed —
	 *     from {@code service_artifacts} through {@code artifacts}, {@code artifact_versions},
	 *     {@code artifact_version_properties}, to {@code property_definitions} — but it is
	 *     package-private today, so either expose it for reuse here or re-create the same join.</li>
	 *     <li>Set {@code service_releases.state} to {@link ReleaseState#RELEASED} and
	 *     {@code published_at} to the current time.</li>
	 *     <li>Publish a {@code ServiceEvent.Released} event so that
	 *     {@code ServiceCatalogQueueListener} picks up this release for indexing. Building that event
	 *     needs the {@link Service} (already a parameter here) and a {@code Manifest}
	 *     ({@code Services.manifest(Service)}), which means this class needs a new dependency on
	 *     {@code Services} that it does not have today.</li>
	 *     <li>Log the outcome, matching the {@code log.info(...)} call at the top of {@link #open}.</li>
	 * </ul>
	 *
	 * @param service the service the release belongs to, can't be {@literal null}.
	 * @param releaseId the entity identifier of the release to complete, can't be {@literal null}.
	 * @return the completed service release, never {@literal null}.
	 */
	@Override
	public ServiceRelease complete(Service service, EntityId releaseId) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private record CandidateArtifact(ArtifactCoordinates coordinates, ServiceReleaseCandidate artifact) {

		private CandidateArtifact(ServiceReleaseCandidate candidate) {
			this(ArtifactCoordinates.of(candidate),  candidate);
		}

		/**
		 * Resolves the {@link ArtifactUploadStatus} of this candidate against what is already known
		 * about its coordinates, either a previously recorded {@code service_artifacts} row or a
		 * {@link Publication} indexed by the {@link Artifactory}.
		 * <p>
		 * An {@link ArtifactSource#ARTIFACTORY} match is always {@link ArtifactUploadStatus#SKIP},
		 * regardless of checksum: the plugin never uploads a dependency it doesn't own, so there is
		 * nothing to compare against. Otherwise, the outcome depends on whether this candidate's own
		 * checksum matches what was already recorded.
		 *
		 * @param existing what is already known about this candidate's coordinates, can't be {@literal null}.
		 * @return the resolved upload status, never {@literal null}.
		 */
		private ArtifactUploadStatus resolveStatus(ExistingArtifact existing) {
			if (existing.source() == ArtifactSource.ARTIFACTORY) {
				return ArtifactUploadStatus.SKIP;
			}
			return Objects.equals(artifact().checksum(), existing.checksum()) ?
					ArtifactUploadStatus.SKIP : ArtifactUploadStatus.UPLOAD_REQUIRED;
		}

		/**
		 * Resolves the checksum that should be persisted to {@code service_artifacts.checksum} for
		 * this candidate, given its resolved {@code status}.
		 * <p>
		 * {@literal null} is the "not yet uploaded" signal for a {@link ArtifactSource#LOCAL} row, so
		 * it must only be replaced with a real value once a matching, already-uploaded checksum is
		 * confirmed, i.e. {@code status} is {@link ArtifactUploadStatus#SKIP} and {@code existing} is
		 * {@link ArtifactSource#LOCAL}. Every other outcome persists {@literal null}: an
		 * {@link ArtifactSource#ARTIFACTORY} row has no local upload to checksum in the first place,
		 * and anything that isn't {@code SKIP} still needs to be uploaded, so carrying over its old or
		 * unrelated checksum would wrongly read back as already settled.
		 *
		 * @param status the resolved upload status for this candidate, can't be {@literal null}.
		 * @param existing what is already known about this candidate's coordinates, can't be {@literal null}.
		 * @return the checksum to persist, or {@literal null} when nothing settled should be recorded.
		 */
		private String resolveChecksum(ArtifactUploadStatus status, ExistingArtifact existing) {
			return status == ArtifactUploadStatus.SKIP && existing.source() == ArtifactSource.LOCAL
					? existing.checksum()
					: null;
		}
	}

	/**
	 * A previously recorded {@code service_artifacts} row for a single coordinate within a release.
	 */
	private record ExistingArtifact(String checksum, ArtifactSource source) {

		private ExistingArtifact(Publication publication) {
			this(publication.checksum(), ArtifactSource.ARTIFACTORY);
		}

		private ExistingArtifact(String checksum, String source) {
			this(checksum, ArtifactSource.valueOf(source));
		}

	}

}
