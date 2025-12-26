package com.konfigyr.kms.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.kms.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class KmsControllerTest extends AbstractControllerTest {

	@Autowired
	KeysetManager manager;

	@Test
	@DisplayName("should retrieve keyset metadata for namespace")
	void listKeysets() {
		mvc.get().uri("/namespaces/{slug}/kms", "konfigyr")
				.queryParam("sort", "name")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(KeysetMetadata.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(4)
						.satisfiesExactly(
								keyset -> assertThat(keyset)
										.returns(EntityId.from(2L), KeysetMetadata::id)
										.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
										.returns("konfigyr-active", KeysetMetadata::name),
								keyset -> assertThat(keyset)
										.returns(EntityId.from(4L), KeysetMetadata::id)
										.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
										.returns("konfigyr-deleted", KeysetMetadata::name),
								keyset -> assertThat(keyset)
										.returns(EntityId.from(5L), KeysetMetadata::id)
										.returns(KeysetMetadataState.DESTROYED, KeysetMetadata::state)
										.returns("konfigyr-destroyed", KeysetMetadata::name),
								keyset -> assertThat(keyset)
										.returns(EntityId.from(3L), KeysetMetadata::id)
										.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
										.returns("konfigyr-inactive", KeysetMetadata::name)
						)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(20L, PagedModel.PageMetadata::size)
						.returns(0L, PagedModel.PageMetadata::number)
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
	@DisplayName("should retrieve keyset metadata for namespace that are matching the search term")
	void findKeysetByTerm() {
		mvc.get().uri("/namespaces/{slug}/kms", "konfigyr")
				.queryParam("sort", "name")
				.queryParam("term", "active")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(KeysetMetadata.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(2)
						.satisfiesExactly(
								keyset -> assertThat(keyset)
										.returns(EntityId.from(2L), KeysetMetadata::id)
										.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
										.returns("konfigyr-active", KeysetMetadata::name),
								keyset -> assertThat(keyset)
										.returns(EntityId.from(3L), KeysetMetadata::id)
										.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
										.returns("konfigyr-inactive", KeysetMetadata::name)
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
	@DisplayName("should retrieve keyset metadata for namespace that are matching the given state and algorithm")
	void findKeysetByStateAndAlgorithm() {
		mvc.get().uri("/namespaces/{slug}/kms", "konfigyr")
				.queryParam("sort", "name")
				.queryParam("state", KeysetMetadataState.ACTIVE.name())
				.queryParam("algorithm", KeysetMetadataAlgorithm.AES256_GCM.name())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(KeysetMetadata.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.satisfiesExactly(
								keyset -> assertThat(keyset)
										.returns(EntityId.from(2L), KeysetMetadata::id)
										.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
										.returns(KeysetMetadataAlgorithm.AES256_GCM.name(), KeysetMetadata::algorithm)
										.returns("konfigyr-active", KeysetMetadata::name)
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
	@DisplayName("should retrieve keyset metadata for an unknown namespace")
	void listKeysetsForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/kms", "unknown-namespace")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should not retrieve keyset metadata when user is not a member of a namespace")
	void listKeysetsForWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/kms", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should not retrieve keyset metadata when namespaces:read scope is not present")
	void listKeysetsForWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/kms", "konfigyr")
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should retrieve keyset metadata by entity identifier")
	void retrieveKeyset() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataAlgorithm.AES256_GCM.name(), KeysetMetadata::algorithm)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns("konfigyr-active", KeysetMetadata::name)
				.returns("Active keyset", KeysetMetadata::description)
				.returns(Set.of("encryption", "konfigyr"), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(7), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.returns(null, KeysetMetadata::destroyedAt);
	}

	@Test
	@DisplayName("should not retrieve unknown keyset metadata")
	void retrieveUnknownKeyset() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(9999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(keysetMetadataNotFound(9999));
	}

	@Test
	@DisplayName("should not retrieve keyset metadata for an unknown namespace")
	void retrieveKeysetForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}", "unknown", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not retrieve keyset metadata that belongs to a different namespace")
	void retrieveKeysetForDifferentNamespace() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(keysetMetadataNotFound(1));
	}

	@Test
	@DisplayName("should not retrieve keyset metadata when namespaces:read scope is not present")
	void retrieveKeysetWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve keyset metadata when user is not a member of a namespace")
	void retrieveKeysetWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}", "john-doe", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should create new KMS keyset metadata")
	void createKeyset() {
		final var json = JsonNodeFactory.instance.objectNode()
				.put("name", "new keyset")
				.put("algorithm", "AES256_GCM")
				.put("description", "New keyset description")
				.putPOJO("tags", Set.of("tag 1"));

		mvc.post().uri("/namespaces/{slug}/kms", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.toPrettyString())
				.exchange()
				.assertThat()
				.apply(log())
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns("new keyset", KeysetMetadata::name)
				.returns("New keyset description", KeysetMetadata::description)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns(KeysetMetadataAlgorithm.AES256_GCM.name(), KeysetMetadata::algorithm)
				.returns(Set.of("tag 1"), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to create keyset metadata with invalid data")
	void createKeysetInvalidPayload() {
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
	@DisplayName("should fail to create keyset metadata for an unknown namespace")
	void createKeysetUnknownNamespace() {
		mvc.post().uri("/namespaces/{slug}/kms", "unknown-namespace")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"new keyset\", \"algorithm\":\"AES256_GCM\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to create keyset metadata when namespaces:write scope is not present")
	void createKeysetWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/kms", "konfigyr")
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"new keyset\", \"algorithm\":\"AES256_GCM\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to create keyset metadata when user is not a member of a namespace")
	void createKeysetWithoutMembership() {
		mvc.post().uri("/namespaces/{slug}/kms", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"new keyset\", \"algorithm\":\"AES256_GCM\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should patch keyset metadata where description is updated")
	void updateKeysetDescription() {
		mvc.patch().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"new keyset description\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns("new keyset description", KeysetMetadata::description)
				.returns(Set.of("encryption", "konfigyr"), KeysetMetadata::tags);
	}

	@Test
	@Transactional
	@DisplayName("should patch keyset metadata where tags are replaced")
	void updateKeysetTags() {
		mvc.patch().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"tags\":[\"new keyset tags\"]}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns("Active keyset", KeysetMetadata::description)
				.returns(Set.of("new keyset tags"), KeysetMetadata::tags);
	}

	@Test
	@Transactional
	@DisplayName("should patch keyset metadata where tags and description are removed")
	void removeKeysetDescriptionAndTags() {
		mvc.patch().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"tags\":[], \"description\": null}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns(null, KeysetMetadata::description)
				.returns(Set.of(), KeysetMetadata::tags);
	}

	@Test
	@DisplayName("should fail to update keyset metadata for an unknown namespace")
	void updateKeysetUnknownNamespace() {
		mvc.patch().uri("/namespaces/{slug}/kms/{id}", "unknown-namespace", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to update keyset metadata when namespaces:write scope is not present")
	void updateKeysetWithoutScope() {
		mvc.patch().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to update keyset metadata when user is not a member of a namespace")
	void updateKeysetWithoutMembership() {
		mvc.patch().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should deactivate keyset metadata")
	void deactivateKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/deactivate", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state);

		assertThat(manager.get(EntityId.from(2L)))
				.isPresent()
				.get()
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state);
	}

	@Test
	@DisplayName("should fail to deactivate keyset metadata that is already destroyed")
	void deactivateDestroyedKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/deactivate", "konfigyr", EntityId.from(5).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Keyset transition error")
						.hasDetailContaining("Failed to transition a keyset from DESTROYED to INACTIVE state")
				).andThen(hasFailedWithException(KeysetTransitionException.class, ex -> ex
						.hasMessageContaining("Failed to transition keyset %s from DESTROYED to INACTIVE", EntityId.from(5).serialize())
				)));
	}

	@Test
	@DisplayName("should fail to deactivate keyset metadata for an unknown namespace")
	void deactivateKeysetUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/deactivate", "unknown-namespace", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to deactivate keyset metadata when namespaces:write scope is not present")
	void deactivateKeysetWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/deactivate", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to transition keyset metadata when user is not a member of a namespace")
	void disableKeysetWithoutMembership() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/deactivate", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should destroy keyset metadata")
	void destroyKeyset() {
		mvc.delete().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state);

		assertThat(manager.get(EntityId.from(2L)))
				.isPresent()
				.get()
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state);
	}

	@Test
	@DisplayName("should fail to destroy keyset metadata for an unknown namespace")
	void destroyKeysetUnknownNamespace() {
		mvc.delete().uri("/namespaces/{slug}/kms/{id}", "unknown-namespace", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to destroy keyset metadata when namespaces:write scope is not present")
	void destroyKeysetWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to destroy keyset metadata when user is not a member of a namespace")
	void destroyKeysetWithoutMembership() {
		mvc.delete().uri("/namespaces/{slug}/kms/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	static Consumer<MvcTestResult> keysetMetadataNotFound(long id) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Keyset not found")
				.hasDetailContaining("The keyset you're trying to access doesn't exist or is no longer available")
		).andThen(hasFailedWithException(KeysetNotFoundException.class, ex -> ex
				.hasMessageContaining("Could not find keyset metadata with the following identifier: %s", EntityId.from(id).serialize())
		));
	}

}
