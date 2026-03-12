package com.konfigyr.artifactory;

import com.konfigyr.artifactory.converter.ArtifactoryConverters;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jooq.Condition;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@Slf4j
@RequiredArgsConstructor
class DefaultArtifactory implements Artifactory {

	private final DSLContext context;
	private final MetadataStore store;
	private final ArtifactoryConverters converters;
	private final ApplicationEventPublisher eventPublisher;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-versioned-artifact")
	public Optional<VersionedArtifact> get(@NonNull ArtifactCoordinates coordinates) {
		return context.select(
						ARTIFACT_VERSIONS.ID,
						ARTIFACT_VERSIONS.STATE,
						ARTIFACT_VERSIONS.CHECKSUM,
						ARTIFACTS.ID,
						ARTIFACTS.GROUP_ID,
						ARTIFACTS.ARTIFACT_ID,
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
				.where(toCondition(coordinates))
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
	@Transactional(label = "artifactory.release-artifact-component")
	public VersionedArtifact release(@NonNull ArtifactMetadata metadata) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.of(metadata);

		if (exists(coordinates)) {
			throw new ArtifactVersionExistsException(coordinates);
		}

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

		final Long artifactId = context.insertInto(ARTIFACTS)
				.set(
						SettableRecord.of(context, ARTIFACTS)
								.set(ARTIFACTS.ID, EntityId.generate().map(EntityId::get))
								.set(ARTIFACTS.GROUP_ID, coordinates.groupId())
								.set(ARTIFACTS.ARTIFACT_ID, coordinates.artifactId())
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
				.returning(ARTIFACTS.ID)
				.fetchOne(ARTIFACTS.ID);

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

		eventPublisher.publishEvent(new ArtifactoryEvent.ReleaseCreated(EntityId.from(artifactVersionId), coordinates));

		return VersionedArtifact.from(metadata)
				.id(artifactVersionId)
				.artifact(artifactId)
				.state(ReleaseState.PENDING)
				.checksum(checksum.encode())
				.releasedAt(Instant.now())
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

		return context.select(PROPERTY_DEFINITIONS.fields())
				.from(ARTIFACT_VERSION_PROPERTIES)
				.innerJoin(PROPERTY_DEFINITIONS)
				.on(PROPERTY_DEFINITIONS.ID.eq(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID))
				.where(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID.eq(artifactVersionId))
				.fetch(record -> toPropertyDefinition(record, converters));
	}

	@NonNull
	static Condition toCondition(@NonNull ArtifactCoordinates coordinates) {
		return DSL.and(
				ARTIFACTS.GROUP_ID.eq(coordinates.groupId()),
				ARTIFACTS.ARTIFACT_ID.eq(coordinates.artifactId()),
				ARTIFACT_VERSIONS.VERSION.eq(coordinates.version().get())
		);
	}

	static VersionedArtifact toVersionedArtifact(Record record) {
		return VersionedArtifact.builder()
				.id(record.get(ARTIFACT_VERSIONS.ID))
				.artifact(record.get(ARTIFACTS.ID))
				.groupId(record.get(ARTIFACTS.GROUP_ID))
				.artifactId(record.get(ARTIFACTS.ARTIFACT_ID))
				.version(record.get(ARTIFACT_VERSIONS.VERSION))
				.state(record.get(ARTIFACT_VERSIONS.STATE, ReleaseState.class))
				.checksum(record.get(ARTIFACT_VERSIONS.CHECKSUM, Converter.from(ByteArray.class, String.class, ByteArray::encode)))
				.name(record.get(ARTIFACTS.NAME))
				.description(record.get(ARTIFACTS.DESCRIPTION))
				.website(record.get(ARTIFACTS.WEBSITE))
				.repository(record.get(ARTIFACTS.REPOSITORY))
				.releasedAt(record.get(ARTIFACT_VERSIONS.RELEASED_AT))
				.build();
	}

	static PropertyDefinition toPropertyDefinition(Record record, ArtifactoryConverters converters) {
		return PropertyDefinition.builder()
				.id(record.get(PROPERTY_DEFINITIONS.ID))
				.artifact(record.get(PROPERTY_DEFINITIONS.ARTIFACT_ID))
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
