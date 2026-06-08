package com.konfigyr.mcp;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.PropertyDefinition;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * MCP tool that exposes Konfigyr Artifactory property search to AI agents.
 * <p>
 * The tool supports two search modes that are resolved from the parameters supplied by the caller:
 *
 * <ol>
 *   <li>
 *       <b>Service-scoped search</b>: the highest-relevance mode. When a {@code service} slug is
 *       provided, the search is restricted to the configuration catalog materialized for that
 *       service's current artifact manifest within the authenticated namespace. Delegates to
 *       {@link Services#search(Service, SearchQuery)}.
 *   </li>
 *   <li>
 *       <b>Artifact-scoped search</b>: when a {@code artifactCoordinates} string in the form
 *       {@code groupId:artifactId:version} is provided, the search is restricted to property
 *       definitions contributed by that specific artifact version. Delegates to
 *       {@link Artifactory#search(SearchQuery)}.
 *   </li>
 * </ol>
 *
 * <p>The target namespace is always derived from the authenticated OAuth2 client principal via
 * {@link NamespaceContext}, it is never an explicit parameter.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see McpAutoConfiguration
 * @see PropertyDefinition#ARTIFACT_CRITERIA
 * @see PropertyDefinition#INCLUDE_DEPRECATED_CRITERIA
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ArtifactoryTools {

	static final int MAX_LIMIT = 100;

	private final NamespaceContext namespaceContext;
	private final Services services;
	private final Artifactory artifactory;

	/**
	 * Searches for Spring Boot configuration property descriptors relevant to the given query.
	 * <p>
	 * Exactly one of {@code service} or {@code artifactCoordinates} must be provided. Providing
	 * both is supported; {@code service} takes precedence. Providing neither is rejected with a
	 * validation error.
	 *
	 * @param query             natural-language or keyword search term, required and must not be blank
	 * @param service           slug of the Konfigyr service to scope the search to; when present,
	 *                          the search is restricted to that service's configuration catalog
	 * @param artifacts         Maven coordinates in the form {@code groupId:artifactId:version};
	 *                          when present and {@code service} is absent, the search is restricted
	 *                          to properties contributed by that artifact version
	 * @param includeDeprecated when {@code true}, deprecated properties are included in the results
	 * @param limit             maximum number of results; capped at {@value #MAX_LIMIT}
	 * @return list of matching property descriptors, never {@literal null}
	 */
	@Tool(description = """
			Search for Spring Boot configuration property descriptors in the authenticated namespace.

			Use 'service' (a Konfigyr service slug) for the highest relevance — it searches that \
			service's exact dependency catalog. Use 'artifactCoordinates' \
			(groupId:artifactId:version) when you know the specific library but not the service. \
			Exactly one of these must be provided.

			Deprecated properties are excluded by default; set 'includeDeprecated' to true to include them. \
			Results are capped at 'limit' entries (maximum 100). \
			Examples of effective queries: 'datasource timeout', 'kafka ssl', 'actuator security'.\
			""")
	public @NonNull List<PropertyDescriptor> searchProperties(
			@ToolParam(description = "Natural language or keyword search query, e.g. 'datasource timeout' or 'kafka retry'")
			@NonNull String query,
			@ToolParam(description = "Konfigyr service slug to restrict the search to that service's catalog (preferred mode)", required = false)
			@Nullable String service,
			@ToolParam(description = "Maven artifact coordinates in the form groupId:artifactId:version to restrict the search to a specific artifact version", required = false)
			@Nullable String artifacts,
			@ToolParam(description = "Whether to include deprecated properties in the results; defaults to false")
			boolean includeDeprecated,
			@ToolParam(description = "Maximum number of results to return; values above 100 are capped to 100")
			int limit
	) {
		final Namespace namespace = namespaceContext.resolve();

		Assert.hasText(query, "Search query must not be blank");
		Assert.isTrue(limit > 0, "Limit must be a positive integer");

		final int effectiveLimit = Math.min(limit, MAX_LIMIT);

		if (StringUtils.hasText(service)) {
			return searchByService(namespace, service.trim(), query.trim(), includeDeprecated, effectiveLimit);
		}

		if (StringUtils.hasText(artifacts)) {
			return searchByArtifact(artifacts.trim(), query.trim(), includeDeprecated, effectiveLimit);
		}

		throw new IllegalArgumentException(
				"Either 'service' or 'artifactCoordinates' must be provided. " +
				"Global search is not supported in this version."
		);
	}

	private List<PropertyDescriptor> searchByService(
			Namespace namespace,
			String serviceSlug,
			String term,
			boolean includeDeprecated,
			int limit
	) {
		final Service service = services.get(namespace, serviceSlug)
				.orElseThrow(() -> new IllegalArgumentException(
						"No service found with slug '%s' in namespace '%s'".formatted(serviceSlug, namespace.slug())
				));

		final SearchQuery searchQuery = SearchQuery.builder()
				.term(term)
				.criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA, includeDeprecated)
				.pageable(PageRequest.of(0, limit))
				.build();

		final List<PropertyDescriptor> results = services.search(service, searchQuery).getContent();

		log.debug("searchProperties: mode=SERVICE service='{}' term='{}' includeDeprecated={} results={}",
				serviceSlug, term, includeDeprecated, results.size());

		return results;
	}

	private List<PropertyDescriptor> searchByArtifact(
			String coordinatesString,
			String term,
			boolean includeDeprecated,
			int limit
	) {
		final ArtifactCoordinates coordinates = ArtifactCoordinates.parse(coordinatesString);

		final SearchQuery searchQuery = SearchQuery.builder()
				.term(term)
				.criteria(PropertyDefinition.ARTIFACT_CRITERIA, coordinates)
				.criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA, includeDeprecated)
				.pageable(PageRequest.of(0, limit))
				.build();

		final List<PropertyDescriptor> results = artifactory.search(searchQuery)
				.map(d -> (PropertyDescriptor) d)
				.getContent();

		log.debug("searchProperties: mode=ARTIFACT coordinates='{}' term='{}' includeDeprecated={} results={}",
				coordinatesString, term, includeDeprecated, results.size());

		return results;
	}

}
