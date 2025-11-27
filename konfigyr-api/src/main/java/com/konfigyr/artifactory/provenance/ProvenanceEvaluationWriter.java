package com.konfigyr.artifactory.provenance;

import com.konfigyr.artifactory.Deprecation;
import com.konfigyr.artifactory.PropertyMetadata;
import com.konfigyr.artifactory.VersionedArtifact;
import com.konfigyr.artifactory.converter.DeprecationConverter;
import com.konfigyr.artifactory.converter.HintsConverter;
import com.konfigyr.entity.EntityId;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.util.function.ThrowingConsumer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@Slf4j
class ProvenanceEvaluationWriter implements ItemWriter<@NonNull EvaluationResult> {

	private static final Marker MARKER = MarkerFactory.getMarker("PROPERTY_PROVENANCE_WRITER");

	private final DSLContext context;
	private final Converter<String, List<String>> hintsConverter;
	private final Converter<String, Deprecation> deprecationConverter;

	ProvenanceEvaluationWriter(ObjectMapper mapper, DSLContext context) {
		this.context = context;
		this.hintsConverter = new HintsConverter();
		this.deprecationConverter = new DeprecationConverter(mapper);
	}

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
		final VersionedArtifact version = result.version();
		final PropertyMetadata metadata = result.metadata();

		final Long identifier = context.insertInto(PROPERTY_DEFINITIONS)
				.set(PROPERTY_DEFINITIONS.ID, EntityId.generate().orElseThrow().get())
				.set(PROPERTY_DEFINITIONS.ARTIFACT_ID, version.artifact().get())
				.set(PROPERTY_DEFINITIONS.CHECKSUM, provenance.checksum())
				.set(PROPERTY_DEFINITIONS.NAME, metadata.name())
				.set(PROPERTY_DEFINITIONS.TYPE, metadata.type().name())
				.set(PROPERTY_DEFINITIONS.DATA_TYPE, metadata.dataType().name())
				.set(PROPERTY_DEFINITIONS.TYPE_NAME, metadata.typeName())
				.set(PROPERTY_DEFINITIONS.DEFAULT_VALUE, metadata.defaultValue())
				.set(PROPERTY_DEFINITIONS.DESCRIPTION, metadata.typeName())
				.set(PROPERTY_DEFINITIONS.DEPRECATION, deprecationConverter.to(metadata.deprecation()))
				.set(PROPERTY_DEFINITIONS.HINTS, hintsConverter.to(metadata.hints()))
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
				version.format(), metadata.name(), provenance.checksum().encode());
	}

	private void update(EvaluationResult.Unused result) {
		final VersionedArtifact version = result.version();
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
				version.format(), result.metadata().name(), provenance.checksum().encode());
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
