package com.konfigyr.artifactory;

import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.artifactory.ownership.GroupVerifications;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import io.micrometer.observation.annotation.ObservationKeyValue;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jooq.Condition;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@Slf4j
@RequiredArgsConstructor
class DefaultArtifactory implements Artifactory {

	private final DSLContext context;
	private final MetadataStore store;
	private final ArtifactoryConverters converters;
	private final ApplicationEventPublisher eventPublisher;
	private final GroupVerifications groupVerifications;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-artifact-definition")
	public Optional<ArtifactDefinition> get(@NonNull ArtifactKey key) {
		return createArtifactDefinitionQuery()
				.where(toCondition(key))
				.fetchOptional(DefaultArtifactory::toArtifactDefinition);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-visible-artifact-definition")
	public Optional<ArtifactDefinition> get(@Nullable Owner owner, @NonNull ArtifactKey key) {
		return createArtifactDefinitionQuery()
				.where(DSL.and(visibilityCondition(owner), toCondition(key)))
				.fetchOptional(DefaultArtifactory::toArtifactDefinition);
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.artifact-key-exists")
	public boolean exists(@NonNull ArtifactKey key) {
		return context.fetchExists(ARTIFACTS, toCondition(key));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.visible-artifact-key-exists")
	public boolean exists(@Nullable Owner owner, @NonNull ArtifactKey key) {
		return context.fetchExists(ARTIFACTS, DSL.and(toCondition(key), visibilityCondition(owner)));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-versioned-artifact")
	public Optional<VersionedArtifact> get(@NonNull ArtifactCoordinates coordinates) {
		return createVersionedArtifactQuery()
				.where(toCondition(coordinates))
				.fetchOptional(DefaultArtifactory::toVersionedArtifact);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-visible-versioned-artifact")
	public Optional<VersionedArtifact> get(@Nullable Owner owner, @NonNull ArtifactCoordinates coordinates) {
		return createVersionedArtifactQuery()
				.where(DSL.and(toCondition(coordinates), visibilityCondition(owner)))
				.fetchOptional(DefaultArtifactory::toVersionedArtifact);
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.coordinates-exists")
	public boolean exists(@NonNull ArtifactCoordinates coordinates) {
		return context.fetchExists(
				DSL.select(ARTIFACT_VERSIONS.ID)
						.from(ARTIFACT_VERSIONS)
						.innerJoin(ARTIFACTS)
						.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
						.where(toCondition(coordinates))
		);
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.visible-coordinates-exists")
	public boolean exists(@Nullable Owner owner, @NonNull ArtifactCoordinates coordinates) {
		return context.fetchExists(
				DSL.select(ARTIFACT_VERSIONS.ID)
						.from(ARTIFACT_VERSIONS)
						.innerJoin(ARTIFACTS)
						.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
						.where(DSL.and(toCondition(coordinates), visibilityCondition(owner)))
		);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.existing-coordinates")
	public Set<Publication> existing(@NonNull Owner owner, @NonNull Collection<ArtifactCoordinates> coordinates) {
		if (coordinates.isEmpty()) {
			return Set.of();
		}

		final String[] groupIds = coordinates.stream().map(ArtifactCoordinates::groupId).toArray(String[]::new);
		final String[] artifactIds = coordinates.stream().map(ArtifactCoordinates::artifactId).toArray(String[]::new);
		final String[] versions = coordinates.stream().map(coordinate -> coordinate.version().get()).toArray(String[]::new);

		return createVersionedArtifactQuery()
				.where(DSL.and(
						DSL.row(ARTIFACTS.GROUP_ID, ARTIFACTS.ARTIFACT_ID, ARTIFACT_VERSIONS.VERSION)
								.in(DSL.select(DSL.field("g", String.class), DSL.field("a", String.class), DSL.field("v", String.class))
										.from("unnest({0}::text[], {1}::text[], {2}::text[]) AS t(g, a, v)",
												groupIds, artifactIds, versions)),
						visibilityCondition(owner)
				))
				.fetchSet(DefaultArtifactory::toVersionedArtifact);
	}

	@Override
	@Observed(name = "konfigyr.artifactory.release")
	@Transactional(label = "artifactory.release-artifact-component")
	public VersionedArtifact publish(
			@NonNull Owner owner,
			@NonNull
			@ObservationKeyValue(key = "konfigyr.artifactory.artifact", expression = "#this")
			ArtifactMetadata metadata
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(metadata);

		if (exists(coordinates)) {
			throw new ArtifactVersionExistsException(coordinates);
		}

		assertCanRelease(owner, metadata.groupId());
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

		final Record artifactRecord = context.insertInto(ARTIFACTS)
				.set(
						SettableRecord.of(context, ARTIFACTS)
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

		final Long artifactVersionId = context.insertInto(ARTIFACT_VERSIONS)
				.set(
						SettableRecord.of(context, ARTIFACT_VERSIONS)
								.set(ARTIFACT_VERSIONS.ID, EntityId.generate().map(EntityId::get))
								.set(ARTIFACT_VERSIONS.ARTIFACT_ID, artifactId)
								.set(ARTIFACT_VERSIONS.VERSION, coordinates.version().get())
								.set(ARTIFACT_VERSIONS.STATE, ReleaseState.PENDING.name())
								.set(ARTIFACT_VERSIONS.CHECKSUM, checksum)
								.set(ARTIFACT_VERSIONS.RELEASED_AT, OffsetDateTime.now())
								.get()
				)
				.returning(ARTIFACT_VERSIONS.ID)
				.fetchOne(ARTIFACT_VERSIONS.ID);

		Assert.state(artifactVersionId != null, "Failed to insert new artifact version record");

		try {
			store.save(coordinates, resource);
		} catch (Exception ex) {
			throw new ArtifactoryException("Unexpected error occurred while storing metadata for artifact: " + coordinates.format(), ex);
		}

		eventPublisher.publishEvent(new ArtifactoryEvent.PublicationCreated(EntityId.from(artifactVersionId), coordinates));

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

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.version-properties")
	public List<PropertyDefinition> properties(@NonNull ArtifactCoordinates coordinates) {
		final Long artifactVersionId = context.select(ARTIFACT_VERSIONS.ID)
				.from(ARTIFACT_VERSIONS)
				.innerJoin(ARTIFACTS)
				.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
				.where(toCondition(coordinates))
				.fetchOptional(ARTIFACT_VERSIONS.ID)
				.orElseThrow(() -> new ArtifactVersionNotFoundException(coordinates));

		return createPropertySearchQuery()
				.innerJoin(ARTIFACT_VERSION_PROPERTIES)
				.on(PROPERTY_DEFINITIONS.ID.eq(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID))
				.where(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID.eq(artifactVersionId))
				.fetch(record -> toPropertyDefinition(record, converters));
	}

	@Override
	@Transactional(label = "artifactory.change-visibility")
	public void changeVisibility(
			@NonNull Owner owner,
			@NonNull ArtifactKey key,
			@NonNull ArtifactVisibility visibility
	) {
		final Long existingOwnerId = context.select(ARTIFACTS.NAMESPACE_ID)
				.from(ARTIFACTS)
				.where(toCondition(key))
				.fetchOptional(ARTIFACTS.NAMESPACE_ID)
				.orElseThrow(() -> new ArtifactDefinitionNotFoundException(key));

		if (!existingOwnerId.equals(owner.id().get())) {
			throw new ArtifactOwnershipMismatchException(key, owner);
		}

		context.update(ARTIFACTS)
				.set(ARTIFACTS.VISIBILITY, visibility.name())
				.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
				.where(toCondition(key))
				.execute();
	}

	@NonNull
	private SelectJoinStep<? extends Record> createArtifactDefinitionQuery() {
		return context.select(ARTIFACTS.fields())
				.select(NAMESPACES.ID, NAMESPACES.SLUG)
				.from(ARTIFACTS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID));
	}

	@NonNull
	private SelectJoinStep<? extends Record> createVersionedArtifactQuery() {
		return context.select(
						ARTIFACT_VERSIONS.ID,
						ARTIFACT_VERSIONS.STATE,
						ARTIFACT_VERSIONS.CHECKSUM,
						ARTIFACTS.ID,
						ARTIFACTS.GROUP_ID,
						ARTIFACTS.ARTIFACT_ID,
						ARTIFACTS.VISIBILITY,
						NAMESPACES.ID,
						NAMESPACES.SLUG,
						ARTIFACT_VERSIONS.VERSION,
						ARTIFACTS.NAME,
						ARTIFACTS.DESCRIPTION,
						ARTIFACTS.WEBSITE,
						ARTIFACTS.REPOSITORY,
						ARTIFACT_VERSIONS.RELEASED_AT
				)
				.from(ARTIFACT_VERSIONS)
				.innerJoin(ARTIFACTS)
				.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID));
	}

	@NonNull
	private SelectJoinStep<? extends Record> createPropertySearchQuery() {
		return context.select(PROPERTY_DEFINITIONS.fields())
				.select(ARTIFACTS.GROUP_ID, ARTIFACTS.ARTIFACT_ID, NAMESPACES.ID, NAMESPACES.SLUG)
				.from(PROPERTY_DEFINITIONS)
				.innerJoin(ARTIFACTS)
				.on(ARTIFACTS.ID.eq(PROPERTY_DEFINITIONS.ARTIFACT_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID));
	}

	private void assertCanRelease(Owner owner, String groupId) {
		groupVerifications.findActiveCovering(owner, groupId)
				.orElseThrow(() -> new GroupIdNotVerifiedException(groupId, owner));
	}

	private void assertSameOwner(Owner owner, ArtifactCoordinates coordinates) {
		final boolean ownedByAnotherNamespace = context.fetchExists(
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

	@NonNull
	static Condition visibilityCondition(@Nullable Owner owner) {
		if (owner == null) {
			return ARTIFACTS.VISIBILITY.eq(ArtifactVisibility.PUBLIC.name());
		}

		return DSL.or(
				ARTIFACTS.VISIBILITY.eq(ArtifactVisibility.PUBLIC.name()),
				ARTIFACTS.NAMESPACE_ID.eq(owner.id().get())
		);
	}

	@NonNull
	static Condition toCondition(@NonNull ArtifactKey key) {
		return DSL.and(
				ARTIFACTS.GROUP_ID.eq(key.groupId()),
				ARTIFACTS.ARTIFACT_ID.eq(key.artifactId())
		);
	}

	@NonNull
	static Condition toCondition(@NonNull ArtifactCoordinates coordinates) {
		return DSL.and(
				ARTIFACTS.GROUP_ID.eq(coordinates.groupId()),
				ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()),
				ARTIFACT_VERSIONS.VERSION.eq(coordinates.version().get())
		);
	}

	static Owner toOwner(Record record) {
		return new Owner(record.get(NAMESPACES.ID, EntityId.class), record.get(NAMESPACES.SLUG));
	}

	static ArtifactDefinition toArtifactDefinition(Record record) {
		return ArtifactDefinition.builder()
				.id(record.get(ARTIFACTS.ID))
				.owner(toOwner(record))
				.groupId(record.get(ARTIFACTS.GROUP_ID))
				.artifactId(record.get(ARTIFACTS.ARTIFACT_ID))
				.visibility(record.get(ARTIFACTS.VISIBILITY, ArtifactVisibility.class))
				.name(record.get(ARTIFACTS.NAME))
				.description(record.get(ARTIFACTS.DESCRIPTION))
				.website(record.get(ARTIFACTS.WEBSITE))
				.repository(record.get(ARTIFACTS.REPOSITORY))
				.createdAt(record.get(ARTIFACTS.CREATED_AT))
				.updatedAt(record.get(ARTIFACTS.UPDATED_AT))
				.build();
	}

	static VersionedArtifact toVersionedArtifact(Record record) {
		return toVersionedArtifact(record, toOwner(record));
	}

	static VersionedArtifact toVersionedArtifact(Record record, Owner owner) {
		return VersionedArtifact.builder()
				.id(record.get(ARTIFACT_VERSIONS.ID))
				.artifact(record.get(ARTIFACTS.ID))
				.owner(owner)
				.groupId(record.get(ARTIFACTS.GROUP_ID))
				.artifactId(record.get(ARTIFACTS.ARTIFACT_ID))
				.visibility(record.get(ARTIFACTS.VISIBILITY, ArtifactVisibility.class))
				.version(record.get(ARTIFACT_VERSIONS.VERSION))
				.state(record.get(ARTIFACT_VERSIONS.STATE, PublicationState.class))
				.checksum(record.get(ARTIFACT_VERSIONS.CHECKSUM, Converter.from(ByteArray.class, String.class, ByteArray::encodeHex)))
				.name(record.get(ARTIFACTS.NAME))
				.description(record.get(ARTIFACTS.DESCRIPTION))
				.website(record.get(ARTIFACTS.WEBSITE))
				.repository(record.get(ARTIFACTS.REPOSITORY))
				.publishedAt(record.get(ARTIFACT_VERSIONS.RELEASED_AT))
				.build();
	}

	static PropertyDefinition toPropertyDefinition(Record record, ArtifactoryConverters converters) {
		return PropertyDefinition.builder()
				.id(record.get(PROPERTY_DEFINITIONS.ID))
				.artifact(record.get(PROPERTY_DEFINITIONS.ARTIFACT_ID))
				.groupId(record.get(ARTIFACTS.GROUP_ID))
				.artifactId(record.get(ARTIFACTS.ARTIFACT_ID))
				.owner(toOwner(record))
				.checksum(record.get(PROPERTY_DEFINITIONS.CHECKSUM))
				.name(record.get(PROPERTY_DEFINITIONS.NAME))
				.typeName(record.get(PROPERTY_DEFINITIONS.TYPE_NAME))
				.defaultValue(record.get(PROPERTY_DEFINITIONS.DEFAULT_VALUE))
				.description(record.get(PROPERTY_DEFINITIONS.DESCRIPTION))
				.schema(record.get(PROPERTY_DEFINITIONS.SCHEMA, converters.schema()))
				.deprecation(record.get(PROPERTY_DEFINITIONS.DEPRECATION, converters.deprecation()))
				.occurrences(record.get(PROPERTY_DEFINITIONS.OCCURRENCES))
				.firstSeen(record.get(PROPERTY_DEFINITIONS.FIRST_SEEN))
				.lastSeen(record.get(PROPERTY_DEFINITIONS.LAST_SEEN))
				.build();
	}
}
