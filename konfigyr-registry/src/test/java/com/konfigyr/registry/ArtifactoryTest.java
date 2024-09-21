package com.konfigyr.registry;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = RegistryTestConfiguration.class)
class ArtifactoryTest {

	@Autowired
	Artifactory artifactory;

	@Test
	@DisplayName("should search for all repositories")
	void shouldSearchForAllRepositories() {
		final SearchQuery query = ArtifactorySearchQuery.of(Pageable.ofSize(2));

		assertThatObject(artifactory.searchRepositories(query))
				.returns(2, Page::getSize)
				.returns(2, Page::getNumberOfElements)
				.returns(3L, Page::getTotalElements)
				.returns(2, Page::getTotalPages)
				.asInstanceOf(InstanceOfAssertFactories.iterable(Repository.class))
				.extracting(Repository::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(2));
	}

	@Test
	@DisplayName("should search repositories with a matching name")
	void shouldSearchForRepositoriesByTerm() {
		final SearchQuery query = ArtifactorySearchQuery.of("Website", null, Pageable.ofSize(20));

		assertThatObject(artifactory.searchRepositories(query))
				.returns(20, Page::getSize)
				.returns(2, Page::getNumberOfElements)
				.returns(2L, Page::getTotalElements)
				.returns(1, Page::getTotalPages)
				.asInstanceOf(InstanceOfAssertFactories.iterable(Repository.class))
				.extracting(Repository::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(3));
	}

	@Test
	@DisplayName("should search repositories within a namespace")
	void shouldSearchForRepositoriesByNamespace() {
		final SearchQuery query = ArtifactorySearchQuery.of(null, "konfigyr", Pageable.ofSize(20));

		assertThatObject(artifactory.searchRepositories(query))
				.returns(20, Page::getSize)
				.returns(2, Page::getNumberOfElements)
				.returns(2L, Page::getTotalElements)
				.returns(1, Page::getTotalPages)
				.asInstanceOf(InstanceOfAssertFactories.iterable(Repository.class))
				.extracting(Repository::id)
				.containsExactlyInAnyOrder(EntityId.from(2), EntityId.from(3));
	}

	@Test
	@DisplayName("should search repositories within a namespace and search term")
	void shouldSearchForRepositoriesByNamespaceAndTerm() {
		final SearchQuery query = ArtifactorySearchQuery.of("crypto", "konfigyr", Pageable.ofSize(20));

		assertThatObject(artifactory.searchRepositories(query))
				.returns(20, Page::getSize)
				.returns(1, Page::getNumberOfElements)
				.returns(1L, Page::getTotalElements)
				.returns(1, Page::getTotalPages)
				.asInstanceOf(InstanceOfAssertFactories.iterable(Repository.class))
				.extracting(Repository::id)
				.containsExactlyInAnyOrder(EntityId.from(2));
	}

	@Test
	@DisplayName("should lookup repository by entity identifier")
	void shouldLookupRepositoryById() {
		final var id = EntityId.from(1);

		assertThat(artifactory.findRepositoryById(id))
				.isPresent()
				.get()
				.returns(id, Repository::id)
				.returns("website", Repository::slug)
				.returns("john-doe", Repository::namespace)
				.returns("Johns Website", Repository::name)
				.returns("Personal website", Repository::description)
				.returns(true, Repository::isPrivate)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should lookup repository by namespace and repository slugs")
	void shouldLookupRepositoryBySlugs() {
		final var namespace = "konfigyr";
		final var slug = "konfigyr-crypto-api";

		assertThat(artifactory.findRepositoryBySlug(namespace, slug))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Repository::id)
				.returns(slug, Repository::slug)
				.returns(namespace, Repository::namespace)
				.returns("Konfigyr Crypto API", Repository::name)
				.returns("Spring Boot Crypto API library", Repository::description)
				.returns(false, Repository::isPrivate)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should lookup repository by namespace identifier and repository slug")
	void shouldLookupRepositoryBySlug() {
		final var namespace = EntityId.from(2);
		final var slug = "website";

		assertThat(artifactory.findRepositoryBySlug(namespace, slug))
				.isPresent()
				.get()
				.returns(EntityId.from(3), Repository::id)
				.returns(slug, Repository::slug)
				.returns("konfigyr", Repository::namespace)
				.returns("Konfigyr Website", Repository::name)
				.returns("Konfigyr Landing page", Repository::description)
				.returns(false, Repository::isPrivate)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should return empty repository search results when namespace is unknown")
	void shouldFailToSearchRepositoriesByUnknownNamespace() {
		final SearchQuery query = ArtifactorySearchQuery.of(null, "unknown", Pageable.ofSize(20));

		assertThat(artifactory.searchRepositories(query)).isEmpty();
	}

	@Test
	@DisplayName("should return empty repository search results when name does not match")
	void shouldFailToSearchRepositoriesByNonMatchingTerm() {
		final SearchQuery query = ArtifactorySearchQuery.of("unknown", null, Pageable.ofSize(20));

		assertThat(artifactory.searchRepositories(query)).isEmpty();
	}

	@Test
	@DisplayName("should return empty repository search results when name matches but not the namespace")
	void shouldFailToSearchRepositoriesByMatchingTermForDifferentNamespace() {
		final SearchQuery query = ArtifactorySearchQuery.of("crypto", "john-doe", Pageable.ofSize(20));

		assertThat(artifactory.searchRepositories(query)).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when repository is not found by entity identifier")
	void shouldFailToLookupRepositoryById() {
		assertThat(artifactory.findRepositoryById(EntityId.from(182648155))).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when repository does not exist")
	void shouldFailToLookupRepositoryBySlug() {
		assertThat(artifactory.findRepositoryBySlug("john-doe", "unknown-repository")).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when repository is not owned by namespace")
	void shouldFailToLookupRepositoryWhenNotOwnedByNamespace() {
		assertThat(artifactory.findRepositoryBySlug("john-doe", "konfigyr-crypto-api")).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when namespace does not exist")
	void shouldFailToLookupRepositoryByUnknownNamespace() {
		assertThat(artifactory.findRepositoryBySlug(EntityId.from(18562423), "konfigyr-crypto-api")).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when namespace and repository do not exist")
	void shouldFailToLookupRepositoryByUnknownNamespaceAndRepository() {
		assertThat(artifactory.findRepositoryBySlug(EntityId.from(18562423), "unknown-repository")).isEmpty();
	}


}