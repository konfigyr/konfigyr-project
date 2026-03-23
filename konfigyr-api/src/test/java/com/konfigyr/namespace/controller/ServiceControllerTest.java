package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.*;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.version.Version;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class ServiceControllerTest extends AbstractNamespaceControllerTest {

	@Test
	@DisplayName("should retrieve all available services for Konfigyr namespace")
	void shouldListServices() {
		mvc.get().uri("/namespaces/konfigyr/services")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(Service.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(2)
						.extracting(Service::slug)
						.containsExactly("konfigyr-api", "konfigyr-id")
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
	@DisplayName("should not retrieve services from an unknown namespace")
	void retrieveServicesFromUnknownNamespace() {
		mvc.get().uri("/namespaces/unknown-namespace/services")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve namespace services when user is not a member of a namespace")
	void retrieveServicesWithoutMembership() {
		mvc.get().uri("/namespaces/john-doe/services")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not retrieve namespace services when namespaces:read scope is not present")
	void retrieveServicesWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should retrieve namespace service by slug")
	void retrieveService() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(Service.class)
				.returns(EntityId.from(2), Service::id)
				.returns(EntityId.from(2), Service::namespace)
				.returns("konfigyr-id", Service::slug)
				.returns("Konfigyr ID", Service::name)
				.returns("Konfigyr Identity service", Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should not retrieve an unknown namespace service")
	void retrieveUnknownService() {
		mvc.get().uri("/namespaces/konfigyr/services/unknown-service")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not retrieve a service for an unknown namespace")
	void retrieveServiceForUnknownNamespace() {
		mvc.get().uri("/namespaces/unknown-namespace/services/unknown-service")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve namespace service when namespaces:read scope is not present")
	void retrieveServiceWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve namespace service when user is not a member of a namespace")
	void retrieveServiceWithoutMembership() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should perform a namespace service check by slug on an existing service")
	void shouldCheckExistingService() {
		mvc.head().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should perform a namespace service check by slug on an unknown service")
	void shouldCheckUnknownService() {
		mvc.head().uri("/namespaces/konfigyr/services/konfigyr-registry")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.body()
				.isEmpty();
	}

	@Test
	@DisplayName("should perform a service check by slug on an unknown namespace")
	void shouldCheckServiceForUnknownNamespace() {
		mvc.head().uri("/namespaces/unknown-namespace/services/konfigyr-registry")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.body()
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create namespace service")
	void createService() {
		mvc.post().uri("/namespaces/konfigyr/services")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-registry\", \"name\":\"Konfigyr Registry\", \"description\":\"Registry for Konfigyr services\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.convertTo(Service.class)
				.returns(EntityId.from(2), Service::namespace)
				.returns("konfigyr-registry", Service::slug)
				.returns("Konfigyr Registry", Service::name)
				.returns("Registry for Konfigyr services", Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to create namespace service with invalid data")
	void createServiceInvalidPayload() {
		mvc.post().uri("/namespaces/konfigyr/services")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
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
								.containsExactlyInAnyOrder("slug", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to create namespace service with existing URL slug")
	void createServiceExistingSlug() {
		mvc.post().uri("/namespaces/konfigyr/services")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Service already exists")
						.hasDetailContaining("A service with the same name or identifier already exists")
				));
	}

	@Test
	@DisplayName("should fail to create service for unknown namespace")
	void createServiceUnknownNamespace() {
		mvc.post().uri("/namespaces/unknown-namespace/services")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to create namespace service when namespaces:read scope is not present")
	void createServiceWithoutScope() {
		mvc.post().uri("/namespaces/konfigyr/services")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to create namespace service when user is not a member of a namespace")
	void createServiceWithoutMembership() {
		mvc.post().uri("/namespaces/john-doe/services")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should update namespace service")
	void updateService() {
		mvc.put().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-oidc\", \"name\":\"Konfigyr OIDC\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Service.class)
				.returns(EntityId.from(2), Service::id)
				.returns(EntityId.from(2), Service::namespace)
				.returns("konfigyr-oidc", Service::slug)
				.returns("Konfigyr OIDC", Service::name)
				.returns(null, Service::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to update namespace service with invalid data")
	void updateServiceInvalidPayload() {
		mvc.put().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
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
								.containsExactlyInAnyOrder("slug", "name")
						)
				));
	}

	@Test
	@DisplayName("should fail to create namespace service with existing URL slug")
	void updateServiceExistingSlug() {
		mvc.put().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Service already exists")
						.hasDetailContaining("A service with the same name or identifier already exists")
				));
	}

	@Test
	@DisplayName("should fail to update unknown service")
	void updateUnknownService() {
		mvc.put().uri("/namespaces/konfigyr/services/unknown-service")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should fail to update service for unknown namespace")
	void updateServiceUnknownNamespace() {
		mvc.put().uri("/namespaces/unknown-namespace/services/unknown-service")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"konfigyr-api\", \"name\":\"Konfigyr API\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to create namespace service when namespaces:read scope is not present")
	void updateServiceWithoutScope() {
		mvc.put().uri("/namespaces/john-doe/services/john-doe-blog")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"john-doe-blog\", \"name\":\"Blog service\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to update namespace service when user is not a member of a namespace")
	void updateServiceWithoutMembership() {
		mvc.put().uri("/namespaces/john-doe/services/john-doe-blog")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"john-doe-blog\", \"name\":\"Blog service\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should delete namespace service")
	void deleteService() {
		mvc.delete().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@DisplayName("should fail to delete a service for an unknown namespace")
	void deleteServiceForUnknownNamespace() {
		mvc.delete().uri("/namespaces/unknown-namespace/services/unknown-service")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to delete an unknown namespace service")
	void deleteUnknownService() {
		mvc.delete().uri("/namespaces/konfigyr/services/unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(serviceNotFound("unknown"));
	}

	@Test
	@DisplayName("should fail to delete namespace service when namespaces:read scope is not present")
	void deleteServiceWithoutScope() {
		mvc.delete().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to delete namespace service when user is not an admin member of a namespace")
	void deleteServiceWithoutMembership() {
		mvc.delete().uri("/namespaces/konfigyr/services/konfigyr-id")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should retrieve the latest namespace service manifest")
	void retrieveServiceManifest() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Manifest.class)
				.returns(EntityId.from(1).serialize(), Manifest::id)
				.returns("Konfigyr ID", Manifest::name)
				.satisfies(it -> assertThat(it.artifacts())
						.hasSize(3)
						.containsExactly(
								Artifact.builder()
										.groupId("com.konfigyr")
										.artifactId("konfigyr-api")
										.version("1.0.0")
										.name("Konfigyr API")
										.description("Private REST API")
										.website("konfigyr.api")
										.repository("https://github.com/konfigyr/konfigyr-project")
										.build(),
								Artifact.builder()
										.groupId("com.konfigyr")
										.artifactId("konfigyr-crypto-api")
										.version("1.0.1")
										.name("Konfigyr Crypto API")
										.description("Spring Boot Crypto API library")
										.repository("https://github.com/konfigyr/konfigyr-crypto")
										.build(),
								Artifact.builder()
										.groupId("com.konfigyr")
										.artifactId("konfigyr-crypto-tink")
										.version("1.0.0")
										.name("Konfigyr Crypto Tink")
										.description("Tink support Konfigyr Crypto API library")
										.repository("https://github.com/konfigyr/konfigyr-crypto")
										.build()
						)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(Instant.now().minus(3, ChronoUnit.DAYS), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should retrieve an empty manifest for service that was not yet released")
	void retrieveManifestForUnreleasedService() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(Manifest.class)
				.returns(EntityId.from(1).serialize(), Manifest::id)
				.returns("John Doe Blog", Manifest::name)
				.returns(List.of(), Manifest::artifacts)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(Instant.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to retrieve manifest for unknown service")
	void retrieveManifestForUnknownService() {
		mvc.get().uri("/namespaces/konfigyr/services/unknown-service/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not retrieve a service manifest for an unknown namespace")
	void retrieveManifestForUnknownNamespace() {
		mvc.get().uri("/namespaces/unknown-namespace/services/unknown-service/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve service manifest when namespaces:read scope is not present")
	void retrieveManifestWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/manifest")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve service manifest when user is not a member of a namespace")
	void retrieveManifestWithoutMembership() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/manifest")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should publish the namespace service manifest")
	void publishServiceManifest() {
		mvc.post().uri("/namespaces/konfigyr/services/konfigyr-api/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artifacts\": [\"com.konfigyr:konfigyr-crypto-api:1.0.2\", \"com.konfigyr:konfigyr-crypto-tink:1.0.0\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.convertTo(Manifest.class)
				.returns(EntityId.from(2).serialize(), Manifest::id)
				.returns("Konfigyr API", Manifest::name)
				.satisfies(it -> assertThat(it.artifacts())
						.hasSize(2)
						.containsExactly(
								Artifact.builder()
										.groupId("com.konfigyr")
										.artifactId("konfigyr-crypto-api")
										.version("1.0.2")
										.name("Konfigyr Crypto API")
										.description("Spring Boot Crypto API library")
										.repository("https://github.com/konfigyr/konfigyr-crypto")
										.build(),
								Artifact.builder()
										.groupId("com.konfigyr")
										.artifactId("konfigyr-crypto-tink")
										.version("1.0.0")
										.name("Konfigyr Crypto Tink")
										.description("Tink support Konfigyr Crypto API library")
										.repository("https://github.com/konfigyr/konfigyr-crypto")
										.build()
						)
				)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(Instant.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@ValueSource(strings = {
			"{}",
			"{\"artifacts\": null}",
			"{\"artifacts\": []}",
			"{\"artifacts\": [null]}",
			"{\"artifacts\": [\"\", \"  \"]}",
			"{\"artifacts\": [\"invalid-coordinates\"]}",
			"{\"artifacts\": [\"invalid:coordinates\", \"com.konfigyr:konfigyr-api:1.0.0\"]}"
	})
	@ParameterizedTest(name = "with payload: {0}")
	@DisplayName("should fail to publish manifest with invalid payload")
	void publishManifestWithInvalidArtifacts(String payload) {
		mvc.post().uri("/namespaces/konfigyr/services/konfigyr-api/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload)
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.containsProperty("errors")
				));
	}

	@Test
	@DisplayName("should fail to publish manifest for unknown service")
	void publishManifestForUnknownService() {
		mvc.post().uri("/namespaces/konfigyr/services/unknown-service/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artifacts\": [\"com.konfigyr:konfigyr-crypto-api:1.0.0\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not publish a service manifest for an unknown namespace")
	void publishManifestForUnknownNamespace() {
		mvc.post().uri("/namespaces/unknown-namespace/services/unknown-service/manifest")
				.with(authentication(TestPrincipals.john(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artifacts\": [\"com.konfigyr:konfigyr-crypto-api:1.0.0\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not publish service manifest when namespaces:read scope is not present")
	void publishManifestWithoutScope() {
		mvc.post().uri("/namespaces/konfigyr/services/konfigyr-id/manifest")
				.with(authentication(TestPrincipals.jane()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artifacts\": [\"com.konfigyr:konfigyr-crypto-api:1.0.0\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.PUBLISH_MANIFESTS));
	}

	@Test
	@DisplayName("should not publish service manifest when user is not a member of a namespace")
	void publishManifestWithoutMembership() {
		mvc.post().uri("/namespaces/john-doe/services/john-doe-blog/manifest")
				.with(authentication(TestPrincipals.jane(), OAuthScope.PUBLISH_MANIFESTS))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"artifacts\": [\"com.konfigyr:konfigyr-crypto-api:1.0.0\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should retrieve the latest namespace service catalog")
	void retrieveServiceCatalog() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/catalog")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceCatalog.class)
				.satisfies(it -> assertThat(it.service())
						.returns(EntityId.from(2), Service::id)
						.returns("konfigyr-id", Service::slug)
				)
				.satisfies(it -> assertThat(it.properties())
						.hasSize(3)
						.allSatisfy(property -> assertThat(property.artifact())
								.returns("org.springframework.boot", ArtifactCoordinates::groupId)
								.returns("spring-boot", ArtifactCoordinates::artifactId)
								.returns(Version.of("4.0.3"), ArtifactCoordinates::version)
						)
						.satisfiesExactlyInAnyOrder(
								property -> assertThat(property)
										.returns("spring.application.name", ServiceCatalog.Property::name)
										.returns("java.lang.String", ServiceCatalog.Property::typeName)
										.returns(StringSchema.instance(), ServiceCatalog.Property::schema)
										.returns("Application name. Typically used with logging to help identify the application.", ServiceCatalog.Property::description)
										.returns(null, ServiceCatalog.Property::defaultValue)
										.returns(null, ServiceCatalog.Property::deprecation),
								property -> assertThat(property)
										.returns("spring.application.index", ServiceCatalog.Property::name)
										.returns("java.lang.Integer", ServiceCatalog.Property::typeName)
										.returns(IntegerSchema.builder().format("int32").build(), ServiceCatalog.Property::schema)
										.returns("Application index.", ServiceCatalog.Property::description)
										.returns(null, ServiceCatalog.Property::defaultValue)
										.returns(null, ServiceCatalog.Property::deprecation),
								property -> assertThat(property)
										.returns("spring.application.deprecated", ServiceCatalog.Property::name)
										.returns("java.lang.Boolean", ServiceCatalog.Property::typeName)
										.returns(BooleanSchema.instance(), ServiceCatalog.Property::schema)
										.returns("Deprecated property that is no longer needed.", ServiceCatalog.Property::description)
										.returns("true", ServiceCatalog.Property::defaultValue)
										.returns(new Deprecation("No longer needed", null), ServiceCatalog.Property::deprecation)
						)
				);
	}

	@Test
	@DisplayName("should retrieve an empty catalog for service that was not yet released")
	void retrieveCatalogForUnreleasedService() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/catalog")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ServiceCatalog.class)
				.satisfies(it -> assertThat(it.service())
						.returns(EntityId.from(1), Service::id)
						.returns("john-doe-blog", Service::slug)
				)
				.satisfies(it -> assertThat(it.properties())
						.isEmpty()
				);
	}

	@Test
	@DisplayName("should fail to retrieve catalog for unknown service")
	void retrieveCatalogForUnknownService() {
		mvc.get().uri("/namespaces/konfigyr/services/unknown-service/catalog")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not retrieve a service catalog for an unknown namespace")
	void retrieveCatalogForUnknownNamespace() {
		mvc.get().uri("/namespaces/unknown-namespace/services/unknown-service/catalog")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve service catalog when namespaces:read scope is not present")
	void retrieveCatalogWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/catalog")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve service catalog when user is not a member of a namespace")
	void retrieveCatalogWithoutMembership() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/catalog")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should search the latest namespace service catalog without search term")
	void searchServiceCatalog() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/catalog/search")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ServiceCatalog.Property.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(3)
						.allSatisfy(property -> assertThat(property.artifact())
								.returns("org.springframework.boot", ArtifactCoordinates::groupId)
								.returns("spring-boot", ArtifactCoordinates::artifactId)
								.returns(Version.of("4.0.3"), ArtifactCoordinates::version)
						)
						.satisfiesExactlyInAnyOrder(
								property -> assertThat(property)
										.returns("spring.application.name", ServiceCatalog.Property::name)
										.returns("java.lang.String", ServiceCatalog.Property::typeName)
										.returns(StringSchema.instance(), ServiceCatalog.Property::schema)
										.returns("Application name. Typically used with logging to help identify the application.", ServiceCatalog.Property::description)
										.returns(null, ServiceCatalog.Property::defaultValue)
										.returns(null, ServiceCatalog.Property::deprecation),
								property -> assertThat(property)
										.returns("spring.application.index", ServiceCatalog.Property::name)
										.returns("java.lang.Integer", ServiceCatalog.Property::typeName)
										.returns(IntegerSchema.builder().format("int32").build(), ServiceCatalog.Property::schema)
										.returns("Application index.", ServiceCatalog.Property::description)
										.returns(null, ServiceCatalog.Property::defaultValue)
										.returns(null, ServiceCatalog.Property::deprecation),
								property -> assertThat(property)
										.returns("spring.application.deprecated", ServiceCatalog.Property::name)
										.returns("java.lang.Boolean", ServiceCatalog.Property::typeName)
										.returns(BooleanSchema.instance(), ServiceCatalog.Property::schema)
										.returns("Deprecated property that is no longer needed.", ServiceCatalog.Property::description)
										.returns("true", ServiceCatalog.Property::defaultValue)
										.returns(new Deprecation("No longer needed", null), ServiceCatalog.Property::deprecation)
						)
				);
	}

	@Test
	@DisplayName("should search the latest namespace service catalog with search term")
	void searchServiceCatalogWithSearchTerm() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/catalog/search")
				.queryParam("term", "deprecated")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ServiceCatalog.Property.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.extracting(ServiceCatalog.Property::name)
						.contains("spring.application.deprecated")
				);
	}

	@Test
	@DisplayName("should search a catalog for service that was not yet released")
	void searchCatalogForUnreleasedService() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/catalog/search")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ServiceCatalog.Property.class))
				.satisfies(it -> assertThat(it.getContent())
						.isEmpty()
				);
	}

	@Test
	@DisplayName("should fail to search catalog for unknown service")
	void searchCatalogForUnknownService() {
		mvc.get().uri("/namespaces/konfigyr/services/unknown-service/catalog/search")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should not search a service catalog for an unknown namespace")
	void searchCatalogForUnknownNamespace() {
		mvc.get().uri("/namespaces/unknown-namespace/services/unknown-service/catalog/search")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not search service catalog when namespaces:read scope is not present")
	void searchCatalogWithoutScope() {
		mvc.get().uri("/namespaces/konfigyr/services/konfigyr-id/catalog/search")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not search service catalog when user is not a member of a namespace")
	void searchCatalogWithoutMembership() {
		mvc.get().uri("/namespaces/john-doe/services/john-doe-blog/catalog/search")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

}
