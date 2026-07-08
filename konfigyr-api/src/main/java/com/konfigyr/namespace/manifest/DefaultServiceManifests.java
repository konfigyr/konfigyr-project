package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.*;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
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
	private static final Name SERVICE_ARTIFACTS_ALIAS = DSL.name("artifacts");

	private final Marker RELEASE_OPENED = MarkerFactory.getMarker("SERVICE_RELEASE_OPENED");
	private final Marker ARTIFACT_UPLOADED = MarkerFactory.getMarker("SERVICE_ARTIFACT_UPLOADED");
	private final Marker RELEASE_COMPLETED = MarkerFactory.getMarker("SERVICE_RELEASE_COMPLETED");

	private final DSLContext context;
	private final Artifactory artifactory;
	private final ArtifactoryConverters converters;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional(readOnly = true, label = "service-manifest.get")
	public Manifest get(Service service) {
		return context.select(SERVICE_RELEASES.ID, SERVICE_RELEASES.CREATED_AT, createServiceArtifactMultiselectField())
				.from(SERVICE_RELEASES)
				.where(SERVICE_RELEASES.SERVICE_ID.eq(service.id().get()))
				.fetchOptional(record -> toManifest(service, record))
				.orElseGet(() -> Manifest.builder()
						.id(service.id().serialize())
						.name(service.name())
						.build()
				);
	}

	@Override
	@Transactional(label = "service-manifest.open")
	public ServiceRelease open(Service service, Collection<ServiceReleaseCandidate> artifacts) {
		log.debug("Attempting to open a service manifest for {} with: {}", service, artifacts);

		final EntityId releaseId = upsertRelease(service);

		// Convert the incoming candidates to their ArtifactCoordinates pairs...
		final List<CandidateArtifact> candidates = artifacts.stream().map(CandidateArtifact::new).toList();

		// Merge in whatever the Artifactory already has indexed for these coordinates, without
		// overriding a real, previously uploaded checksum already recorded for this release: a
		// LOCAL row with a non-null checksum is settled and must win over merely being indexed.
		final Map<ArtifactCoordinates, ExistingArtifact> existing = new LinkedHashMap<>();
		existing.putAll(loadExistingPublications(artifacts));
		existing.putAll(loadExistingServiceArtifacts(releaseId));

		final OffsetDateTime timestamp = OffsetDateTime.now();
		final List<ServiceReleaseEntry> entries = new ArrayList<>(artifacts.size());

		// prepare the bulk insert query for the release///
		var insert = context.insertInto(SERVICE_ARTIFACTS).columns(
				SERVICE_ARTIFACTS.RELEASE_ID,
				SERVICE_ARTIFACTS.GROUP_ID,
				SERVICE_ARTIFACTS.ARTIFACT_ID,
				SERVICE_ARTIFACTS.VERSION,
				SERVICE_ARTIFACTS.SOURCE,
				SERVICE_ARTIFACTS.CHECKSUM,
				SERVICE_ARTIFACTS.CREATED_AT
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
					releaseId.get(),
					candidate.coordinates().groupId(),
					candidate.coordinates().artifactId(),
					candidate.coordinates().version().get(),
					source.name(),
					checksum,
					timestamp
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
				.id(releaseId.serialize())
				.state(ReleaseState.PENDING)
				.artifacts(entries)
				.build();
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
	 * signal. The row's {@code name}/{@code description}/{@code website}/{@code repository} columns are
	 * updated from {@code metadata} at the same time: this is the only place that descriptive metadata
	 * for a {@code LOCAL} coordinate is ever recorded, since it is never indexed in the Artifactory's
	 * own tables the way an {@code ARTIFACTORY} coordinate is.
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
		final String formatted = coordinates.format();

		log.debug("Attempting to upload artifact metadata for {} to release {} for service {}",
				coordinates, releaseId, service);

		final ReleaseState state = context.select(SERVICE_RELEASES.STATE)
				.from(SERVICE_ARTIFACTS)
				.join(SERVICE_RELEASES).on(SERVICE_RELEASES.ID.eq(SERVICE_ARTIFACTS.RELEASE_ID))
				.where(DSL.and(
						SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()),
						SERVICE_RELEASES.SERVICE_ID.eq(service.id().get()),
						SERVICE_ARTIFACTS.COORDINATES.eq(formatted),
						SERVICE_ARTIFACTS.SOURCE.eq(ArtifactSource.LOCAL.name())
				))
				.fetchOptional(SERVICE_RELEASES.STATE)
				.map(ReleaseState::valueOf)
				.orElseThrow(() -> new UndeclaredArtifactException(coordinates));

		if (state != ReleaseState.PENDING) {
			throw new ReleaseNotPendingException(releaseId, state);
		}

		context.deleteFrom(SERVICE_CONFIGURATION_CATALOG)
				.where(DSL.and(
						SERVICE_CONFIGURATION_CATALOG.RELEASE_ID.eq(releaseId.get()),
						SERVICE_CONFIGURATION_CATALOG.COORDINATES.eq(formatted)
				))
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
				.set(SERVICE_ARTIFACTS.NAME, metadata.name())
				.set(SERVICE_ARTIFACTS.DESCRIPTION, metadata.description())
				.set(SERVICE_ARTIFACTS.WEBSITE, Objects.toString(metadata.website(), null))
				.set(SERVICE_ARTIFACTS.REPOSITORY, Objects.toString(metadata.repository(), null))
				.where(DSL.and(
						SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()),
						SERVICE_ARTIFACTS.COORDINATES.eq(formatted)
				))
				.execute();

		log.info(ARTIFACT_UPLOADED, "Successfully uploaded artifact metadata for {} to release {}", coordinates, releaseId);
	}

	/**
	 * Finalizes a {@link ReleaseState#PENDING} release: checks that every declared {@code LOCAL}
	 * artifact was uploaded, then transitions the release to {@link ReleaseState#RELEASED}.
	 * <p>
	 * If any {@code LOCAL} {@code service_artifacts} row for this release still has a {@literal null}
	 * checksum — declared via {@link #open} but never uploaded via {@link #upload} — the release
	 * cannot complete: it transitions to {@link ReleaseState#FAILED} instead, the returned
	 * {@link ServiceRelease#errors()} lists those coordinates, and a {@code ServiceEvent.ReleaseFailed}
	 * event is published so anything left behind by the attempt can be cleaned up. This is reported as
	 * a normal, non-error result rather than an exception, since it is the release itself that failed,
	 * not the call.
	 * <p>
	 * Otherwise, the release transitions to {@link ReleaseState#RELEASED} and a
	 * {@code ServiceEvent.Released} event is published. This class does not populate
	 * {@code service_configuration_catalog} for the release's {@code ARTIFACTORY} coordinates itself:
	 * {@code ServiceCatalogQueueListener} reacts to that event and schedules
	 * {@code ServiceCatalogWorker} to do so, the same mechanism already used when Artifactory metadata
	 * changes. {@code LOCAL} coordinates were already exploded into the catalog by {@link #upload}.
	 *
	 * @param service the service the release belongs to, can't be {@literal null}.
	 * @param releaseId the entity identifier of the release to complete, can't be {@literal null}.
	 * @return the completed service release, never {@literal null}.
	 * @throws ReleaseNotFoundException if no release with this identifier exists for this service.
	 * @throws ReleaseNotPendingException if the release is not currently {@link ReleaseState#PENDING}.
	 */
	@Override
	@Transactional(label = "service-manifest.complete")
	public ServiceRelease complete(Service service, EntityId releaseId) {
		log.debug("Attempting to complete release {} for service {}", releaseId, service);

		final ReleaseState state = context.select(SERVICE_RELEASES.STATE)
				.from(SERVICE_RELEASES)
				.where(DSL.and(
						SERVICE_RELEASES.ID.eq(releaseId.get()),
						SERVICE_RELEASES.SERVICE_ID.eq(service.id().get())
				))
				.fetchOptional(SERVICE_RELEASES.STATE)
				.map(ReleaseState::valueOf)
				.orElseThrow(() -> new ReleaseNotFoundException(releaseId));

		if (state != ReleaseState.PENDING) {
			throw new ReleaseNotPendingException(releaseId, state);
		}

		final List<ArtifactCoordinates> missing = findMissingArtifactCoordinates(releaseId);

		if (!missing.isEmpty()) {
			final List<String> errors = missing.stream()
					.map(ArtifactCoordinates::format)
					.map("Artifact with coordinates '%s' was not uploaded"::formatted)
					.toList();

			context.update(SERVICE_RELEASES)
					.set(SERVICE_RELEASES.STATE, ReleaseState.FAILED.name())
					.where(SERVICE_RELEASES.ID.eq(releaseId.get()))
					.execute();

			publisher.publishEvent(new ServiceEvent.ReleaseFailed(service, errors));

			log.info(RELEASE_COMPLETED, "Failed to complete release {} for service {}: {} artifact(s) never uploaded",
					releaseId, service.id(), missing.size());

			return ServiceRelease.builder()
					.id(releaseId.serialize())
					.state(ReleaseState.FAILED)
					.errors(errors)
					.build();
		}

		final OffsetDateTime publishedAt = OffsetDateTime.now();

		context.update(SERVICE_RELEASES)
				.set(SERVICE_RELEASES.STATE, ReleaseState.RELEASED.name())
				.set(SERVICE_RELEASES.PUBLISHED_AT, publishedAt)
				.where(SERVICE_RELEASES.ID.eq(releaseId.get()))
				.execute();

		publisher.publishEvent(new ServiceEvent.Released(service, get(service)));

		log.info(RELEASE_COMPLETED, "Successfully completed release {} for service {}", releaseId, service.id());

		return ServiceRelease.builder()
				.id(releaseId.serialize())
				.state(ReleaseState.RELEASED)
				.publishedAt(publishedAt.toInstant())
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
	private EntityId upsertRelease(Service service) {
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

		return EntityId.from(releaseId);
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
	private Map<ArtifactCoordinates, ExistingArtifact> loadExistingServiceArtifacts(EntityId releaseId) {
		return context.select(
						SERVICE_ARTIFACTS.GROUP_ID,
						SERVICE_ARTIFACTS.ARTIFACT_ID,
						SERVICE_ARTIFACTS.VERSION,
						SERVICE_ARTIFACTS.SOURCE,
						SERVICE_ARTIFACTS.CHECKSUM
				)
				.from(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()))
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
	private void pruneStaleArtifacts(EntityId releaseId, Collection<CandidateArtifact> candidates) {
		if (candidates.isEmpty()) {
			context.deleteFrom(SERVICE_ARTIFACTS)
					.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()))
					.execute();
			return;
		}

		final String[] coordinates = candidates.stream()
				.map(CandidateArtifact::coordinates)
				.map(ArtifactCoordinates::format)
				.toArray(String[]::new);

		context.deleteFrom(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()))
				.and(SERVICE_ARTIFACTS.COORDINATES.notIn(DSL.select(DSL.field("c", String.class))
						.from("unnest({0}::text[]) AS t(c)", (Object) coordinates)))
				.execute();
	}

	/**
	 * Finds the coordinates of every {@code LOCAL} artifact declared for this release that was never
	 * uploaded, i.e. whose {@code service_artifacts} row still has a {@literal null} checksum.
	 * <p>
	 * {@link #complete} calls this to decide whether a release can be finalized: a non-empty result
	 * means the release must transition to {@link ReleaseState#FAILED} instead of
	 * {@link ReleaseState#RELEASED}, with these coordinates reported back as the reason.
	 * {@code ARTIFACTORY} coordinates are never included, since their checksum is always
	 * {@literal null} by design and does not indicate a missing upload.
	 *
	 * @param releaseId the release to check for missing uploads, can't be {@literal null}.
	 * @return the coordinates of every declared but not yet uploaded artifact, never {@literal null};
	 *         empty when every declared {@code LOCAL} artifact has been uploaded.
	 */
	private List<ArtifactCoordinates> findMissingArtifactCoordinates(EntityId releaseId) {
		return context.select(SERVICE_ARTIFACTS.COORDINATES)
				.from(SERVICE_ARTIFACTS)
				.where(DSL.and(
						SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId.get()),
						SERVICE_ARTIFACTS.SOURCE.eq(ArtifactSource.LOCAL.name()),
						SERVICE_ARTIFACTS.CHECKSUM.isNull()
				))
				.fetch(record -> ArtifactCoordinates.parse(
						record.get(SERVICE_ARTIFACTS.COORDINATES)
				));
	}

	private Field<List<Artifact>> createServiceArtifactMultiselectField() {
		return DSL.multiset(
				DSL.select(
						SERVICE_ARTIFACTS.GROUP_ID,
						SERVICE_ARTIFACTS.ARTIFACT_ID,
						SERVICE_ARTIFACTS.VERSION,
						SERVICE_ARTIFACTS.SOURCE,
						SERVICE_ARTIFACTS.CHECKSUM,
						SERVICE_ARTIFACTS.CREATED_AT,
						SERVICE_ARTIFACTS.NAME,
						SERVICE_ARTIFACTS.DESCRIPTION,
						SERVICE_ARTIFACTS.WEBSITE,
						SERVICE_ARTIFACTS.REPOSITORY,
						ARTIFACTS.NAME,
						ARTIFACTS.DESCRIPTION,
						ARTIFACTS.WEBSITE,
						ARTIFACTS.REPOSITORY,
						ARTIFACT_VERSIONS.CHECKSUM
				)
				.from(SERVICE_ARTIFACTS)
				.leftJoin(ARTIFACTS)
				.on(DSL.and(
						ARTIFACTS.GROUP_ID.eq(SERVICE_ARTIFACTS.GROUP_ID),
						ARTIFACTS.ARTIFACT_ID.eq(SERVICE_ARTIFACTS.ARTIFACT_ID)
				))
				.leftJoin(ARTIFACT_VERSIONS)
				.on(DSL.and(
						ARTIFACT_VERSIONS.ARTIFACT_ID.eq(ARTIFACTS.ID),
						ARTIFACT_VERSIONS.VERSION.eq(SERVICE_ARTIFACTS.VERSION)
				))
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(SERVICE_RELEASES.ID))
		).as(SERVICE_ARTIFACTS_ALIAS).convertFrom(results -> results.map(DefaultServiceManifests::toManifestEntry));
	}

	@SuppressWarnings("unchecked")
	private static Manifest toManifest(Service service, Record record) {
		return Manifest.builder()
				.id(record.get(SERVICE_RELEASES.ID, EntityId.class).serialize())
				.name(service.name())
				.artifacts((Iterable<? extends ManifestEntry>) record.get(SERVICE_ARTIFACTS_ALIAS))
				.createdAt(record.get(SERVICE_RELEASES.CREATED_AT, Instant.class))
				.build();
	}

	private static ManifestEntry toManifestEntry(Record record) {
		final ArtifactSource source = record.get(SERVICE_ARTIFACTS.SOURCE, ArtifactSource.class);
		final boolean local = source == ArtifactSource.LOCAL;

		// LOCAL artifacts carry their own uploaded metadata checksum; ARTIFACTORY artifacts reuse the
		// checksum of the indexed artifact version, since service_artifacts.checksum is always null for them
		final String checksum = local
				? record.get(SERVICE_ARTIFACTS.CHECKSUM)
				: record.get(ARTIFACT_VERSIONS.CHECKSUM, Converter.from(ByteArray.class, String.class, ByteArray::encodeHex));

		// LOCAL artifacts are never indexed in the Artifactory's own tables, so their descriptive
		// metadata comes from what the plugin itself uploaded via upload(); ARTIFACTORY artifacts have
		// this already resolved through the artifacts join
		return ManifestEntry.builder()
				.groupId(record.get(SERVICE_ARTIFACTS.GROUP_ID))
				.artifactId(record.get(SERVICE_ARTIFACTS.ARTIFACT_ID))
				.version(record.get(SERVICE_ARTIFACTS.VERSION))
				.name(local ? record.get(SERVICE_ARTIFACTS.NAME) : record.get(ARTIFACTS.NAME))
				.description(local ? record.get(SERVICE_ARTIFACTS.DESCRIPTION) : record.get(ARTIFACTS.DESCRIPTION))
				.website(local ? record.get(SERVICE_ARTIFACTS.WEBSITE) : record.get(ARTIFACTS.WEBSITE))
				.repository(local ? record.get(SERVICE_ARTIFACTS.REPOSITORY) : record.get(ARTIFACTS.REPOSITORY))
				.resolvedAt(record.get(SERVICE_ARTIFACTS.CREATED_AT, Instant.class))
				.checksum(checksum)
				.source(source)
				.build();
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
		@Nullable
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
