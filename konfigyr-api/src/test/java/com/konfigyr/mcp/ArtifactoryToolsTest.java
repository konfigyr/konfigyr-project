package com.konfigyr.mcp;

import com.konfigyr.artifactory.*;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.version.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactoryToolsTest {

	@Mock(strictness = Mock.Strictness.LENIENT)
	NamespaceContext namespaceContext;

	@Mock
	Services services;

	@Mock
	Artifactory artifactory;

	@Mock
	Namespace namespace;

	@Mock
	Service service;

	ArtifactoryTools tool;

	@BeforeEach
	void setup() {
		tool = new ArtifactoryTools(namespaceContext, services, artifactory);
		when(namespaceContext.resolve()).thenReturn(namespace);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	// -------------------------------------------------------------------------
	// Service-scoped search
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("service search delegates to Services.get and Services.search with correct query")
	void serviceSearchDelegatesCorrectly() {
		final PropertyDefinition definition = propertyDefinition("spring.datasource.url");

		when(services.get(namespace, "my-service")).thenReturn(Optional.of(service));
		when(services.search(eq(service), any(SearchQuery.class)))
				.thenReturn(new PageImpl<>(List.of(definition)));

		final List<PropertyDescriptor> results = tool.searchProperties("datasource", "my-service", null, false, 10);

		assertThat(results)
				.hasSize(1)
				.extracting(PropertyDescriptor::name)
				.containsExactly("spring.datasource.url");

		final ArgumentCaptor<SearchQuery> queryCaptor = ArgumentCaptor.forClass(SearchQuery.class);
		verify(services).search(eq(service), queryCaptor.capture());

		final SearchQuery captured = queryCaptor.getValue();
		assertThat(captured.term()).contains("datasource");
		assertThat(captured.criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA)).contains(false);
		assertThat(captured.pageable().getPageSize()).isEqualTo(10);

		verifyNoInteractions(artifactory);
	}

	@Test
	@DisplayName("service search trims whitespace from service slug and query")
	void serviceSearchTrimsInputs() {
		when(services.get(namespace, "my-service")).thenReturn(Optional.of(service));
		when(services.search(eq(service), any(SearchQuery.class)))
				.thenReturn(new PageImpl<>(List.of()));

		tool.searchProperties("  datasource  ", "  my-service  ", null, false, 5);

		final ArgumentCaptor<SearchQuery> queryCaptor = ArgumentCaptor.forClass(SearchQuery.class);
		verify(services).get(namespace, "my-service");
		verify(services).search(eq(service), queryCaptor.capture());
		assertThat(queryCaptor.getValue().term()).contains("datasource");
	}

	@Test
	@DisplayName("service search includes deprecated properties when requested")
	void serviceSearchIncludesDeprecated() {
		when(services.get(namespace, "svc")).thenReturn(Optional.of(service));
		when(services.search(eq(service), any(SearchQuery.class)))
				.thenReturn(new PageImpl<>(List.of()));

		tool.searchProperties("kafka", "svc", null, true, 10);

		final ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
		verify(services).search(eq(service), captor.capture());
		assertThat(captor.getValue().criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA)).contains(true);
	}

	@Test
	@DisplayName("service search prefers service over artifact coordinates when both are provided")
	void serviceSearchTakesPrecedenceOverArtifact() {
		when(services.get(namespace, "svc")).thenReturn(Optional.of(service));
		when(services.search(eq(service), any(SearchQuery.class)))
				.thenReturn(new PageImpl<>(List.of()));

		tool.searchProperties("redis", "svc", "org.springframework.data:spring-data-redis:3.2.0", false, 20);

		verify(services).search(eq(service), any());
		verifyNoInteractions(artifactory);
	}

	@Test
	@DisplayName("service search throws IllegalArgumentException when the service slug is not found")
	void serviceSearchThrowsWhenServiceNotFound() {
		when(namespace.slug()).thenReturn("test-namespace");
		when(services.get(namespace, "unknown-svc")).thenReturn(Optional.empty());

		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("redis", "unknown-svc", null, false, 10))
				.withMessageContaining("unknown-svc")
				.withMessageContaining("test-namespace");
	}

	// -------------------------------------------------------------------------
	// Artifact-scoped search
	// -------------------------------------------------------------------------

	@Test
	@DisplayName("artifact search delegates to Artifactory.search with parsed coordinates in query")
	void artifactSearchDelegatesCorrectly() {
		final PropertyDefinition definition = propertyDefinition("spring.kafka.bootstrap-servers");

		when(artifactory.search(any(SearchQuery.class)))
				.thenReturn(new PageImpl<>(List.of(definition)));

		final List<PropertyDescriptor> results = tool.searchProperties(
				"kafka", null, "org.springframework.kafka:spring-kafka:3.2.0", false, 25);

		assertThat(results).hasSize(1)
				.extracting(PropertyDescriptor::name)
				.containsExactly("spring.kafka.bootstrap-servers");

		final ArgumentCaptor<SearchQuery> queryCaptor = ArgumentCaptor.forClass(SearchQuery.class);
		verify(artifactory).search(queryCaptor.capture());

		final SearchQuery captured = queryCaptor.getValue();
		assertThat(captured.term()).contains("kafka");
		assertThat(captured.pageable().getPageSize()).isEqualTo(25);
		assertThat(captured.criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA)).contains(false);

		final ArtifactCoordinates coords = captured.criteria(PropertyDefinition.ARTIFACT_CRITERIA).orElseThrow();
		assertThat(coords.groupId()).isEqualTo("org.springframework.kafka");
		assertThat(coords.artifactId()).isEqualTo("spring-kafka");
		assertThat(coords.version().get()).isEqualTo("3.2.0");

		verifyNoInteractions(services);
	}

	@Test
	@DisplayName("artifact search includes deprecated properties when requested")
	void artifactSearchIncludesDeprecated() {
		when(artifactory.search(any(SearchQuery.class))).thenReturn(new PageImpl<>(List.of()));

		tool.searchProperties("ssl", null,
				"org.springframework.boot:spring-boot-autoconfigure:3.3.0", true, 10);

		final ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
		verify(artifactory).search(captor.capture());
		assertThat(captor.getValue().criteria(PropertyDefinition.INCLUDE_DEPRECATED_CRITERIA)).contains(true);
	}

	@Test
	@DisplayName("artifact search throws IllegalArgumentException for malformed coordinates")
	void artifactSearchThrowsForInvalidCoordinates() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("kafka", null, "not-valid-coords", false, 10));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("kafka", null, "group:artifact", false, 10));

		verifyNoInteractions(artifactory);
		verifyNoInteractions(services);
	}

	@Test
	@DisplayName("limit parameter must be capped at 100")
	void limitIsCappedToMaximum() {
		when(services.get(namespace, "svc")).thenReturn(Optional.of(service));
		when(services.search(eq(service), any(SearchQuery.class))).thenReturn(new PageImpl<>(List.of()));

		tool.searchProperties("redis", "svc", null, false, 999);

		final ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
		verify(services).search(eq(service), captor.capture());
		assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(ArtifactoryTools.MAX_LIMIT);
	}

	@Test
	@DisplayName("blank query is rejected with IllegalArgumentException")
	void blankQueryIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("", null, null, false, 10))
				.withMessageContaining("Search query must not be blank");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("   ", null, null, false, 10))
				.withMessageContaining("Search query must not be blank");

		verifyNoInteractions(services, artifactory);
	}

	@Test
	@DisplayName("non-positive limit is rejected with IllegalArgumentException")
	void nonPositiveLimitIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("kafka", "svc", null, false, 0))
				.withMessageContaining("Limit must be a positive integer");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("kafka", "svc", null, false, -1))
				.withMessageContaining("Limit must be a positive integer");

		verifyNoInteractions(services, artifactory);
	}

	@Test
	@DisplayName("providing neither service nor artifact coordinates is rejected")
	void neitherServiceNorCoordinatesIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("kafka", null, null, false, 10))
				.withMessageContaining("Either 'service' or 'artifactCoordinates' must be provided.");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> tool.searchProperties("kafka", "  ", "  ", false, 10))
				.withMessageContaining("Either 'service' or 'artifactCoordinates' must be provided.");

		verifyNoInteractions(services, artifactory);
	}

	@Test
	@DisplayName("namespace resolution failure propagates to the caller")
	void namespaceResolutionFailurePropagates() {
		doThrow(new AccessDeniedException("not authenticated")).when(namespaceContext).resolve();

		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> tool.searchProperties("kafka", "svc", null, false, 10));
	}

	private static PropertyDefinition propertyDefinition(String name) {
		return PropertyDefinition.builder()
				.id(1L)
				.artifact(2L)
				.name(name)
				.typeName("java.lang.String")
				.schema(StringSchema.builder().build())
				.checksum(ByteArray.fromString("checksum"))
				.firstSeen(Version.of("3.2.0"))
				.lastSeen(Version.of("3.2.0"))
				.build();
	}

}
