package com.konfigyr.artifactory;

import com.konfigyr.artifactory.converter.DeprecationConverter;
import com.konfigyr.artifactory.converter.HintsConverter;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.data.Keys;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.version.Version;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@Slf4j
@RequiredArgsConstructor
class DefaultArtifactory implements Artifactory {

	private final DSLContext context;
	private final MetadataStore store;
	private final ApplicationEventPublisher eventPublisher;

	@NonNull
	@Override
	@Cacheable("artifactory.versioned-artifact")
	@Transactional(readOnly = true, label = "artifactory.get-versioned-artifact")
	public Optional<VersionedArtifact> get(@NonNull ArtifactCoordinates coordinates) {
		return context.select(
						ARTIFACT_VERSIONS.ID,
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
	public void release(@NonNull ArtifactCoordinates coordinates, @NonNull Resource metadata) {
		if (exists(coordinates)) {
			throw new ArtifactVersionExistsException(coordinates);
		}

		final Long artifactId = context.insertInto(ARTIFACTS)
				.set(
						SettableRecord.of(context, ARTIFACTS)
								.set(ARTIFACTS.ID, EntityId.generate().map(EntityId::get))
								.set(ARTIFACTS.GROUP_ID, coordinates.groupId())
								.set(ARTIFACTS.ARTIFACT_ID, coordinates.artifactId())
								.set(ARTIFACTS.CREATED_AT, OffsetDateTime.now())
								.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
								.get()
				)
				.onConflictOnConstraint(Keys.UNIQUE_ARTIFACT)
				.doUpdate()
				.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
				.returning(ARTIFACTS.ID)
				.fetchOne(ARTIFACTS.ID);

		final Long artifactVersionId = context.insertInto(ARTIFACT_VERSIONS)
				.set(
						SettableRecord.of(context, ARTIFACT_VERSIONS)
								.set(ARTIFACT_VERSIONS.ID, EntityId.generate().map(EntityId::get))
								.set(ARTIFACT_VERSIONS.ARTIFACT_ID, artifactId)
								.set(ARTIFACT_VERSIONS.VERSION, coordinates.version().get())
								.set(ARTIFACT_VERSIONS.RELEASED_AT, OffsetDateTime.now())
								.get()
				)
				.returning(ARTIFACT_VERSIONS.ID)
				.fetchOne(ARTIFACT_VERSIONS.ID);

		Assert.state(artifactVersionId != null, "Failed to insert new artifact version record");

		try {
			store.save(coordinates, metadata);
		} catch (Exception ex) {
			throw new ArtifactoryException("Unexpected error occurred while storing metadata for artifact: " + coordinates.format(), ex);
		}

		eventPublisher.publishEvent(new ArtifactoryEvent.Release(EntityId.from(artifactVersionId), coordinates));
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
				.fetch(DefaultArtifactory::toPropertyDefinition);
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
				.version(Version.of(record.get(ARTIFACT_VERSIONS.VERSION)))
				.name(record.get(ARTIFACTS.NAME))
				.description(record.get(ARTIFACTS.DESCRIPTION))
				.website(record.get(ARTIFACTS.WEBSITE))
				.repository(record.get(ARTIFACTS.REPOSITORY))
				.releasedAt(record.get(ARTIFACT_VERSIONS.RELEASED_AT))
				.build();
	}

	static PropertyDefinition toPropertyDefinition(Record record) {
		return PropertyDefinition.builder()
				.id(record.get(PROPERTY_DEFINITIONS.ID))
				.artifact(record.get(PROPERTY_DEFINITIONS.ARTIFACT_ID))
				.checksum(record.get(PROPERTY_DEFINITIONS.CHECKSUM))
				.name(record.get(PROPERTY_DEFINITIONS.NAME))
				.type(record.get(PROPERTY_DEFINITIONS.TYPE))
				.dataType(record.get(PROPERTY_DEFINITIONS.DATA_TYPE))
				.typeName(record.get(PROPERTY_DEFINITIONS.TYPE_NAME))
				.defaultValue(record.get(PROPERTY_DEFINITIONS.DEFAULT_VALUE))
				.description(record.get(PROPERTY_DEFINITIONS.DESCRIPTION))
				.hints(record.get(PROPERTY_DEFINITIONS.HINTS, new HintsConverter()))
				.deprecation(record.get(PROPERTY_DEFINITIONS.DEPRECATION, new DeprecationConverter()))
				.occurrences(record.get(PROPERTY_DEFINITIONS.OCCURRENCES))
				.firstSeen(record.get(PROPERTY_DEFINITIONS.FIRST_SEEN))
				.lastSeen(record.get(PROPERTY_DEFINITIONS.LAST_SEEN))
				.build();
	}
}
