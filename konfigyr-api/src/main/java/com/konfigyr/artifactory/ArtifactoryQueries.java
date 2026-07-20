package com.konfigyr.artifactory;

import com.konfigyr.data.PageableExecutor;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@NullMarked
@RequiredArgsConstructor
final class ArtifactoryQueries {

	private static final PageableExecutor artifactDefinitionPageableExecutor = PageableExecutor.builder()
			.defaultSortField(ARTIFACTS.CREATED_AT.desc())
			.sortField("name", ARTIFACTS.NAME)
			.sortField("groupId", ARTIFACTS.GROUP_ID)
			.sortField("artifactId", ARTIFACTS.ARTIFACT_ID)
			.build();

	private static final PageableExecutor versionedArtifactPageableExecutor = PageableExecutor.builder()
			.defaultSortField(ARTIFACT_VERSIONS.RELEASED_AT.desc())
			.sortField("date", ARTIFACT_VERSIONS.RELEASED_AT)
			.sortField("name", ARTIFACTS.NAME)
			.sortField("groupId", ARTIFACTS.GROUP_ID)
			.sortField("artifactId", ARTIFACTS.ARTIFACT_ID)
			.sortField("version", ARTIFACT_VERSIONS.VERSION)
			.build();

	private static final PageableExecutor propertySearchPageableExecutor = PageableExecutor.builder()
			.defaultSortField(PROPERTY_DEFINITIONS.NAME.asc())
			.sortField("name", PROPERTY_DEFINITIONS.NAME)
			.build();

	private final DSLContext context;
	private final ArtifactoryConverters converters;

	DSLContext context() {
		return context;
	}

	ArtifactoryConverters converters() {
		return converters;
	}

	Page<ArtifactDefinition> definitions(Condition condition, Pageable pageable) {
		return artifactDefinitionPageableExecutor.execute(
				createArtifactDefinitionQuery().where(condition),
				ArtifactoryQueries::toArtifactDefinition,
				pageable,
				() -> context.fetchCount(createArtifactDefinitionQuery().where(condition))
		);
	}

	Optional<ArtifactDefinition> definition(Condition condition) {
		return createArtifactDefinitionQuery()
				.where(condition)
				.fetchOptional(ArtifactoryQueries::toArtifactDefinition);
	}

	Page<VersionedArtifact> versions(Condition condition, Pageable pageable) {
		return versionedArtifactPageableExecutor.execute(
				createVersionedArtifactQuery().where(condition),
				ArtifactoryQueries::toVersionedArtifact,
				pageable,
				() -> context.fetchCount(createVersionedArtifactQuery().where(condition))
		);
	}

	Optional<VersionedArtifact> version(Condition condition) {
		return createVersionedArtifactQuery()
				.where(condition)
				.fetchOptional(ArtifactoryQueries::toVersionedArtifact);
	}

	Page<PropertyDefinition> properties(Condition condition, Pageable pageable) {
		return propertySearchPageableExecutor.execute(
				createPropertySearchQuery().where(condition),
				record -> toPropertyDefinition(record, converters),
				pageable,
				() -> context.fetchCount(createPropertySearchQuery().where(condition))
		);
	}

	Optional<PropertyDefinition> property(Condition condition) {
		return createPropertySearchQuery()
				.where(condition)
				.fetchOptional(record -> toPropertyDefinition(record, converters));
	}

	SelectJoinStep<? extends Record> createArtifactDefinitionQuery() {
		return context.select(ARTIFACTS.fields())
				.select(NAMESPACES.ID, NAMESPACES.SLUG)
				.from(ARTIFACTS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID));
	}

	SelectJoinStep<? extends Record> createVersionedArtifactQuery() {
		return context.select(ARTIFACT_VERSIONS.fields())
				.select(ARTIFACTS.ID, ARTIFACTS.GROUP_ID, ARTIFACTS.ARTIFACT_ID, ARTIFACTS.VISIBILITY,
						ARTIFACTS.NAME, ARTIFACTS.DESCRIPTION, ARTIFACTS.WEBSITE, ARTIFACTS.REPOSITORY)
				.select(NAMESPACES.ID, NAMESPACES.SLUG)
				.from(ARTIFACT_VERSIONS)
				.innerJoin(ARTIFACTS)
				.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID));
	}

	SelectJoinStep<? extends Record> createPropertySearchQuery() {
		return context.select(PROPERTY_DEFINITIONS.fields())
				.select(ARTIFACTS.GROUP_ID, ARTIFACTS.ARTIFACT_ID, NAMESPACES.ID, NAMESPACES.SLUG)
				.from(PROPERTY_DEFINITIONS)
				.innerJoin(ARTIFACTS)
				.on(ARTIFACTS.ID.eq(PROPERTY_DEFINITIONS.ARTIFACT_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID));
	}

	static Owner toOwner(Record record) {
		return new Owner(
				record.get(NAMESPACES.ID, EntityId.class),
				record.get(NAMESPACES.SLUG)
		);
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
				.artifact(record.get(ARTIFACT_VERSIONS.ARTIFACT_ID))
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
