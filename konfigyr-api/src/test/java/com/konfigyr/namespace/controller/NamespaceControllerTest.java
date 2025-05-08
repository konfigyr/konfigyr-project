package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class NamespaceControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should retrieve all available namespaces for logged in John Doe principal")
	void shouldListNamespaces() {
		mvc.get().uri("/namespaces")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Namespace.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(2)
						.extracting(Namespace::slug)
						.containsExactly("john-doe", "konfigyr")
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(0L, PagedModel.PageMetadata::number)
						.returns(2L, PagedModel.PageMetadata::totalElements)
						.returns(1L, PagedModel.PageMetadata::totalPages)
				)
				.satisfies(it -> assertThat(it.getLinks())
						.hasSize(2)
						.containsExactly(
								Link.of("http://localhost?page=1", LinkRelation.FIRST),
								Link.of("http://localhost?page=1", LinkRelation.LAST)
						)
				);
	}

	@Test
	@DisplayName("should retrieve all available namespaces for logged in Jane Doe principal")
	void shouldListNamespace() {
		mvc.get().uri("/namespaces")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Namespace.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.extracting(Namespace::slug)
						.containsExactly("konfigyr")
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(0L, PagedModel.PageMetadata::number)
						.returns(1L, PagedModel.PageMetadata::totalElements)
						.returns(1L, PagedModel.PageMetadata::totalPages)
				)
				.satisfies(it -> assertThat(it.getLinks())
						.hasSize(2)
						.containsExactly(
								Link.of("http://localhost?page=1", LinkRelation.FIRST),
								Link.of("http://localhost?page=1", LinkRelation.LAST)
						)
				);
	}

	@Test
	@DisplayName("should not retrieve namespaces when namespaces:read scope is not present")
	void retrieveNamespacesWithoutScope() {
		mvc.get().uri("/namespaces")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should retrieve namespace by slug")
	void shouldRetrieveNamespace() {
		mvc.get().uri("/namespaces/{slug}", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Namespace.class)
				.returns("konfigyr", Namespace::slug);
	}

	@Test
	@DisplayName("should retrieve unknown namespace by slug")
	void shouldRetrieveUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle(HttpStatus.NOT_FOUND.getReasonPhrase())
						.hasDetailContaining("Could not find a namespace")
				));
	}

	@Test
	@DisplayName("should not retrieve namespace when not a member")
	void retrieveNamespaceForNonMembers() {
		mvc.get().uri("/namespaces/{slug}", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not retrieve namespace when namespaces:read scope is not present")
	void retrieveNamespaceWithoutScope() {
		mvc.get().uri("/namespaces/{slug}", "john-doe")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@Transactional
	@DisplayName("should create namespace")
	void shouldCreateNamespace() {
		mvc.post().uri("/namespaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"arakis\",\"name\":\"Arakis\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CREATED)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Namespace.class)
				.returns("arakis", Namespace::slug)
				.returns("Arakis", Namespace::name)
				.returns(null, Namespace::description)
				.satisfies(it -> assertThat(it.id())
						.isNotNull()
				).satisfies(it -> assertThat(it.avatar())
						.isNotNull()
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to create namespace with invalid data")
	void shouldFailCreateNamespace() {
		mvc.post().uri("/namespaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"\",\"name\":\"    \"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("slug", "slug", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to create namespace with existing slug")
	void shouldFailCreateNamespaceWithExistingSlug() {
		mvc.post().uri("/namespaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr\",\"name\":\"Konfigyr\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle(HttpStatus.BAD_REQUEST.getReasonPhrase())
						.hasDetailContaining("Could not create namespace as one already exists")
				));
	}

	@Test
	@DisplayName("should not update namespace when namespaces:write is not present")
	void createNamespaceWithoutScope() {
		mvc.post().uri("/namespaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"arakis\",\"name\":\"Arakis\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@Transactional
	@DisplayName("should update namespace by slug")
	void shouldUpdateNamespace() {
		mvc.put().uri("/namespaces/{slug}", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr\",\"name\":\"Konfigyr Project\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.OK)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Namespace.class)
				.returns(EntityId.from(2), Namespace::id)
				.returns("konfigyr", Namespace::slug)
				.returns("Konfigyr Project", Namespace::name)
				.returns(null, Namespace::description)
				.satisfies(it -> assertThat(it.avatar())
						.isNotNull()
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to update namespace with invalid data")
	void shouldFailUpdateNamespace() {
		mvc.put().uri("/namespaces/{slug}", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("slug", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to update namespace with existing slug")
	void shouldFailUpdateNamespaceWithExistingSlug() {
		mvc.put().uri("/namespaces/{slug}", "john-doe")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr\",\"name\":\"Konfigyr\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle(HttpStatus.BAD_REQUEST.getReasonPhrase())
						.hasDetailContaining("Could not create namespace as one already exists")
				));
	}

	@Test
	@DisplayName("should update unknown namespace by slug")
	void shouldUpdateUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}", "unknown")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"unknown\",\"name\":\"Unknown\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle(HttpStatus.NOT_FOUND.getReasonPhrase())
						.hasDetailContaining("Could not find a namespace")
				));
	}

	@Test
	@DisplayName("should not update namespace when not a member")
	void updateNamespaceForNonMembers() {
		mvc.put().uri("/namespaces/{slug}", "john-doe")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr\",\"name\":\"Konfigyr Project\"}")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not update namespace when not an administrator")
	void updateNamespaceForNonAdministrators() {
		mvc.put().uri("/namespaces/{slug}", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr\",\"name\":\"Konfigyr Project\"}")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not update namespace when namespaces:write is not present")
	void updateNamespaceWithoutScope() {
		mvc.put().uri("/namespaces/{slug}", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr\",\"name\":\"Konfigyr Project\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@Transactional
	@DisplayName("should delete namespace by slug")
	void shouldDeleteNamespace() {
		mvc.delete().uri("/namespaces/{slug}", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@DisplayName("should delete unknown namespace by slug")
	void shouldDeleteUnknownNamespace() {
		mvc.delete().uri("/namespaces/{slug}", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle(HttpStatus.NOT_FOUND.getReasonPhrase())
						.hasDetailContaining("Could not find a namespace")
				));
	}

	@Test
	@DisplayName("should not delete namespace when not a member")
	void deleteNamespaceForNonMembers() {
		mvc.delete().uri("/namespaces/{slug}", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.DELETE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not delete namespace when not an administrator")
	void deleteNamespaceForNonAdministrators() {
		mvc.delete().uri("/namespaces/{slug}", "konfigyr")
				.with(authentication(TestPrincipals.jane(), OAuthScope.DELETE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not delete namespace when namespaces:delete scope is not present")
	void deleteNamespaceWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.DELETE_NAMESPACES));
	}

}
