package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.*;
import com.konfigyr.security.*;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class ApplicationsControllerTest extends AbstractControllerTest {

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
						.hasSize(4)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(2L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
										.returns("Konfigyr active app", NamespaceApplication::name)
										.returns("kfg-AQEAAAAAAAAAAgAAAABqJTnoOdowT1b42n8N2q5ZQBo", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes),
								app -> assertThat(app)
										.returns(EntityId.from(5L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns(NamespaceClientType.AGENT, NamespaceApplication::type)
										.returns("Konfigyr agent app", NamespaceApplication::name)
										.returns("kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes),
								app -> assertThat(app)
										.returns(EntityId.from(1L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns("Konfigyr expired app", NamespaceApplication::name)
										.returns("kfg-AQEAAAAAAAAAAgAAAABqJToWfXkWbVML9iZbEPVai4o", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes),
								app -> assertThat(app)
										.returns(EntityId.from(6L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns(NamespaceClientType.WORKLOAD, NamespaceApplication::type)
										.returns("Konfigyr workload app", NamespaceApplication::name)
										.returns("kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.PUBLISH_RELEASES), NamespaceApplication::scopes)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(1L, PagedModel.PageMetadata::number)
						.returns(4L, PagedModel.PageMetadata::totalElements)
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
										.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
										.returns("Konfigyr active app", NamespaceApplication::name)
										.returns("kfg-AQEAAAAAAAAAAgAAAABqJTnoOdowT1b42n8N2q5ZQBo", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(1L, PagedModel.PageMetadata::number)
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
	@DisplayName("should retrieve workload applications for namespace")
	void searchWorkloadApplications() {
		mvc.get().uri("/namespaces/{slug}/applications", "konfigyr")
				.queryParam("type", NamespaceClientType.WORKLOAD.name())
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
										.returns(EntityId.from(6L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns(NamespaceClientType.WORKLOAD, NamespaceApplication::type)
										.returns("Konfigyr workload app", NamespaceApplication::name)
										.returns("kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.PUBLISH_RELEASES), NamespaceApplication::scopes)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(1L, PagedModel.PageMetadata::number)
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
						.hasSize(3)
						.satisfiesExactly(
								app -> assertThat(app)
										.returns(EntityId.from(6L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns(NamespaceClientType.WORKLOAD, NamespaceApplication::type)
										.returns("Konfigyr workload app", NamespaceApplication::name)
										.returns("kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.PUBLISH_RELEASES), NamespaceApplication::scopes),
								app -> assertThat(app)
										.returns(EntityId.from(2L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns("Konfigyr active app", NamespaceApplication::name)
										.returns("kfg-AQEAAAAAAAAAAgAAAABqJTnoOdowT1b42n8N2q5ZQBo", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes),
								app -> assertThat(app)
										.returns(EntityId.from(5L), NamespaceApplication::id)
										.returns(EntityId.from(2L), NamespaceApplication::namespace)
										.returns(NamespaceClientType.AGENT, NamespaceApplication::type)
										.returns("Konfigyr agent app", NamespaceApplication::name)
										.returns("kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY", NamespaceApplication::clientId)
										.returns(null, NamespaceApplication::clientSecret)
										.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(1L, PagedModel.PageMetadata::number)
						.returns(3L, PagedModel.PageMetadata::totalElements)
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
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns("Konfigyr active app", NamespaceApplication::name)
				.returns("kfg-AQEAAAAAAAAAAgAAAABqJTnoOdowT1b42n8N2q5ZQBo", NamespaceApplication::clientId)
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
				.content("{\"name\":\"Agent app\",\"type\":\"AGENT\",\"scopes\":\"namespaces:read namespaces:invite\",\"settings\":{\"type\":\"agent\",\"redirectUris\":[\"https://127.0.0.1/callback\"]}}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns("Agent app", NamespaceApplication::name)
				.returns(NamespaceClientType.AGENT, NamespaceApplication::type)
				.returns(OAuthScopes.of(OAuthScope.READ_NAMESPACES, OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes)
				.returns(null, NamespaceApplication::expiresAt)
				.satisfies(it -> assertThat(it.clientId())
						.isNotBlank()
				)
				.returns(null, NamespaceApplication::clientSecret)
				.satisfies(it -> assertThat(it.settings())
						.asInstanceOf(InstanceOfAssertFactories.type(NamespaceApplicationSettings.AgentSettings.class))
						.returns(List.of("https://127.0.0.1/callback"), NamespaceApplicationSettings.AgentSettings::redirectUris)
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
	@DisplayName("should create namespace OAuth application with expiresAt")
	void createApplicationWithExpiration() {
		OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

		mvc.post().uri("/namespaces/{slug}/applications", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\",\"type\":\"SERVICE_ACCOUNT\",\"scopes\":\"namespaces:read namespaces:invite\",\"expiresAt\":\"" + expiresAt + "\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns("New app", NamespaceApplication::name)
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns(OAuthScopes.of(OAuthScope.READ_NAMESPACES, OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes)
				.satisfies(it -> assertThat(it.expiresAt())
						.isNotNull()
						.isCloseTo(expiresAt, within(1, ChronoUnit.SECONDS))
				)
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
								.containsExactlyInAnyOrder("scopes", "name", "type")
						)
				));
	}

	@Test
	@DisplayName("should fail to create application for unknown namespace")
	void createApplicationUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/applications", "unknown-namespace")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\",\"type\":\"AGENT\",\"scopes\":\"namespaces\"}")
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
				.content("{\"name\":\"New app\",\"type\":\"WORKLOAD\",\"scopes\":\"namespaces\"}")
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
				.content("{\"name\":\"New app\",\"type\":\"SERVICE_ACCOUNT\",\"scopes\":\"namespaces\"}")
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
				.content("{\"name\":\"removed expiry\",\"type\":\"WORKLOAD\",\"scopes\":\"namespaces\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(1L), NamespaceApplication::id)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns("removed expiry", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.returns("kfg-AQEAAAAAAAAAAgAAAABqJToWfXkWbVML9iZbEPVai4o", NamespaceApplication::clientId)
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
	@DisplayName("should update namespace OAuth application with expiresAt")
	void updateApplicationWithExpiration() {
		final var expiry = OffsetDateTime.now().plusDays(2);

		mvc.put().uri("/namespaces/{slug}/applications/{id}", "john-doe", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Updated\",\"type\":\"AGENT\",\"scopes\":\"namespaces:invite\", \"expiresAt\":\"" + expiry + "\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(3L), NamespaceApplication::id)
				.returns(EntityId.from(1L), NamespaceApplication::namespace)
				.returns("Updated", NamespaceApplication::name)
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns(OAuthScopes.of(OAuthScope.INVITE_MEMBERS), NamespaceApplication::scopes)
				.returns("kfg-AQEAAAAAAAAAAQAAAABqJTlV2OXTvVveqnbXJ21wbPw", NamespaceApplication::clientId)
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
	@Transactional
	@DisplayName("should update namespace Workload OAuth application settings")
	void updateApplicationSettings() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "konfigyr", EntityId.from(6).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Updated app\",\"type\":\"AGENT\",\"scopes\":\"namespaces:read\",\"settings\":{\"type\":\"workload\",\"issuerUri\":\"https://issuer.com\"}}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceApplication.class)
				.returns(EntityId.from(6L), NamespaceApplication::id)
				.returns(EntityId.from(2L), NamespaceApplication::namespace)
				.returns(NamespaceClientType.WORKLOAD, NamespaceApplication::type)
				.returns("Updated app", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.READ_NAMESPACES), NamespaceApplication::scopes)
				.returns("kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0", NamespaceApplication::clientId)
				.returns(null, NamespaceApplication::clientSecret)
				.returns(null, NamespaceApplication::expiresAt)
				.satisfies(it -> assertThat(it.settings())
						.asInstanceOf(InstanceOfAssertFactories.type(NamespaceApplicationSettings.WorkloadSettings.class))
						.returns("https://issuer.com", NamespaceApplicationSettings.WorkloadSettings::issuerUri)
						.returns(null, NamespaceApplicationSettings.WorkloadSettings::subjectPattern)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusHours(15), within(1, ChronoUnit.HOURS))
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
								.containsExactlyInAnyOrder("scopes", "name", "type")
						)
				));
	}

	@Test
	@DisplayName("should fail to update application for unknown namespace")
	void updateApplicationUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}", "unknown-namespace", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New app\",\"type\":\"WORKLOAD\",\"scopes\":\"namespaces\"}")
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
				.content("{\"name\":\"New app\",\"type\":\"AGENT\",\"scopes\":\"namespaces\"}")
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
				.content("{\"name\":\"New app\",\"type\":\"SERVICE_ACCOUNT\",\"scopes\":\"namespaces\"}")
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
				.content("{\"name\":\"New app\",\"type\":\"AGENT\",\"scopes\":\"namespaces\"}")
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
				.returns(NamespaceClientType.SERVICE_ACCOUNT, NamespaceApplication::type)
				.returns("Personal app", NamespaceApplication::name)
				.returns(OAuthScopes.of(OAuthScope.NAMESPACES), NamespaceApplication::scopes)
				.returns("kfg-AQEAAAAAAAAAAQAAAABqJTlV2OXTvVveqnbXJ21wbPw", NamespaceApplication::clientId)
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
	@Transactional
	@DisplayName("should fail to reset namespace OAuth application which does not support client_secret")
	void resetAgentApplication() {
		mvc.put().uri("/namespaces/{slug}/applications/{id}/reset", "konfigyr", EntityId.from(5).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.UNPROCESSABLE_CONTENT, problem -> problem
						.hasTitle("Client secret is not supported")
						.hasDetailContaining("Client secret reset is not available for %s applications.", NamespaceClientType.AGENT.displayName())
				))
				.satisfies(hasFailedWithException(NamespaceApplicationTypeException.class, ex -> ex
				.extracting(NamespaceApplicationTypeException::getErrorCode)
				.isEqualTo(NamespaceApplicationTypeException.ErrorCode.SECRET_NOT_SUPPORTED)
		));
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
