package com.konfigyr.kms;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeysetStore;
import com.konfigyr.crypto.tink.TinkAlgorithm;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
	KeysetManager manager;

	@Autowired
	KeysetStore store;

	@Test
	@DisplayName("should search for keysets by namespace")
	void searchByNamespace() {
		final var query = SearchQuery.builder()
				.criteria(SearchQuery.NAMESPACE, "konfigyr")
				.build();

		final var metadata = manager.find(query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(4, Page::getNumberOfElements)
				.returns(1, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(4)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(2), KeysetMetadata::id)
								.returns(TinkAlgorithm.AES256_GCM.name(), KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
								.returns("konfigyr-active", KeysetMetadata::name)
								.returns("Active keyset", KeysetMetadata::description)
								.returns(Set.of("encryption", "konfigyr"), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(5), KeysetMetadata::id)
								.returns(TinkAlgorithm.AES128_GCM.name(), KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.DESTROYED, KeysetMetadata::state)
								.returns("konfigyr-destroyed", KeysetMetadata::name)
								.returns("Destroyed keyset", KeysetMetadata::description)
								.returns(Set.of("destroyed", "konfigyr"), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(4), KeysetMetadata::id)
								.returns(TinkAlgorithm.ED25519.name(), KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
								.returns("konfigyr-deleted", KeysetMetadata::name)
								.returns("Pending removal keyset", KeysetMetadata::description)
								.returns(Set.of(), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(3), KeysetMetadata::id)
								.returns(TinkAlgorithm.ECDSA_P256.name(), KeysetMetadata::algorithm)
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
				.criteria(KeysetMetadata.ALGORITHM_CRITERIA, TinkAlgorithm.AES128_GCM.name())
				.criteria(KeysetMetadata.STATE_CRITERIA, KeysetMetadataState.ACTIVE)
				.build();

		final var metadata = manager.find(query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(1, Page::getNumberOfElements)
				.returns(1, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(1)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(1), KeysetMetadata::id)
								.returns(TinkAlgorithm.AES128_GCM.name(), KeysetMetadata::algorithm)
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

		final var metadata = manager.find(query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(1, Page::getNumberOfElements)
				.returns(1, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(1)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(3), KeysetMetadata::id)
								.returns(TinkAlgorithm.ECDSA_P256.name(), KeysetMetadata::algorithm)
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

		final var metadata = manager.find(query);

		assertThatObject(metadata)
				.isNotNull()
				.returns(2, Page::getNumberOfElements)
				.returns(2, Page::getTotalPages);

		assertThat(metadata)
				.hasSize(2)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(EntityId.from(2), KeysetMetadata::id)
								.returns(TinkAlgorithm.AES256_GCM.name(), KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
								.returns("konfigyr-active", KeysetMetadata::name)
								.returns("Active keyset", KeysetMetadata::description)
								.returns(Set.of("encryption", "konfigyr"), KeysetMetadata::tags),
						it -> assertThat(it)
								.returns(EntityId.from(4), KeysetMetadata::id)
								.returns(TinkAlgorithm.ED25519.name(), KeysetMetadata::algorithm)
								.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
								.returns("konfigyr-deleted", KeysetMetadata::name)
								.returns("Pending removal keyset", KeysetMetadata::description)
								.returns(Set.of(), KeysetMetadata::tags)
				);
	}

	@Test
	@DisplayName("should retrieve keyset metadata by identifier")
	void retrieveKeyset() {
		assertThat(manager.get(EntityId.from(1)))
				.isNotEmpty()
				.get()
				.returns(EntityId.from(1), KeysetMetadata::id)
				.returns(TinkAlgorithm.AES128_GCM.name(), KeysetMetadata::algorithm)
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
		assertThat(manager.get(EntityId.from(9999)))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve keyset metadata by namespace")
	void retrieveKeysetByNamespace() {
		assertThat(manager.get(EntityId.from(2), EntityId.from(5)))
				.isNotEmpty()
				.get()
				.returns(EntityId.from(5), KeysetMetadata::id)
				.returns(TinkAlgorithm.AES128_GCM.name(), KeysetMetadata::algorithm)
				.returns(KeysetMetadataState.DESTROYED, KeysetMetadata::state)
				.returns("konfigyr-destroyed", KeysetMetadata::name)
				.returns("Destroyed keyset", KeysetMetadata::description)
				.returns(Set.of("destroyed", "konfigyr"), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(10), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(8), within(5, ChronoUnit.MINUTES))
				)
				.satisfies(it -> assertThat(it.destroyedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(7), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should retrieve an unknown keyset metadata by namespace")
	void retrieveUnknownKeysetByNamespace() {
		assertThat(manager.get(EntityId.from(1), EntityId.from(9999)))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to retrieve a keyset metadata that belongs to a different namespace")
	void retrieveKeysetForDifferentNamespace() {
		assertThat(manager.get(EntityId.from(1), EntityId.from(5)))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve keyset operations by identifier")
	void retrieveKeysetOperations() {
		final var operations = manager.operations(EntityId.from(1));

		assertThat(operations)
				.isNotNull();

		assertThatNoException()
				.isThrownBy(() -> operations.encrypt(ByteArray.fromString("data")));
	}

	@Test
	@DisplayName("should failed to retrieve keyset operations by identifier for unknown keyset")
	void retrieveUnknownKeysetOperations() {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.operations(EntityId.from(9999)));
	}

	@Test
	@DisplayName("should retrieve keyset operations by namespace")
	void retrieveKeysetOperationsByNamespace() {
		final var operations = manager.operations(EntityId.from(1), EntityId.from(1));

		assertThat(operations)
				.isNotNull();

		assertThatNoException()
				.isThrownBy(() -> operations.encrypt(ByteArray.fromString("data")));
	}

	@Test
	@DisplayName("should fail to retrieve keyset operations for a non-active keyset")
	void retrieveInactiveKeysetOperations() {
		assertThatExceptionOfType(InactiveKeysetException.class)
				.isThrownBy(() -> manager.operations(EntityId.from(2), EntityId.from(5)));
	}

	@Test
	@DisplayName("should fail to retrieve keyset operations for keyset that belongs to a different namespace")
	void retrieveKeysetOperationsForDifferentNamespace() {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.operations(EntityId.from(1), EntityId.from(5)));
	}

	@Test
	@Transactional
	@DisplayName("should create keyset metadata")
	void createKeyset(AssertablePublishedEvents events) {
		final var definition = KeysetMetadataDefinition.builder()
				.namespace(EntityId.from(2))
				.algorithm(KeysetMetadataAlgorithm.AES128_GCM)
				.name("test-keyset")
				.description("Testing keyset for konfigyr namespace")
				.tags(Set.of("test", "konfigyr"))
				.rotationInterval(Duration.ofDays(180))
				.build();

		final var keyset = manager.create(definition);

		assertThat(keyset)
				.returns(definition.algorithm().name(), KeysetMetadata::algorithm)
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
	@DisplayName("should fail to create keyset metadata for unknown namespace")
	void createKeysetForUnknownNamespace(AssertablePublishedEvents events) {
		final var definition = KeysetMetadataDefinition.builder()
				.namespace(9999L)
				.algorithm(KeysetMetadataAlgorithm.AES128_GCM)
				.name("test-keyset")
				.build();

		assertThatExceptionOfType(NamespaceNotFoundException.class)
				.isThrownBy(() -> manager.create(definition));

		assertThatExceptionOfType(CryptoException.KeysetNotFoundException.class)
				.isThrownBy(() -> store.read(definition.toKeysetDefinition().getName()));

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to create keyset metadata for namespace that already has a keyset with given name")
	void createKeysetForDuplicateName(AssertablePublishedEvents events) {
		final var definition = KeysetMetadataDefinition.builder()
				.namespace(EntityId.from(2))
				.algorithm(KeysetMetadataAlgorithm.AES128_GCM)
				.name("konfigyr-active")
				.rotationInterval(Duration.ofDays(180))
				.build();

		assertThatExceptionOfType(KeysetExistsException.class)
				.isThrownBy(() -> manager.create(definition))
				.returns(definition, KeysetExistsException::getDefinition)
				.returns(HttpStatus.BAD_REQUEST, KeysetExistsException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should update keyset metadata description and replace tags")
	void updateKeysetDescription() {
		assertThat(manager.update(EntityId.from(1), "Updated keyset description", Set.of("updated")))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
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
		assertThat(manager.update(EntityId.from(1), null, null))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.returns(null, KeysetMetadata::description)
				.returns(Set.of(), KeysetMetadata::tags)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);
	}

	@Test
	@DisplayName("should fail to update unknown keyset metadata")
	void updateUnknownKeyset() {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.update(EntityId.from(9999), "Unknown", null))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);
	}

	@ValueSource(longs = { 3, 4, 5 })
	@DisplayName("should fail to update inactive keyset metadata")
	@ParameterizedTest(name = "update keyset metadata with identifier: {0}")
	void updateInactiveKeyset(long id) {
		assertThatExceptionOfType(InactiveKeysetException.class)
				.isThrownBy(() -> manager.update(EntityId.from(id), null, null))
				.returns(HttpStatus.CONFLICT, InactiveKeysetException::getStatusCode);
	}

	@Test
	@Transactional
	@DisplayName("should transition active keyset metadata to inactive")
	void disableKeyset(AssertablePublishedEvents events) {
		assertThat(manager.transition(EntityId.from(1), KeysetMetadataState.INACTIVE))
				.returns(KeysetMetadataState.INACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Disabled.class)
				.matching(KeysetManagementEvent::id, EntityId.from(1))
				.matching(KeysetManagementEvent::namespace, EntityId.from(1));
	}

	@Test
	@Transactional
	@DisplayName("should transition active keyset metadata to pending for destruction")
	void destroyActiveKeyset(AssertablePublishedEvents events) {
		assertThat(manager.transition(EntityId.from(1), KeysetMetadataState.PENDING_DESTRUCTION))
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Removed.class)
				.matching(KeysetManagementEvent::id, EntityId.from(1))
				.matching(KeysetManagementEvent::namespace, EntityId.from(1));
	}

	@Test
	@Transactional
	@DisplayName("should transition inactive keyset metadata to pending for destruction")
	void destroyInactiveKeyset(AssertablePublishedEvents events) {
		assertThat(manager.transition(EntityId.from(3), KeysetMetadataState.PENDING_DESTRUCTION))
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Removed.class)
				.matching(KeysetManagementEvent::id, EntityId.from(3))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2));
	}

	@Test
	@Transactional
	@DisplayName("should transition inactive keyset metadata to active")
	void activateInactiveKeyset(AssertablePublishedEvents events) {
		assertThat(manager.transition(EntityId.from(3), KeysetMetadataState.ACTIVE))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(KeysetManagementEvent.Activated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(3))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2));
	}

	@Test
	@Transactional
	@DisplayName("should restore keyset metadata that is scheduled for destruction")
	void restoreDestroyedKeyset(AssertablePublishedEvents events) {
		assertThat(manager.transition(EntityId.from(4), KeysetMetadataState.ACTIVE))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				)
				.returns(null, KeysetMetadata::destroyedAt);

		events.assertThat()
				.contains(KeysetManagementEvent.Activated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(4))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2));
	}

	@Test
	@Transactional
	@DisplayName("should transition keyset metadata to same state")
	void transitionKeysetToSameState(AssertablePublishedEvents events) {
		assertThat(manager.transition(EntityId.from(1), KeysetMetadataState.ACTIVE))
				.returns(KeysetMetadataState.ACTIVE, KeysetMetadata::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(5, ChronoUnit.MINUTES))
				);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition keyset metadata to a destroyed state")
	void transitionKeysetToDestroyedState(AssertablePublishedEvents events) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> manager.transition(EntityId.from(1), KeysetMetadataState.DESTROYED))
				.withMessageContaining("Can not transition keyset metadata to destroyed state");

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition keyset metadata to an unsupported state")
	void transitionKeysetToUnsupportedState(AssertablePublishedEvents events) {
		assertThatExceptionOfType(KeysetTransitionException.class)
				.isThrownBy(() -> manager.transition(EntityId.from(4), KeysetMetadataState.INACTIVE))
				.returns(HttpStatus.BAD_REQUEST, KeysetTransitionException::getStatusCode)
				.returns(EntityId.from(4), KeysetTransitionException::getKeyset)
				.returns(KeysetMetadataState.PENDING_DESTRUCTION, KeysetTransitionException::getCurrentState)
				.returns(KeysetMetadataState.INACTIVE, KeysetTransitionException::getTargetState);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition destroyed keyset metadata")
	void transitionDestroyedKeyset(AssertablePublishedEvents events) {
		assertThatExceptionOfType(KeysetTransitionException.class)
				.isThrownBy(() -> manager.transition(EntityId.from(5), KeysetMetadataState.ACTIVE))
				.returns(HttpStatus.BAD_REQUEST, KeysetTransitionException::getStatusCode)
				.returns(EntityId.from(5), KeysetTransitionException::getKeyset)
				.returns(KeysetMetadataState.DESTROYED, KeysetTransitionException::getCurrentState)
				.returns(KeysetMetadataState.ACTIVE, KeysetTransitionException::getTargetState);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to transition destroyed keyset metadata")
	void transitionUnknownKeyset(AssertablePublishedEvents events) {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.transition(EntityId.from(9999), KeysetMetadataState.ACTIVE))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should rotate keyset metadata and update the keyset material")
	void rotateKeyset(AssertablePublishedEvents events) {
		assertThat(store.read("kms-john-doe-keyset"))
				.hasSize(1);

		assertThat(manager.rotate(EntityId.from(1)))
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS))
				);

		assertThat(store.read("kms-john-doe-keyset"))
				.hasSize(2);

		events.assertThat()
				.contains(KeysetManagementEvent.Rotated.class)
				.matching(KeysetManagementEvent::id, EntityId.from(1))
				.matching(KeysetManagementEvent::namespace, EntityId.from(1));
	}

	@ValueSource(longs = { 3, 4, 5 })
	@DisplayName("should fail to rotate inactive keyset metadata")
	@ParameterizedTest(name = "rotating keyset metadata with identifier: {0}")
	void rotateInactiveKeyset(long id, AssertablePublishedEvents events) {
		assertThatExceptionOfType(InactiveKeysetException.class)
				.isThrownBy(() -> manager.rotate(EntityId.from(id)))
				.returns(HttpStatus.CONFLICT, InactiveKeysetException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to rotate unknown keyset metadata")
	void rotateUnknownKeyset(AssertablePublishedEvents events) {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.rotate(EntityId.from(9999)))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should remove keyset metadata and destroy the keyset material")
	void removeKeyset(AssertablePublishedEvents events) {
		assertThatNoException()
				.isThrownBy(() -> manager.delete(EntityId.from(2)));

		assertThat(manager.get(EntityId.from(2)))
				.isEmpty();

		assertThatExceptionOfType(CryptoException.KeysetNotFoundException.class)
				.isThrownBy(() -> store.read("kms-konfigyr-active"));

		events.assertThat()
				.contains(KeysetManagementEvent.Destroyed.class)
				.matching(KeysetManagementEvent::id, EntityId.from(2))
				.matching(KeysetManagementEvent::namespace, EntityId.from(2));
	}

	@Test
	@DisplayName("should fail to delete unknown keyset metadata")
	void removeUnknownKeyset(AssertablePublishedEvents events) {
		assertThatExceptionOfType(KeysetNotFoundException.class)
				.isThrownBy(() -> manager.delete(EntityId.from(9999)))
				.returns(HttpStatus.NOT_FOUND, KeysetNotFoundException::getStatusCode);

		assertThat(events.eventOfTypeWasPublished(KeysetManagementEvent.Rotated.class))
				.isFalse();
	}

}
