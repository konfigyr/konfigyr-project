package com.konfigyr.kms;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeyStatus;
import com.konfigyr.crypto.KeysetStore;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class KeysetManagerTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	KeysetManager manager;

	@Autowired
	KeysetStore store;

	@Test
	@DisplayName("should search for keysets by namespace")
	void searchByNamespace() {
		final var query = SearchQuery.of(Pageable.ofSize(10));
		final var metadata = manager.find(namespaceFor("konfigyr"), query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(4, Page::getNumberOfElements)
				.returns(1, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(4)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(2), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.AES256_GCM, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
								.returns("konfigyr-active", KeysetMetadata::name)
								.returns("Active keyset", KeysetMetadata::description)
								.returns(Set.of("encryption", "konfigyr"), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(5), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.AES128_GCM, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.DESTROYED, KeysetMetadata::state)
								.returns("konfigyr-destroyed", KeysetMetadata::name)
								.returns("Destroyed keyset", KeysetMetadata::description)
								.returns(Set.of("destroyed", "konfigyr"), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(4), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.ED25519, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
								.returns("konfigyr-deleted", KeysetMetadata::name)
								.returns("Pending removal keyset", KeysetMetadata::description)
								.returns(Set.of(), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(3), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.ECDSA_P256, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
								.returns("konfigyr-inactive", KeysetMetadata::name)
								.returns("Inactive keyset", KeysetMetadata::description)
								.returns(Set.of("signing", "konfigyr"), KeysetMetadata::tags)
				);
	}

	@Test
	@DisplayName("should search for keysets by algorithm and state")
	void searchByAlgorithmAndState() {
		final var query = SearchQuery.builder()
				.criteria(KeysetMetadata.ALGORITHM_CRITERIA, KeysetMetadataAlgorithm.AES128_GCM)
				.criteria(KeysetMetadata.STATE_CRITERIA, KeysetMetadataState.ACTIVE)
				.build();

		final var metadata = manager.find(namespaceFor("john-doe"), query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(1, Page::getNumberOfElements)
				.returns(1, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(1)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(1), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.AES128_GCM, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
								.returns("john-doe-keyset", KeysetMetadata::name)
								.returns("John Doe keyset", KeysetMetadata::description)
								.returns(Set.of("private-key"), KeysetMetadata::tags)
				);
	}

	@Test
	@DisplayName("should search for keysets by identifier")
	void searchByIdentifier() {
		final var query = SearchQuery.builder()
				.criteria(KeysetMetadata.ID_CRITERIA, EntityId.from(3))
				.build();

		final var metadata = manager.find(namespaceFor("konfigyr"), query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(1, Page::getNumberOfElements)
				.returns(1, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(1)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(3), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.ECDSA_P256, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
								.returns("konfigyr-inactive", KeysetMetadata::name)
								.returns("Inactive keyset", KeysetMetadata::description)
								.returns(Set.of("signing", "konfigyr"), KeysetMetadata::tags)
				);
	}

	@Test
	@DisplayName("should search for keysets by term and sort by keyset name")
	void searchByTerm() {
		final var query = SearchQuery.builder()
				.term("konfigyr")
				.pageable(PageRequest.of(0, 2, Sort.by("name")))
				.build();

		final var metadata = manager.find(namespaceFor("konfigyr"), query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(2, Page::getNumberOfElements)
				.returns(2, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(2)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(2), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.AES256_GCM, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
								.returns("konfigyr-active", KeysetMetadata::name)
								.returns("Active keyset", KeysetMetadata::description)
								.returns(Set.of("encryption", "konfigyr"), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(4), KeysetMetadata::id)
								.returns(KeysetMetadataAlgorithm.ED25519, KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
								.returns("konfigyr-deleted", KeysetMetadata::name)
								.returns("Pending removal keyset", KeysetMetadata::description)
								.returns(Set.of(), KeysetMetadata::tags)
				);
	}

	@Test
	@DisplayName("should retrieve keyset metadata by identifier")
	void retrieveKeyset() {
		assertThat(manager.get(namespaceFor("john-doe"), EntityId.from(1)))
				.isNotEmpty()
				.get()
				.returns(EntityId.from(1), KeysetMetadata::id)
				.returns(KeysetMetadataAlgorithm.AES128_GCM, KeysetMetadata::algorithm)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns("john-doe-keyset", KeysetMetadata::name)
				.returns("John Doe keyset", KeysetMetadata::description)
				.returns(Set.of("private-key"), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(5, ChronoUnit.MINUTES))
				)
				.returns(null, KeysetMetadata::destroyedAt);
	}

	@Test
	@DisplayName("should retrieve an unknown keyset metadata by identifier")
	void retrieveUnknownKeyset() {
		assertThat(manager.get(namespaceFor("konfigyr"), EntityId.from(9999)))
				.isEmpty();
	}

	@Test
	@DisplayName("should not retrieve keyset metadata by identifier that belongs to a different namespace")
	void retrieveKeysetFromDifferentNamespace() {
		assertThat(manager.get(namespaceFor("konfigyr"), EntityId.from(1)))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve keyset operations by identifier")
	void retrieveKeysetOperations() {
		final var operations = manager.operations(namespaceFor("john-doe"), EntityId.from(1));

		assertThat(operations)
				.isNotNull();

		assertThatNoException()
				.isThrownBy(() -> operations.encrypt(ByteArray.fromString("data")));
	}

	@Test
	@DisplayName("should failed to retrieve keyset operations by identifier for unknown keyset")
	void retrieveUnknownKeysetOperations() {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.operations(namespaceFor("konfigyr"), EntityId.from(9999)));
	}

	@Test
	@DisplayName("should fail to retrieve keyset operations for a non-active keyset")
	void retrieveInactiveKeysetOperations() {
		assertThatExceptionOfType(InactiveKeysetException.class)
				.isThrownBy(() -> manager.operations(namespaceFor("konfigyr"), EntityId.from(5)));
	}

	@Test
	@DisplayName("should fail to retrieve keyset operations for keyset that belongs to a different namespace")
	void retrieveKeysetOperationsForDifferentNamespace() {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.operations(namespaceFor("konfigyr"), EntityId.from(1)));
	}

	@Test
	@DisplayName("should retrieve key metadata for keyset")
	void retrieveKeyMetadata() {
		final var keyset = keysetMetadataFor("konfigyr", 2);

		assertThat(manager.keys(keyset))
				.isNotNull()
				.hasSize(2)
				.satisfiesExactly(
						key -> assertThat(key)
								.returns("738802", KeyMetadata::id)
								.returns(KeysetMetadataAlgorithm.AES256_GCM, KeyMetadata::algorithm)
								.returns(KeyStatus.ENABLED, KeyMetadata::status)
								.returns(true, KeyMetadata::isPrimary),
						key -> assertThat(key)
								.returns("604025", KeyMetadata::id)
								.returns(KeysetMetadataAlgorithm.AES128_GCM, KeyMetadata::algorithm)
								.returns(KeyStatus.ENABLED, KeyMetadata::status)
								.returns(false, KeyMetadata::isPrimary)
				);
	}

	@Test
	@Transactional
	@DisplayName("should create keyset metadata")
	void createKeyset(AssertablePublishedEvents events) {
		final var definition = KeysetMetadataDefinition.builder()
				.algorithm(KeysetMetadataAlgorithm.AES128_GCM)
				.name("test-keyset")
				.description("Testing keyset for konfigyr namespace")
				.tags(Set.of("test", "konfigyr"))
				.rotationInterval(Duration.ofDays(180))
				.build();

		final var keyset = manager.create(namespaceFor("konfigyr"), definition);

		assertThat(keyset)
				.returns(definition.algorithm(), KeysetMetadata::algorithm)
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns(definition.name(), KeysetMetadata::name)
				.returns(definition.description(), KeysetMetadata::description)
				.returns(definition.tags(), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				)
				.returns(null, KeysetMetadata::destroyedAt);

		events.assertThat()
				.contains(KeysetManagementEvent.Created.class)
				.matching(KeysetManagementEvent::id, keyset.id())
				.matching(KeysetManagementEvent::namespace, EntityId.from(2));
	}

	@Test
	@DisplayName("should fail to create keyset metadata for namespace that already has a keyset with given name")
	void createKeysetForDuplicateName(AssertablePublishedEvents events) {
		final var definition = KeysetMetadataDefinition.builder()
				.algorithm(KeysetMetadataAlgorithm.AES128_GCM)
				.name("konfigyr-active")
				.rotationInterval(Duration.ofDays(180))
				.build();

		assertThatExceptionOfType(KeysetExistsException.class)
				.isThrownBy(() -> manager.create(namespaceFor("konfigyr"), definition))
				.returns(definition, KeysetExistsException::getDefinition)
				.returns(HttpStatus.BAD_REQUEST, KeysetExistsException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should update keyset metadata description and replace tags")
	void updateKeysetDescription() {
		final var keyset = keysetMetadataFor("john-doe", 1);

		assertThat(manager.update(keyset, "Updated keyset description", Set.of("updated")))
				.returns(keyset.name(), KeysetMetadata::name)
				.returns(keyset.state(), KeysetMetadata::state)
				.returns("Updated keyset description", KeysetMetadata::description)
				.returns(Set.of("updated"), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should remove keyset metadata description and tags")
	void removeKeysetDescription() {
		final var keyset = keysetMetadataFor("john-doe", 1);

		assertThat(manager.update(keyset, null, null))
				.returns(keyset.name(), KeysetMetadata::name)
				.returns(keyset.state(), KeysetMetadata::state)
				.returns(null, KeysetMetadata::description)
				.returns(Set.of(), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);
	}

	@ValueSource(longs = { 3, 4, 5 })
	@DisplayName("should fail to update inactive keyset metadata")
	@ParameterizedTest(name = "update keyset metadata with identifier: {0}")
	void updateInactiveKeyset(long id) {
		final var keyset = keysetMetadataFor("konfigyr", id);

		assertThatExceptionOfType(InactiveKeysetException.class)
				.isThrownBy(() -> manager.update(keyset, null, null))
				.returns(HttpStatus.CONFLICT, InactiveKeysetException::getStatusCode);
	}

	@Test
	@Transactional
	@DisplayName("should transition active keyset metadata to inactive")
	void disableKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("john-doe");
		final var operation = KeyOperation.deactivate(EntityId.from(1), "106475");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Deactivated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(1))
				.matching(KeysetManagementEvent::namespace, EntityId.from(1))
				.matching(KeysetManagementEvent.Deactivated::key, "106475");
	}

	@Test
	@Transactional
	@DisplayName("should transition active keyset metadata to pending for destruction")
	void destroyActiveKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("john-doe");
		final var operation = KeyOperation.destroy(EntityId.from(1), "106475");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Destroyed.class)
				.matching(KeysetManagementEvent::id, EntityId.from(1))
				.matching(KeysetManagementEvent::namespace, EntityId.from(1))
				.matching(KeysetManagementEvent.Destroyed::key, "106475");
	}

	@Test
	@Transactional
	@DisplayName("should transition inactive keyset metadata to pending for destruction")
	void destroyInactiveKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.destroy(EntityId.from(3), "374108");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Destroyed.class)
				.matching(KeysetManagementEvent::id, EntityId.from(3))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2))
				.matching(KeysetManagementEvent.Destroyed::key, "374108");
	}

	@Test
	@Transactional
	@DisplayName("should transition inactive keyset metadata to active")
	void activateInactiveKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.reactivate(EntityId.from(3), "374108");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				)
				.returns(null, KeysetMetadata::destroyedAt);

		events.assertThat()
				.contains(KeysetManagementEvent.Reactivated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(3))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2))
				.matching(KeysetManagementEvent.Reactivated::key, "374108");
	}

	@Test
	@Transactional
	@DisplayName("should restore keyset metadata that is scheduled for destruction")
	void restoreDestroyedKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.restore(EntityId.from(4), "162009");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				)
				.returns(null, KeysetMetadata::destroyedAt);

		events.assertThat()
				.contains(KeysetManagementEvent.Restored.class)
				.matching(KeysetManagementEvent::id, EntityId.from(4))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2))
				.matching(KeysetManagementEvent.Restored::key, "162009");
	}

	@Test
	@Transactional
	@DisplayName("should transition keyset metadata to same state")
	void transitionKeysetToSameState(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("john-doe");
		final var operation = KeyOperation.reactivate(EntityId.from(1), "106475");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(5, ChronoUnit.MINUTES))
				);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should transition keyset metadata to a destroyed state")
	void transitionKeysetToDestroyedState(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.destroy(EntityId.from(3), "374108");

		assertThat(manager.transition(namespace, operation))
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.MINUTES))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Destroyed.class)
				.matching(KeysetManagementEvent::id, EntityId.from(3))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2))
				.matching(KeysetManagementEvent.Destroyed::key, "374108");
	}

	@Test
	@DisplayName("should fail to transition keyset metadata to an unsupported state")
	void transitionKeysetToUnsupportedState(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.reactivate(EntityId.from(4), "162009");

		assertThatExceptionOfType(KeysetTransitionException.class)
				.isThrownBy(() -> manager.transition(namespace, operation))
				.returns(HttpStatus.BAD_REQUEST, KeysetTransitionException::getStatusCode)
				.returns(EntityId.from(4), KeysetTransitionException::getKeyset)
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetTransitionException::getCurrentState)
				.returns(KeysetMetadataState.ACTIVE, KeysetTransitionException::getTargetState);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition destroyed keyset metadata")
	void transitionDestroyedKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.reactivate(EntityId.from(5), "431904");

		assertThatExceptionOfType(KeysetTransitionException.class)
				.isThrownBy(() -> manager.transition(namespace, operation))
				.returns(HttpStatus.BAD_REQUEST, KeysetTransitionException::getStatusCode)
				.returns(EntityId.from(5), KeysetTransitionException::getKeyset)
				.returns(KeysetMetadataState.DESTROYED, KeysetTransitionException::getCurrentState)
				.returns(KeysetMetadataState.ACTIVE, KeysetTransitionException::getTargetState);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition unknown keyset metadata")
	void transitionUnknownKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.deactivate(EntityId.from(9999), "some-key-id");

		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.transition(namespace, operation))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition keyset metadata that belongs to a different namespace")
	void transitionKeysetForDifferentNamespace(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");
		final var operation = KeyOperation.reactivate(EntityId.from(1), "some-key-id");

		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.transition(namespace, operation))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should rotate keyset metadata and update the keyset material")
	void rotateKeyset(AssertablePublishedEvents events) {
		assertThat(store.read("kms-john-doe-keyset"))
				.hasSize(2);

		assertThat(manager.rotate(namespaceFor("john-doe"), EntityId.from(1)))
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		assertThat(store.read("kms-john-doe-keyset"))
				.hasSize(3);

		events.assertThat()
				.contains(KeysetManagementEvent.Rotated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(1))
				.matching(KeysetManagementEvent::namespace, EntityId.from(1));
	}

	@Transactional
	@ValueSource(longs = { 3, 4, 5 })
	@DisplayName("should rotate keysets which primary keys are no longer active")
	@ParameterizedTest(name = "rotating keyset metadata with identifier: {0}")
	void rotateInactiveKeyset(long id, AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");

		assertThat(manager.rotate(namespace, EntityId.from(id)))
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Rotated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(id))
				.matching(KeysetManagementEvent::namespace, namespace.id());
	}

	@Test
	@DisplayName("should fail to rotate unknown keyset metadata")
	void rotateUnknownKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");

		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.rotate(namespace, EntityId.from(9999)))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to rotate keyset metadata that belongs to a different namespace")
	void rotateKeysetForDifferentNamespace(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");

		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.rotate(namespace, EntityId.from(1)))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should remove keyset metadata and destroy the keyset material")
	void removeKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");

		assertThatNoException()
				.isThrownBy(() -> manager.delete(namespace, EntityId.from(2)));

		assertThat(manager.get(namespace, EntityId.from(2)))
				.isEmpty();

		assertThatExceptionOfType(CryptoException.KeysetNotFoundException.class)
				.isThrownBy(() -> store.read("kms-konfigyr-active"));

		events.assertThat()
				.contains(KeysetManagementEvent.Deleted.class)
				.matching(KeysetManagementEvent::id, EntityId.from(2))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2));
	}

	@Test
	@DisplayName("should fail to delete unknown keyset metadata")
	void removeUnknownKeyset(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");

		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.delete(namespace, EntityId.from(9999)))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to delete keyset metadata that belongs to a different namespace")
	void removeKeysetForDifferentNamespace(AssertablePublishedEvents events) {
		final var namespace = namespaceFor("konfigyr");

		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.delete(namespace, EntityId.from(1)))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
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

}
