package com.konfigyr.kms.controller;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeyStatus;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.kms.*;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class KmsControllerTest extends AbstractControllerTest {

	@Autowired
	NamespaceManager namespaces;

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
						.returns(1L, PagedModel.PageMetadata::number)
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
										.returns(KeysetMetadataAlgorithm.AES256_GCM, KeysetMetadata::algorithm)
										.returns("konfigyr-active", KeysetMetadata::name)
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
				.returns(KeysetMetadataAlgorithm.AES256_GCM, KeysetMetadata::algorithm)
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
				.returns(KeysetMetadataAlgorithm.AES256_GCM, KeysetMetadata::algorithm)
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
	@DisplayName("should retrieve key metadata by keyset entity identifier")
	void retrieveKeyMetadata() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}/keys", "konfigyr", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(collectionModel(KeyMetadata.class))
				.asInstanceOf(InstanceOfAssertFactories.iterable(KeyMetadata.class))
				.hasSize(1)
				.first()
				.returns("374108", KeyMetadata::id)
				.returns(KeysetMetadataAlgorithm.ECDSA_P256, KeyMetadata::algorithm)
				.returns(KeyStatus.DISABLED, KeyMetadata::status)
				.returns(true, KeyMetadata::isPrimary)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now().minusDays(30), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.initializedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(30), within(1, ChronoUnit.HOURS))
				)
				.returns(null, KeyMetadata::expiresAt)
				.returns(null, KeyMetadata::destructionScheduledAt)
				.returns(null, KeyMetadata::destroyedAt);
	}

	@Test
	@DisplayName("should not retrieve key metadata for an unknown keyset")
	void retrieveUnknownKeyMetadata() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}/keys", "konfigyr", EntityId.from(9999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(keysetMetadataNotFound(9999));
	}

	@Test
	@DisplayName("should not retrieve key metadata for an unknown namespace")
	void retrieveKeyForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}/keys", "unknown", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should not retrieve key metadata that belongs to a different namespace")
	void retrieveKeyForDifferentNamespace() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}/keys", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(keysetMetadataNotFound(1));
	}

	@Test
	@DisplayName("should not retrieve key metadata when namespaces:read scope is not present")
	void retrieveKeyWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}/keys", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.jane()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should not retrieve key metadata when user is not a member of a namespace")
	void retrieveKeyWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/kms/{id}/keys", "john-doe", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@Transactional
	@DisplayName("should rotate keyset metadata with default algorithm")
	void rotateKeysetWithDefaultAlgorithm() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/rotate", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataAlgorithm.AES256_GCM, KeysetMetadata::algorithm);
	}

	@Test
	@Transactional
	@DisplayName("should rotate keyset metadata with custom algorithm")
	void rotateKeysetWitCustomAlgorithm() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/rotate", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"algorithm\":\"AES128_GCM\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataAlgorithm.AES128_GCM, KeysetMetadata::algorithm);
	}

	@Test
	@DisplayName("should fail to rotate keyset metadata with an unsupported algorithm")
	void rotateKeysetWitUnsupportedAlgorithm() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/rotate", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"algorithm\":\"ED25519\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Unsupported algorithm")
						.hasDetailContaining("specified algorithm is not supported for this key purpose or key type")
				).andThen(hasFailedWithException(CryptoException.UnsupportedAlgorithmException.class)));
	}

	@Test
	@DisplayName("should fail to rotate keyset metadata for an unknown namespace")
	void rotateKeysetUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/rotate", "unknown-namespace", EntityId.from(3).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to rotate keyset metadata for a different namespace")
	void rotateKeysetDifferentNamespace() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/rotate", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(keysetMetadataNotFound(1));
	}

	@Test
	@DisplayName("should fail to rotate keyset metadata when namespaces:write scope is not present")
	void rotateKeysetWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/deactivate", "konfigyr", EntityId.from(3).serialize(), "374108")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@Transactional
	@DisplayName("should deactivate keyset metadata")
	void deactivateKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/deactivate", "konfigyr", EntityId.from(2).serialize(), "738802")
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

		assertThat(keysetMetadataFor("konfigyr", 2))
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state);
	}

	@Test
	@Transactional
	@DisplayName("should deactivate a secondary key within a keyset metadata")
	void deactivateSecondaryKey() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/deactivate", "john-doe", EntityId.from(1).serialize(), "407725")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(1L), KeysetMetadata::id)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state);

		final var keyset = keysetMetadataFor("john-doe", 1);

		assertThat(keyset)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state);

		assertThat(manager.keys(keyset))
				.extracting(KeyMetadata::id, KeyMetadata::status, KeyMetadata::isPrimary)
				.containsExactlyInAnyOrder(
						tuple("106475", KeyStatus.ENABLED, true),
						tuple("407725", KeyStatus.DISABLED, false)
				);
	}

	@Test
	@Transactional
	@DisplayName("should mark a key that is marked as primary for keyset metadata as compromised")
	void compromisePrimaryKey() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/compromised", "konfigyr", EntityId.from(2).serialize(), "738802")
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

		final var keyset = keysetMetadataFor("konfigyr", 2);

		assertThat(keyset)
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state);

		assertThat(manager.keys(keyset))
				.extracting(KeyMetadata::id, KeyMetadata::status, KeyMetadata::isPrimary)
				.containsExactlyInAnyOrder(
						tuple("604025", KeyStatus.ENABLED, false),
						tuple("738802", KeyStatus.COMPROMISED, true)
				);
	}

	@Test
	@Transactional
	@DisplayName("should mark a key that is not marked as primary for keyset metadata as compromised")
	void compromiseSecondaryKey() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/compromised", "konfigyr", EntityId.from(2).serialize(), "604025")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(2L), KeysetMetadata::id)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state);

		final var keyset = keysetMetadataFor("konfigyr", 2);

		assertThat(keyset)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state);

		assertThat(manager.keys(keyset))
				.extracting(KeyMetadata::id, KeyMetadata::status, KeyMetadata::isPrimary)
				.containsExactlyInAnyOrder(
						tuple("738802", KeyStatus.ENABLED, true),
						tuple("604025", KeyStatus.COMPROMISED, false)
				);
	}

	@Test
	@Transactional
	@DisplayName("should restore keyset metadata that is scheduled for destruction")
	void restoreDestroyedKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/restore", "konfigyr", EntityId.from(4).serialize(), "162009")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(4L), KeysetMetadata::id)
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state);

		assertThat(keysetMetadataFor("konfigyr", 4))
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state);
	}

	@Test
	@DisplayName("should fail to deactivate keyset metadata that is already destroyed")
	void deactivateDestroyedKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/deactivate", "konfigyr", EntityId.from(5).serialize(), "431904")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Invalid key state transition")
						.hasDetailContaining("cannot be transitioned to DESTROYED because it is currently in the INACTIVE state")
				).andThen(hasFailedWithException(KeysetTransitionException.class, ex -> ex
						.hasMessageContaining("Failed to transition keyset %s from DESTROYED to INACTIVE", EntityId.from(5).serialize())
				)));
	}

	@Test
	@DisplayName("should fail to deactivate keyset metadata for an unknown namespace")
	void deactivateKeysetUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/deactivate", "unknown-namespace", EntityId.from(1).serialize(), "106475")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to deactivate keyset metadata when namespaces:write scope is not present")
	void deactivateKeysetWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/deactivate", "konfigyr", EntityId.from(2).serialize(), "738802")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@Transactional
	@DisplayName("should reactivate keyset metadata")
	void reactivateKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/reactivate", "konfigyr", EntityId.from(3).serialize(), "374108")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(KeysetMetadata.class)
				.returns(EntityId.from(3L), KeysetMetadata::id)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state);

		assertThat(keysetMetadataFor("konfigyr", 3))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state);
	}

	@Test
	@Transactional
	@DisplayName("should fail to reactivate keyset metadata because the key can not be found")
	void reactivateUnknownKeyInKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/reactivate", "konfigyr", EntityId.from(3).serialize(), "738802")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Key not found")
						.hasDetailContaining("The key you are trying to access does not exist or is no longer available within the keyset")
				).andThen(hasFailedWithException(CryptoException.KeyNotFoundException.class, ex -> ex
						.returns("738802", CryptoException.KeyNotFoundException::getKeyId)
				)));
	}

	@Test
	@DisplayName("should fail to reactivate keyset metadata that is already destroyed")
	void reactivateDestroyedKeyset() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/reactivate", "konfigyr", EntityId.from(5).serialize(), "431904")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Invalid key state transition")
						.hasDetailContaining("key cannot be transitioned to DESTROYED because it is currently in the ACTIVE state")
				).andThen(hasFailedWithException(KeysetTransitionException.class, ex -> ex
						.hasMessageContaining("Failed to transition keyset %s from DESTROYED to ACTIVE", EntityId.from(5).serialize())
				)));
	}

	@Test
	@DisplayName("should fail to reactivate keyset metadata for an unknown namespace")
	void reactivateKeysetUnknownNamespace() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/reactivate", "unknown-namespace", EntityId.from(3).serialize(), "374108")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to reactivate keyset metadata when namespaces:write scope is not present")
	void reactivateKeysetWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/reactivate", "konfigyr", EntityId.from(3).serialize(), "374108")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_NAMESPACES));
	}

	@Test
	@DisplayName("should fail to transition keyset metadata when user is not a member of a namespace")
	void disableKeysetWithoutMembership() {
		mvc.put().uri("/namespaces/{slug}/kms/{id}/keys/{key}/reactivate", "konfigyr", EntityId.from(2).serialize(), "738802")
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
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThat(manager.get(namespaceFor("konfigyr"), EntityId.from(2L)))
				.isEmpty();
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

	Namespace namespaceFor(String slug) {
		return assertThat(namespaces.findBySlug(slug))
				.as("Namespace with slug '%s' not found", slug)
				.isPresent()
				.get()
				.actual();
	}

	KeysetMetadata keysetMetadataFor(String namespace, long keyset) {
		return assertThat(manager.get(namespaceFor(namespace), EntityId.from(keyset)))
				.as("Keyset metadata with id '%d' can not be found in '%s' Namespace", keyset, namespace)
				.isPresent()
				.get()
				.actual();
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
