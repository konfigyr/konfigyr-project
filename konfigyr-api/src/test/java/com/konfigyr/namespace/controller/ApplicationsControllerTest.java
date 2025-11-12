package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class ApplicationsControllerTest extends AbstractNamespaceControllerTest {

	@Test
	@DisplayName("should retrieve applications for namespace")
	void listApplications() {
		mvc.get().uri("/namespaces/{slug}/applications", "konfigyr")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(NamespaceApplication.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(2)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(2L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns("Konfigyr active app", NamespaceApplication::name)
										.returns("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes),
								app -> assertThat(app)
										.returns(EntityId.from(1L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns("Konfigyr expired app", NamespaceApplication::name)
										.returns("kfg-A2c7mvoxEP1AW1BUqzQXbS3NAivjfAqD", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
						)
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
	@DisplayName("should retrieve applications for namespace that are matching the search term")
	void searchApplications() {
		mvc.get().uri("/namespaces/{slug}/applications", "konfigyr")
				.queryParam("term", "active")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(NamespaceApplication.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(2L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns("Konfigyr active app", NamespaceApplication::name)
										.returns("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
						)
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
	@DisplayName("should retrieve applications for namespace that are currently active")
	void searchActiveApplications() {
		mvc.get().uri("/namespaces/{slug}/applications", "konfigyr")
				.queryParam("active", "true")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(NamespaceApplication.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(2L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns("Konfigyr active app", NamespaceApplication::name)
										.returns("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
						)
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
	@DisplayName("should not retrieve applications from an unknown namespace")
	void retrieveApplicationsFromUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/applications", "unknown-namespace")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve namespace applications when user is not a member of a namespace")
	void retrieveApplicationsWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/applications", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not retrieve namespace applications when namespaces:write scope is not present")
	void retrieveApplicationsWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/applications", "konfigyr")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should retrieve namespace application by entity identifier")
	void retrieveApplication() {
		mvc.get().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(2L), NamespaceApplication::id)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns("Konfigyr active app", NamespaceApplication::name)
				.returns("kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.satisfies(it -> assertThat(it.expiresAt())
						.isCloseTo(OffsetDateTime.now().plusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(7), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should not retrieve an unknown namespace application")
	void retrieveUnknownApplication() {
		mvc.get().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(9999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(applicationNotFound(9999));
	}

	@Test
	@DisplayName("should not retrieve an application for an unknown namespace")
	void retrieveApplicationForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/applications/{id}", "unknown", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not retrieve namespace application that belongs to a different namespace")
	void retrieveApplicationForDifferentNamespace() {
		mvc.get().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(applicationNotFound(3));
	}

	@Test
	@DisplayName("should not retrieve namespace application when namespaces:read scope is not present")
	void retrieveApplicationWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve namespace application when user is not a member of a namespace")
	void retrieveApplicationWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/applications/{id}", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should create namespace OAuth application")
	void createApplication() {
		mvc.post().uri("/namespaces/{slug}/applications", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces:read namespaces:invite\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns("New app", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.READ_NAMESPACES, OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes)
				.returns(null, NamespaceApplication::expiresAt)
				.satisfies(it -> assertThat(it.clientId())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.clientSecret())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@Transactional
	@DisplayName("should create namespace OAuth application with expiration")
	void createApplicationWithExpiration() {
		mvc.post().uri("/namespaces/{slug}/applications", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces:read namespaces:invite\", \"expiresAt\":\"2020-01-01T00:00:00Z\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns("New app", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.READ_NAMESPACES, OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes)
				.returns(null, NamespaceApplication::expiresAt)
				.satisfies(it -> assertThat(it.clientId())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.clientSecret())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to create namespace application with invalid data")
	void createApplicationInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/applications", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
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
								.containsExactlyInAnyOrder("scopes", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to create application for unknown namespace")
	void createApplicationUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/applications", "unknown-namespace")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to create namespace application when namespaces:write scope is not present")
	void createApplicationWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/applications", "konfigyr")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to create namespace application when user is not a member of a namespace")
	void createApplicationWithoutMembership() {
		mvc.post().uri("/namespaces/{slug}/applications", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should update namespace OAuth application")
	void updateApplication() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"removed expiry\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(1L), NamespaceApplication::id)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns("removed expiry", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.returns("kfg-A2c7mvoxEP1AW1BUqzQXbS3NAivjfAqD", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(null, NamespaceApplication::expiresAt)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(30), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@Transactional
	@DisplayName("should update namespace OAuth application with expiration")
	void updateApplicationWithExpiration() {
		final var expiry = OffsetDateTime.now().plusDays(2);

		mvc.put().uri("/namespaces/{slug}/applications/{id}", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Updated\", \"scopes\":\"namespaces:invite\", \"expiration\":\"" + expiry + "\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(3L), NamespaceApplication::id)
				.returns(EntityId.from(1L), NamespaceApplication::namespace)
				.returns("Updated", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes)
				.returns("kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::clientSecret)
				.satisfies(it -> assertThat(it.expiresAt())
						.isCloseTo(expiry, within(1, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to update namespace application with invalid data")
	void updateApplicationInvalidPayload() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
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
								.containsExactlyInAnyOrder("scopes", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to update application for unknown namespace")
	void updateApplicationUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "unknown-namespace", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to update an application that belongs to a different namespace")
	void updateApplicationFromDifferentNamespace() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(applicationNotFound(3));
	}

	@Test
	@DisplayName("should fail to update namespace application when namespaces:write scope is not present")
	void updateApplicationWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to update namespace application when user is not a member of a namespace")
	void updateApplicationWithoutMembership() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\", \"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should reset namespace OAuth application by generating a new client secret")
	void resetApplication() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}/reset", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(3L), NamespaceApplication::id)
				.returns(EntityId.from(1L), NamespaceApplication::namespace)
				.returns("Personal app", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.returns("kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::expiresAt)
				.satisfies(it -> assertThat(it.clientSecret())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to reset application for unknown namespace")
	void resetApplicationUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}/reset", "unknown-namespace", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to reset an application that belongs to a different namespace")
	void resetApplicationFromDifferentNamespace() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}/reset", "konfigyr", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(applicationNotFound(3));
	}

	@Test
	@DisplayName("should fail to reset namespace application when namespaces:write scope is not present")
	void resetApplicationWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}/reset", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to reset namespace application when user is not a member of a namespace")
	void resetApplicationWithoutMembership() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}/reset", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should delete namespace application")
	void deleteApplication() {
		mvc.delete().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@DisplayName("should fail to delete a application for an unknown namespace")
	void deleteApplicationForUnknownNamespace() {
		mvc.delete().uri("/namespaces/{slug}/applications/{id}", "unknown", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to delete an unknown namespace application")
	void deleteUnknownApplication() {
		mvc.delete().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(99999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(applicationNotFound(99999));
	}

	@Test
	@DisplayName("should fail to delete an application that belongs to a different namespace")
	void deleteApplicationFromDifferentNamespace() {
		mvc.delete().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(applicationNotFound(3));
	}

	@Test
	@DisplayName("should fail to delete namespace application when namespaces:write scope is not present")
	void deleteApplicationWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to delete namespace application when user is not a member of a namespace")
	void deleteApplicationWithoutMembership() {
		mvc.delete().uri("/namespaces/{slug}/applications/{id}", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	static Consumer<MvcTestResult> applicationNotFound(long id) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("OAuth application not found")
				.hasDetailContaining("We couldn't find an OAuth application matching your request.")
		).andThen(hasFailedWithException(NamespaceApplicationNotFoundException.class, ex -> ex
				.hasMessageContaining("Could not find a namespace application with the following identifier: %s", EntityId.from(id).serialize())
		));
	}

}
