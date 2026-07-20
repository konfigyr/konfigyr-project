package com.konfigyr.artifactory;

import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.artifactory.ownership.GroupVerifications;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.support.SearchQuery;
import io.micrometer.observation.annotation.ObservationKeyValue;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class DefaultPublications implements Publications {

	private final MetadataStore store;
	private final ArtifactoryQueries queries;
	private final GroupVerifications verifications;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true, label = "artifactory.publications.search-artifact-definition")
	public Page<ArtifactDefinition> artifacts(Owner owner, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(toCondition(owner));

		query.criteria(ArtifactKey.GROUP_ID_CRITERIA)
				.map(ARTIFACTS.GROUP_ID::equalIgnoreCase)
				.ifPresent(conditions::add);

		query.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA)
				.map(ARTIFACTS.ARTIFACT_ID::equalIgnoreCase)
				.ifPresent(conditions::add);

		query.term().ifPresent(term -> conditions.add(DSL.or(
				ARTIFACTS.GROUP_ID.containsIgnoreCase(term),
				ARTIFACTS.ARTIFACT_ID.containsIgnoreCase(term),
				ARTIFACTS.NAME.containsIgnoreCase(term),
				ARTIFACTS.DESCRIPTION.containsIgnoreCase(term)
		)));

		return queries.definitions(DSL.and(conditions), query.pageable());
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.publications.get-artifact-definition")
	public Optional<ArtifactDefinition> get(Owner owner, ArtifactKey key) {
		return queries.definition(toCondition(owner, key));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.publications.artifact-definition-exists")
	public boolean exists(Owner owner, ArtifactKey key) {
		return queries.context().fetchExists(ARTIFACTS, toCondition(owner, key));
	}

	@Override
	@Transactional(label = "artifactory.publications.deregister-artifact-definition")
	public void deregister(Owner owner, ArtifactKey key) {
		if (!exists(owner, key)) {
			throw new ArtifactDefinitionNotFoundException(key);
		}

		final EntityId artifactId = queries.context().delete(ARTIFACTS)
					.where(toCondition(owner, key))
					.returning(ARTIFACTS.ID)
					.fetchOne(ARTIFACTS.ID, EntityId.class);

		Assert.state(artifactId != null, () -> "Failed to deregister artifact: %s".formatted(key.format()));
		eventPublisher.publishEvent(new ArtifactoryEvent.Deregistered(artifactId, owner, key));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.publications.search-artifact-versions")
	public Page<VersionedArtifact> versions(Owner owner, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(toCondition(owner));

		query.criteria(ArtifactKey.GROUP_ID_CRITERIA)
				.map(ARTIFACTS.GROUP_ID::equalIgnoreCase)
				.ifPresent(conditions::add);

		query.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA)
				.map(ARTIFACTS.ARTIFACT_ID::equalIgnoreCase)
				.ifPresent(conditions::add);

		query.criteria(ArtifactCoordinates.VERSION_CRITERIA)
				.map(ARTIFACT_VERSIONS.VERSION::equalIgnoreCase)
				.ifPresent(conditions::add);

		query.term().ifPresent(term -> conditions.add(DSL.or(
				ARTIFACTS.GROUP_ID.containsIgnoreCase(term),
				ARTIFACTS.ARTIFACT_ID.containsIgnoreCase(term),
				ARTIFACT_VERSIONS.VERSION.containsIgnoreCase(term),
				ARTIFACTS.NAME.containsIgnoreCase(term),
				ARTIFACTS.DESCRIPTION.containsIgnoreCase(term)
		)));

		return queries.versions(DSL.and(conditions), query.pageable());
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.publications.get-artifact-version")
	public Optional<VersionedArtifact> get(Owner owner, ArtifactCoordinates coordinates) {
		return queries.version(toCondition(owner, coordinates));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.publications.artifact-version-exists")
	public boolean exists(Owner owner, ArtifactCoordinates coordinates) {
		return queries.context().fetchExists(
				DSL.select(ARTIFACT_VERSIONS.ID)
						.from(ARTIFACT_VERSIONS)
						.innerJoin(ARTIFACTS)
						.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
						.where(toCondition(owner, coordinates))
		);
	}

	@Override
	@Observed(name = "konfigyr.artifactory.release")
	@Transactional(label = "artifactory.publications.publish-artifact-metadata")
	public VersionedArtifact publish(
			Owner owner,
			@ObservationKeyValue(key = "konfigyr.artifactory.artifact", expression = "#this")
			ArtifactMetadata metadata
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(metadata);
		final ArtifactoryConverters converters = queries.converters();

		if (exists(owner, coordinates)) {
			throw new ArtifactVersionExistsException(coordinates);
		}

		if (verifications.findActiveCovering(owner, coordinates.groupId()).isEmpty()) {
			throw new GroupIdNotVerifiedException(coordinates.groupId(), owner);
		}

		assertSameOwner(owner, coordinates);

		final ByteArray checksum;
		final Resource resource;

		try (ByteArrayOutputStream os = new ByteArrayOutputStream(metadata.properties().size() * 128)) {
			final DigestOutputStream dos = new DigestOutputStream(os, MessageDigest.getInstance("SHA-256"));
			converters.mapper().writeValue(dos, metadata.properties());

			resource = new ByteArrayResource(os.toByteArray(), coordinates.format() + ".json");
			checksum = new ByteArray(dos.getMessageDigest().digest());
		} catch (Exception ex) {
			throw new ArtifactoryException("Unexpected error occurred while calculating metadata checksum for artifact: " + coordinates.format(), ex);
		}

		final Record artifactRecord = queries.context().insertInto(ARTIFACTS)
				.set(
						SettableRecord.of(queries.context(), ARTIFACTS)
								.set(ARTIFACTS.ID, EntityId.generate().map(EntityId::get))
								.set(ARTIFACTS.NAMESPACE_ID, owner.id().get())
								.set(ARTIFACTS.GROUP_ID, coordinates.groupId())
								.set(ARTIFACTS.ARTIFACT_ID, coordinates.artifactId())
								.set(ARTIFACTS.VISIBILITY, ArtifactVisibility.PRIVATE.name())
								.set(ARTIFACTS.NAME, metadata.name())
								.set(ARTIFACTS.DESCRIPTION, metadata.description())
								.set(ARTIFACTS.WEBSITE, metadata.website(), converters.uri())
								.set(ARTIFACTS.REPOSITORY, metadata.repository(), converters.uri())
								.set(ARTIFACTS.CREATED_AT, OffsetDateTime.now())
								.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
								.get()
				)
				.onConflictOnConstraint(Keys.UNIQUE_ARTIFACT)
				.doUpdate()
				.set(ARTIFACTS.NAME, metadata.name())
				.set(ARTIFACTS.DESCRIPTION, metadata.description())
				.set(ARTIFACTS.WEBSITE, converters.uri().to(metadata.website()))
				.set(ARTIFACTS.REPOSITORY, converters.uri().to(metadata.repository()))
				.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
				.returning(ARTIFACTS.ID, ARTIFACTS.VISIBILITY)
				.fetchOne();

		Assert.state(artifactRecord != null, "Failed to insert new artifact record");

		final Long artifactId = artifactRecord.get(ARTIFACTS.ID);
		final ArtifactVisibility visibility = artifactRecord.get(ARTIFACTS.VISIBILITY, ArtifactVisibility.class);

		final EntityId artifactVersionId = queries.context().insertInto(ARTIFACT_VERSIONS)
				.set(
						SettableRecord.of(queries.context(), ARTIFACT_VERSIONS)
								.set(ARTIFACT_VERSIONS.ID, EntityId.generate().map(EntityId::get))
								.set(ARTIFACT_VERSIONS.ARTIFACT_ID, artifactId)
								.set(ARTIFACT_VERSIONS.VERSION, coordinates.version().get())
								.set(ARTIFACT_VERSIONS.STATE, ReleaseState.PENDING.name())
								.set(ARTIFACT_VERSIONS.CHECKSUM, checksum)
								.set(ARTIFACT_VERSIONS.RELEASED_AT, OffsetDateTime.now())
								.get()
				)
				.returning(ARTIFACT_VERSIONS.ID)
				.fetchOne(ARTIFACT_VERSIONS.ID, EntityId.class);

		Assert.state(artifactVersionId != null, () -> "Failed to insert new artifact version record for: %s"
				.formatted(coordinates.format()));

		try {
			store.save(coordinates, resource);
		} catch (Exception ex) {
			throw new ArtifactoryException("Unexpected error occurred while storing metadata for artifact: " + coordinates.format(), ex);
		}

		eventPublisher.publishEvent(new ArtifactoryEvent.PublicationCreated(artifactVersionId, owner, coordinates));

		return VersionedArtifact.from(metadata)
				.id(artifactVersionId)
				.artifact(artifactId)
				.owner(owner)
				.visibility(visibility)
				.state(PublicationState.PENDING)
				.checksum(checksum.encodeHex())
				.publishedAt(Instant.now())
				.build();
	}

	@Override
	@Transactional(label = "artifactory.publications.retract-artifact-version")
	public void retract(Owner owner, ArtifactCoordinates coordinates) {
		if (!exists(owner, coordinates)) {
			throw new ArtifactVersionNotFoundException(coordinates);
		}

		final EntityId artifactVersionId = queries.context().delete(ARTIFACT_VERSIONS)
					.using(ARTIFACTS)
					.where(DSL.and(
							ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID),
							toCondition(owner, coordinates)
					))
					.returning(ARTIFACT_VERSIONS.ID)
					.fetchOne(ARTIFACT_VERSIONS.ID, EntityId.class);

		Assert.state(artifactVersionId != null, () -> "Failed to retract artifact version: %s".formatted(coordinates.format()));
		eventPublisher.publishEvent(new ArtifactoryEvent.PublicationRetracted(artifactVersionId, owner, coordinates));
	}

	@Override
	@Transactional(label = "artifactory.publications.update-artifact-visibility")
	public void changeVisibility(Owner owner, ArtifactKey key, ArtifactVisibility visibility) {
		if (!exists(owner, key)) {
			throw new ArtifactDefinitionNotFoundException(key);
		}

		final EntityId artifactId = queries.context().update(ARTIFACTS)
				.set(ARTIFACTS.VISIBILITY, visibility.name())
				.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
				.where(toCondition(owner, key))
				.returning(ARTIFACTS.ID)
				.fetchOne(ARTIFACTS.ID, EntityId.class);

		Assert.state(artifactId != null, () -> "Failed to update visibility for artifact: %s".formatted(key.format()));
		eventPublisher.publishEvent(new ArtifactoryEvent.VisibilityChanged(artifactId, owner, key, visibility));
	}

	private void assertSameOwner(Owner owner, ArtifactCoordinates coordinates) {
		final boolean ownedByAnotherNamespace = queries.context().fetchExists(
				DSL.select(ARTIFACTS.ID)
						.from(ARTIFACTS)
						.where(
								ARTIFACTS.GROUP_ID.eq(coordinates.groupId()),
								ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()),
								ARTIFACTS.NAMESPACE_ID.ne(owner.id().get())
						)
		);

		if (ownedByAnotherNamespace) {
			throw new ArtifactOwnershipMismatchException(coordinates, owner);
		}
	}

	static Condition toCondition(Owner owner) {
		return ARTIFACTS.NAMESPACE_ID.eq(owner.id().get());
	}

	static Condition toCondition(ArtifactKey key) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(ARTIFACTS.GROUP_ID.eq(key.groupId()));
		conditions.add(ARTIFACTS.ARTIFACT_ID.eq(key.artifactId()));

		if (key instanceof ArtifactCoordinates coordinates) {
			conditions.add(ARTIFACT_VERSIONS.VERSION.eq(coordinates.version().get()));
		}

		return DSL.and(conditions);
	}

	static Condition toCondition(Owner owner, ArtifactKey key) {
		return DSL.and(toCondition(owner), toCondition(key));
	}
}
