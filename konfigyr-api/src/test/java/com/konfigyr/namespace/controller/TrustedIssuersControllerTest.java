package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.NamespaceTrustedIssuer;
import com.konfigyr.namespace.NamespaceTrustedIssuerNotFoundException;
import com.konfigyr.security.OAuthScope;
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
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class TrustedIssuersControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should retrieve trusted issuers for namespace")
	void listTrustedIssuers() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(NamespaceTrustedIssuer.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(3)
						.satisfiesExactly(
								issuer -> assertThat(issuer)
										.returns(EntityId.from(1L), NamespaceTrustedIssuer::id)
										.returns(EntityId.from(2L), NamespaceTrustedIssuer::namespace)
										.returns("Konfigyr CI", NamespaceTrustedIssuer::name)
										.returns("GitHub Actions for Konfigyr org", NamespaceTrustedIssuer::description)
										.returns("https://ci.konfigyr.com", NamespaceTrustedIssuer::issuerUri)
										.returns("https://ci.konfigyr.com/jwks.json", NamespaceTrustedIssuer::jwksUri)
										.returns(true, NamespaceTrustedIssuer::active)
										.satisfies(i -> assertThat(i.allowedAudiences()).containsExactly("konfigyr-api"))
										.satisfies(i -> assertThat(i.customClaims()).containsEntry("environment", "production")),
								issuer -> assertThat(issuer)
										.returns(EntityId.from(2L), NamespaceTrustedIssuer::id)
										.returns(EntityId.from(2L), NamespaceTrustedIssuer::namespace)
										.returns("Konfigyr staging CI", NamespaceTrustedIssuer::name)
										.returns(null, NamespaceTrustedIssuer::description)
										.returns("https://ci-staging.konfigyr.com", NamespaceTrustedIssuer::issuerUri)
										.returns(null, NamespaceTrustedIssuer::jwksUri)
										.returns(true, NamespaceTrustedIssuer::active)
										.satisfies(i -> assertThat(i.allowedAudiences()).isEmpty())
										.satisfies(i -> assertThat(i.customClaims()).isEmpty()),
								issuer -> assertThat(issuer)
										.returns(EntityId.from(3L), NamespaceTrustedIssuer::id)
										.returns(EntityId.from(2L), NamespaceTrustedIssuer::namespace)
										.returns("Disabled issuer", NamespaceTrustedIssuer::name)
										.returns(false, NamespaceTrustedIssuer::active)
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
	@DisplayName("should retrieve only active trusted issuers for namespace")
	void listActiveTrustedIssuers() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.queryParam("active", "true")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(NamespaceTrustedIssuer.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(2)
						.allSatisfy(issuer -> assertThat(issuer.active()).isTrue())
				);
	}

	@Test
	@DisplayName("should retrieve trusted issuer by ID")
	void getTrustedIssuer() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceTrustedIssuer.class)
				.returns(EntityId.from(1L), NamespaceTrustedIssuer::id)
				.returns(EntityId.from(2L), NamespaceTrustedIssuer::namespace)
				.returns("Konfigyr CI", NamespaceTrustedIssuer::name)
				.returns("https://ci.konfigyr.com", NamespaceTrustedIssuer::issuerUri)
				.returns("https://ci.konfigyr.com/jwks.json", NamespaceTrustedIssuer::jwksUri)
				.satisfies(it -> assertThat(it.allowedAudiences()).containsExactly("konfigyr-api"))
				.satisfies(it -> assertThat(it.customClaims()).containsEntry("environment", "production"));
	}

	@Test
	@DisplayName("should not retrieve unknown trusted issuer")
	void getUnknownTrustedIssuer() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(9999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(trustedIssuerNotFound(9999));
	}

	@Test
	@DisplayName("should not retrieve trusted issuer belonging to a different namespace")
	void getTrustedIssuerFromDifferentNamespace() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(4).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(trustedIssuerNotFound(4));
	}

	@Test
	@DisplayName("should not retrieve trusted issuers without namespaces:read scope")
	void listTrustedIssuersWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve trusted issuers when user is not a namespace admin")
	void listTrustedIssuersWithoutAdminRole() {
		mvc.get().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should create trusted issuer for namespace")
	void createTrustedIssuer() {
		mvc.post().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"New CI\",\"issuerUri\":\"https://new-ci.example.com\",\"allowedAudiences\":[\"konfigyr-api\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CREATED)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceTrustedIssuer.class)
				.returns(EntityId.from(2L), NamespaceTrustedIssuer::namespace)
				.returns("New CI", NamespaceTrustedIssuer::name)
				.returns("https://new-ci.example.com", NamespaceTrustedIssuer::issuerUri)
				.returns(null, NamespaceTrustedIssuer::jwksUri)
				.returns(true, NamespaceTrustedIssuer::active)
				.satisfies(it -> assertThat(it.allowedAudiences()).containsExactly("konfigyr-api"))
				.satisfies(it -> assertThat(it.customClaims()).isEmpty())
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@Transactional
	@DisplayName("should fail to create trusted issuer for namespace with invalid payload")
	void createTrustedIssuerWithInvalidData() {
		mvc.post().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"issuerUri\":\"issuer\",\"jwksUri\":\"jwks\",\"allowedAudiences\":[\" \"],\"customClaims\":{\"missing\":\"\"}}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("name", "issuerUri", "jwksUri", "allowedAudiences[0]", "customClaims[missing]")
						)
				));
	}

	@Test
	@DisplayName("should not create trusted issuer without namespaces:write scope")
	void createTrustedIssuerWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/trusted-issuers", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Missing scope\",\"issuerUri\":\"https://x.example.com\",\"allowedAudiences\":[]}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should not create trusted issuer for unknown namespace")
	void createTrustedIssuerForUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/trusted-issuers", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Unknown\",\"issuerUri\":\"https://x.example.com\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@Transactional
	@DisplayName("should update trusted issuer")
	void updateTrustedIssuer() {
		mvc.put().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Konfigyr CI updated\",\"description\":\"Updated description\"," +
						"\"issuerUri\":\"https://ci.konfigyr.com\",\"allowedAudiences\":[\"konfigyr-api\", \"konfigyr-cli\"]," +
						"\"customClaims\":{\"environment\":\"production\",\"org\":\"konfigyr\"}}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(NamespaceTrustedIssuer.class)
				.returns(EntityId.from(1L), NamespaceTrustedIssuer::id)
				.returns("Konfigyr CI updated", NamespaceTrustedIssuer::name)
				.returns("Updated description", NamespaceTrustedIssuer::description)
				.satisfies(it -> assertThat(it.allowedAudiences())
						.containsExactlyInAnyOrder("konfigyr-api", "konfigyr-cli"))
				.satisfies(it -> assertThat(it.customClaims())
						.containsEntry("environment", "production")
						.containsEntry("org", "konfigyr"))
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should not update unknown trusted issuer")
	void updateUnknownTrustedIssuer() {
		mvc.put().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(9999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Unknown\",\"issuerUri\":\"https://x.example.com\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(trustedIssuerNotFound(9999));
	}

	@Test
	@Transactional
	@DisplayName("should delete trusted issuer")
	void deleteTrustedIssuer() {
		mvc.delete().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	@DisplayName("should not delete unknown trusted issuer")
	void deleteUnknownTrustedIssuer() {
		mvc.delete().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(9999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(trustedIssuerNotFound(9999));
	}

	@Test
	@DisplayName("should not delete trusted issuer without namespaces:write scope")
	void deleteTrustedIssuerWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/trusted-issuers/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	static Consumer<MvcTestResult> trustedIssuerNotFound(long id) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Trusted issuer not found")
				.hasDetailContaining("We couldn't find a trusted issuer matching your request.")
		).andThen(hasFailedWithException(NamespaceTrustedIssuerNotFoundException.class, ex -> ex
				.hasMessageContaining("Could not find a namespace trusted issuer with the following identifier: %s",
						EntityId.from(id).serialize())
		));
	}

}
