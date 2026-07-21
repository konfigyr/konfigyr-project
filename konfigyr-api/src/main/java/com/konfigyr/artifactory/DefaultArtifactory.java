package com.konfigyr.artifactory;

import com.konfigyr.data.PageableExecutor;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Tokenizer;
import com.konfigyr.support.Tokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;

@Slf4j
@RequiredArgsConstructor
class DefaultArtifactory implements Artifactory {

	/**
	 * Splits a search term into words the same way the {@code property_definitions} search vector trigger
	 * normalizes {@code name} (see {@code artifactory-1.0.0.xml}), any run of non-alphanumeric characters,
	 * so a dotted/hyphenated term like {@code spring.datasource.url} is treated as three separate words.
	 */
	private static final Tokenizer TERM_TOKENIZER = Tokenizer.alphanumeric();

	private static final PageableExecutor propertySearchPageableExecutor = PageableExecutor.builder()
			.defaultSortField(PROPERTY_DEFINITIONS.NAME.asc())
			.sortField("name", PROPERTY_DEFINITIONS.NAME)
			.build();

	private final ArtifactoryQueries queries;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-artifact-definition")
	public Optional<ArtifactDefinition> get(@NonNull ArtifactKey key) {
		return queries.definition(toCondition(key));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-visible-artifact-definition")
	public Optional<ArtifactDefinition> get(@Nullable Owner owner, @NonNull ArtifactKey key) {
		return queries.definition(DSL.and(visibilityCondition(owner), toCondition(key)));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.artifact-key-exists")
	public boolean exists(@NonNull ArtifactKey key) {
		return queries.context().fetchExists(ARTIFACTS, toCondition(key));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.visible-artifact-key-exists")
	public boolean exists(@Nullable Owner owner, @NonNull ArtifactKey key) {
		return queries.context().fetchExists(ARTIFACTS, DSL.and(toCondition(key), visibilityCondition(owner)));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-versioned-artifact")
	public Optional<VersionedArtifact> get(@NonNull ArtifactCoordinates coordinates) {
		return queries.version(toCondition(coordinates));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.get-visible-versioned-artifact")
	public Optional<VersionedArtifact> get(@Nullable Owner owner, @NonNull ArtifactCoordinates coordinates) {
		return queries.version(DSL.and(toCondition(coordinates), visibilityCondition(owner)));
	}

	@Override
	@Transactional(readOnly = true, label = "artifactory.coordinates-exists")
	public boolean exists(@NonNull ArtifactCoordinates coordinates) {
		return queries.context().fetchExists(
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
		return queries.context().fetchExists(
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

		return queries.createVersionedArtifactQuery()
				.where(DSL.and(
						DSL.row(ARTIFACTS.GROUP_ID, ARTIFACTS.ARTIFACT_ID, ARTIFACT_VERSIONS.VERSION)
								.in(DSL.select(DSL.field("g", String.class), DSL.field("a", String.class), DSL.field("v", String.class))
										.from("unnest({0}::text[], {1}::text[], {2}::text[]) AS t(g, a, v)",
												groupIds, artifactIds, versions)),
						visibilityCondition(owner)
				))
				.fetchSet(ArtifactoryQueries::toVersionedArtifact);
	}

	@NonNull
	@Override
	@SuppressWarnings("deprecation")
	@Transactional(readOnly = true, label = "artifactory.search-properties")
	public Page<PropertyDefinition> search(@Nullable Owner owner, @NonNull SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(visibilityCondition(owner));
		query.criteria(ArtifactKey.GROUP_ID_CRITERIA)
				.map(ARTIFACTS.GROUP_ID::equalIgnoreCase)
				.ifPresent(conditions::add);
		query.criteria(ArtifactKey.ARTIFACT_ID_CRITERIA)
				.map(ARTIFACTS.ARTIFACT_ID::equalIgnoreCase)
				.ifPresent(conditions::add);
		query.criteria(ArtifactCoordinates.VERSION_CRITERIA)
				.map(version -> DSL.exists(
						DSL.select(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID)
								.from(ARTIFACT_VERSION_PROPERTIES)
								.innerJoin(ARTIFACT_VERSIONS)
								.on(ARTIFACT_VERSIONS.ID.eq(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID))
								.where(
										ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID.eq(PROPERTY_DEFINITIONS.ID),
										ARTIFACT_VERSIONS.VERSION.equalIgnoreCase(version)
								)
				))
				.ifPresent(conditions::add);

		final String rankSearchTerm = query.term(TERM_TOKENIZER)
				.map(DefaultArtifactory::toPrefixTsQuery)
				.filter(StringUtils::hasText)
				.orElse(null);

		return queries.properties(
				propertySearchPageableExecutor.rankBy(rankSearchTerm, PROPERTY_DEFINITIONS.SEARCH_VECTOR),
				DSL.and(conditions), query.pageable()
		);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "artifactory.version-properties")
	public List<PropertyDefinition> properties(@NonNull ArtifactCoordinates coordinates) {
		final Long artifactVersionId = queries.context().select(ARTIFACT_VERSIONS.ID)
				.from(ARTIFACT_VERSIONS)
				.innerJoin(ARTIFACTS)
				.on(ARTIFACTS.ID.eq(ARTIFACT_VERSIONS.ARTIFACT_ID))
				.where(toCondition(coordinates))
				.fetchOptional(ARTIFACT_VERSIONS.ID)
				.orElseThrow(() -> new ArtifactVersionNotFoundException(coordinates));

		return queries.createPropertySearchQuery()
				.innerJoin(ARTIFACT_VERSION_PROPERTIES)
				.on(PROPERTY_DEFINITIONS.ID.eq(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID))
				.where(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID.eq(artifactVersionId))
				.fetch(record -> ArtifactoryQueries.toPropertyDefinition(record, queries.converters()));
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

	/**
	 * Builds a {@code tsquery} expression from a search term already split into words by {@link #TERM_TOKENIZER}
	 * (the same normalization the search vector trigger applies to {@code name}), requiring every word to be
	 * present as a prefix ({@code word:*}), so a partial term like {@code spring.appl} matches a property
	 * named {@code spring.application.name}, and a full dotted name matches by requiring each of its segments
	 * as its own word.
	 *
	 * @param words the search term, already split into words, can't be {@literal null}
	 * @return the {@code tsquery} expression text, or blank if {@code words} has no alphanumeric words
	 */
	@NonNull
	static String toPrefixTsQuery(Tokens words) {
		return words.filter(StringUtils::hasText)
				.map(word -> word + ":*")
				.join(" & ");
	}

}
