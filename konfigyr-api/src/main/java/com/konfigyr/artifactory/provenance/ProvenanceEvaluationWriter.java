package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.artifactory.VersionedArtifact;
import com.konfigyr.artifactory.converter.ArtifactoryConverters;
import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.util.function.ThrowingConsumer;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@Slf4j
@RequiredArgsConstructor
class ProvenanceEvaluationWriter implements ItemWriter<@NonNull EvaluationResult> {

	private static final Marker MARKER = MarkerFactory.getMarker("PROPERTY_PROVENANCE_WRITER");

	private final DSLContext context;
	private final ArtifactoryConverters converters;

	@Override
	public void write(@NonNull Chunk<? extends @NonNull EvaluationResult> chunk) throws Exception {
		if (chunk.isEmpty()) {
			return;
		}

		aggregate(chunk, EvaluationResult.New.class, this::insert);
		aggregate(chunk, EvaluationResult.Unused.class, this::update);
	}

	private void insert(EvaluationResult.New result) {
		final Provenance provenance = result.provenance();
		final VersionedArtifact version = result.artifact();
		final PropertyDescriptor property = result.property();

		final Long identifier = context.insertInto(PROPERTY_DEFINITIONS)
				.set(PROPERTY_DEFINITIONS.ID, EntityId.generate().orElseThrow().get())
				.set(PROPERTY_DEFINITIONS.ARTIFACT_ID, version.artifact().get())
				.set(PROPERTY_DEFINITIONS.CHECKSUM, provenance.checksum())
				.set(PROPERTY_DEFINITIONS.NAME, property.name())
				.set(PROPERTY_DEFINITIONS.TYPE_NAME, property.typeName())
				.set(PROPERTY_DEFINITIONS.SCHEMA, converters.schema().to(property.schema()))
				.set(PROPERTY_DEFINITIONS.DEFAULT_VALUE, property.defaultValue())
				.set(PROPERTY_DEFINITIONS.DESCRIPTION, property.description())
				.set(PROPERTY_DEFINITIONS.DEPRECATION, converters.deprecation().to(property.deprecation()))
				.set(PROPERTY_DEFINITIONS.FIRST_SEEN, provenance.firstSeen().get())
				.set(PROPERTY_DEFINITIONS.LAST_SEEN, provenance.lastSeen().get())
				.set(PROPERTY_DEFINITIONS.OCCURRENCES, provenance.occurrences())
				.returning(PROPERTY_DEFINITIONS.ID)
				.fetchOne(PROPERTY_DEFINITIONS.ID);

		if (identifier == null) {
			throw new IllegalStateException("Failed to insert new property definition for: " + result);
		}

		context.insertInto(ARTIFACT_VERSION_PROPERTIES)
				.set(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID, version.id().get())
				.set(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID, identifier)
				.execute();

		log.info(MARKER, "Inserted new property definition for: [artifact={}, property={}, checksum={}]",
				version.coordinates().format(), property.name(), provenance.checksum().encode());
	}

	private void update(EvaluationResult.Unused result) {
		final VersionedArtifact version = result.artifact();
		final Provenance provenance = result.provenance();

		final Long identifier = context.update(PROPERTY_DEFINITIONS)
				.set(PROPERTY_DEFINITIONS.FIRST_SEEN, provenance.firstSeen().get())
				.set(PROPERTY_DEFINITIONS.LAST_SEEN, provenance.lastSeen().get())
				.set(PROPERTY_DEFINITIONS.OCCURRENCES, provenance.occurrences())
				.where(DSL.and(
						PROPERTY_DEFINITIONS.ARTIFACT_ID.eq(version.artifact().get()),
						PROPERTY_DEFINITIONS.CHECKSUM.eq(provenance.checksum())
				))
				.returning(PROPERTY_DEFINITIONS.ID)
				.fetchOne(PROPERTY_DEFINITIONS.ID);

		if (identifier == null) {
			throw new IllegalStateException("Failed to update existing property definition for: " + result);
		}

		context.insertInto(ARTIFACT_VERSION_PROPERTIES)
				.set(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID, version.id().get())
				.set(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID, identifier)
				.execute();

		log.info(MARKER, "Updated property definition for: [artifact={}, property={}, checksum={}]",
				version.coordinates().format(), result.property().name(), provenance.checksum().encode());
	}

	@SuppressWarnings("unchecked")
	private static <T extends EvaluationResult> void aggregate(
			Chunk<? extends @NonNull EvaluationResult> chunk,
			Class<? extends @NonNull EvaluationResult> type,
			ThrowingConsumer<@NonNull T> consumer
	) throws Exception {
		for (EvaluationResult result : chunk) {
			if (type.isInstance(result)) {
				consumer.acceptWithException((T) result);
			}
		}
	}
}
