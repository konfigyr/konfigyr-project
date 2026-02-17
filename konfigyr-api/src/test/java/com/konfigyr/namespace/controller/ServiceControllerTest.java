package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.OAuthScope;
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

}
