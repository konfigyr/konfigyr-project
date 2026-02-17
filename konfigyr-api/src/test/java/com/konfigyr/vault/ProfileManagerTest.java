package com.konfigyr.vault;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

class ProfileManagerTest extends AbstractIntegrationTest {

	@Autowired
	Services services;

	@Autowired
	ProfileManager profiles;

	@Test
	@DisplayName("should search profiles for 'konfigyr-id' service")
	void shouldFindNamespaces() {
		final var service = services.get(EntityId.from(2)).orElseThrow();

		final var query = SearchQuery.builder()
				.build();

		assertThat(profiles.find(service, query))
				.isNotNull()
				.hasSize(3)
				.extracting(Profile::id, Profile::slug, Profile::name)
				.containsExactlyInAnyOrder(
						tuple(EntityId.from(1), "development", "Development"),
						tuple(EntityId.from(2), "staging", "Staging"),
						tuple(EntityId.from(3), "production", "Prod")
				);
	}

	@Test
	@DisplayName("should search profiles for 'konfigyr-id' service with search term")
	void shouldProfilesBySearchTerm() {
		final var service = services.get(EntityId.from(2)).orElseThrow();

		final var query = SearchQuery.builder()
				.term("prod")
				.build();

		assertThat(profiles.find(service, query))
				.isNotNull()
				.hasSize(1)
				.extracting(Profile::id)
				.containsExactlyInAnyOrder(EntityId.from(3));
	}

	@Test
	@DisplayName("should lookup profile by identifier")
	void shouldLookupByIdentifier() {
		assertThat(profiles.get(EntityId.from(3)))
				.isPresent()
				.get()
				.returns(EntityId.from(3), Profile::id)
				.returns(EntityId.from(2), Profile::service)
				.returns("production", Profile::slug)
				.returns("Prod", Profile::name)
				.returns("Careful!", Profile::description)
				.returns(3, Profile::position)
				.returns(ProfilePolicy.PROTECTED, Profile::policy)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(1), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should fail to retrieve profile by unknown identifier")
	void shouldLookupByUnknownIdentifier() {
		assertThat(profiles.get(EntityId.from(9999)))
				.isEmpty();
	}

	@Test
	@DisplayName("should lookup profile by name and service it belongs to")
	void shouldLookupByName() {
		final var service = services.get(EntityId.from(2)).orElseThrow();

		assertThat(profiles.get(service, "staging"))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Profile::id)
				.returns(EntityId.from(2), Profile::service)
				.returns("staging", Profile::slug)
				.returns("Staging", Profile::name)
				.returns(null, Profile::description)
				.returns(ProfilePolicy.PROTECTED, Profile::policy)
				.returns(2, Profile::position)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(3), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(1, ChronoUnit.HOURS))
				);
	}

	@Test
	@DisplayName("should fail to retrieve profile by name that does not exists for service")
	void shouldLookupByUnknownName() {
		final var service = services.get(EntityId.from(1)).orElseThrow();

		assertThat(profiles.get(service, "staging"))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should create profile from definition")
	void shouldCreateProfile(AssertablePublishedEvents events) {
		final var definition = ProfileDefinition.builder()
				.service(2)
				.slug("integration")
				.name("Integration")
				.description("QA Integration profile")
				.policy(ProfilePolicy.PROTECTED)
				.build();

		final var profile = profiles.create(definition);
		assertThat(profile.id())
				.isNotNull();

		assertThat(profile)
				.returns(definition.name(), Profile::name)
				.returns(definition.service(), Profile::service)
				.returns(definition.slug().get(), Profile::slug)
				.returns(definition.description(), Profile::description)
				.returns(definition.policy(), Profile::policy)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(ProfileEvent.Created.class)
				.matching(ProfileEvent::get, profile)
				.matching(EntityEvent::id, profile.id());
	}

	@Test
	@DisplayName("should fail to create profile for unknown service")
	void shouldCreateProfileForUnknownService(AssertablePublishedEvents events) {
		final var definition = ProfileDefinition.builder()
				.service(9999)
				.slug("integration")
				.policy(ProfilePolicy.IMMUTABLE)
				.build();

		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> profiles.create(definition))
				.withNoCause();

		assertThat(events.eventOfTypeWasPublished(ProfileEvent.class))
				.isFalse();
	}

	@Test
	@DisplayName("should fail to create profile with name that already exists")
	void shouldCreateProfileWithExistingName(AssertablePublishedEvents events) {
		final var definition = ProfileDefinition.builder()
				.service(2)
				.slug("production")
				.policy(ProfilePolicy.PROTECTED)
				.build();

		assertThatExceptionOfType(ProfileExistsException.class)
				.isThrownBy(() -> profiles.create(definition))
				.returns(definition, ProfileExistsException::getDefinition)
				.withCauseInstanceOf(DuplicateKeyException.class);

		assertThat(events.eventOfTypeWasPublished(ProfileEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should update profile")
	void shouldUpdateProfile(AssertablePublishedEvents events) {
		final var definition = ProfileDefinition.builder()
				.service(2)
				.slug("development (ignored for updates)")
				.name("Development profile")
				.description("Development profile, it is now protected!")
				.policy(ProfilePolicy.PROTECTED)
				.position(10)
				.build();

		final var profile = profiles.update(EntityId.from(1), definition);

		assertThat(profile)
				.returns(EntityId.from(1), Profile::id)
				.returns(definition.service(), Profile::service)
				.returns("development", Profile::slug)
				.returns(definition.name(), Profile::name)
				.returns(definition.description(), Profile::description)
				.returns(definition.policy(), Profile::policy)
				.returns(definition.position(), Profile::position)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now().minusDays(5), within(1, ChronoUnit.HOURS))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(ProfileEvent.Updated.class)
				.matching(ProfileEvent::get, profile)
				.matching(EntityEvent::id, profile.id());
	}

	@Test
	@DisplayName("should fail to update unknown profile")
	void shouldUpdateUnknownProfile(AssertablePublishedEvents events) {
		final var definition = ProfileDefinition.builder()
				.service(2)
				.slug( "development (ignored for updates)")
				.name("Dev")
				.description("Development profile, unprotected")
				.policy(ProfilePolicy.PROTECTED)
				.build();

		assertThatExceptionOfType(ProfileNotFoundException.class)
				.isThrownBy(() -> profiles.update(EntityId.from(9999), definition))
				.withNoCause();

		assertThat(events.eventOfTypeWasPublished(ProfileEvent.class))
				.isFalse();
	}

	@Test
	@Transactional
	@DisplayName("should delete profile by identifier")
	void shouldDeleteProfileById(AssertablePublishedEvents events) {
		final var id = EntityId.from(1);

		assertThatNoException().isThrownBy(() -> profiles.delete(id));

		assertThat(profiles.get(id))
				.isEmpty();

		events.assertThat()
				.contains(ProfileEvent.Deleted.class)
				.matching(EntityEvent::id, id);
	}

	@Test
	@DisplayName("should fail to delete unknown service by identifier")
	void shouldDeleteUnknownServiceById(AssertablePublishedEvents events) {
		assertThatExceptionOfType(ProfileNotFoundException.class)
				.isThrownBy(() -> profiles.delete(EntityId.from(9999)))
				.withNoCause();

		assertThat(events.ofType(ProfileEvent.class))
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should delete profile by service and name")
	void shouldDeleteProfileByName(AssertablePublishedEvents events) {
		final var service = services.get(EntityId.from(1L)).orElseThrow();

		assertThatNoException().isThrownBy(() -> profiles.delete(service, "live"));

		assertThat(profiles.get(service, "live"))
				.isEmpty();

		events.assertThat()
				.contains(ProfileEvent.Deleted.class)
				.matching(EntityEvent::id, EntityId.from(4));
	}

	@Test
	@DisplayName("should fail to delete profile by name that belongs to a different service")
	void shouldDeleteInvalidProfileByName(AssertablePublishedEvents events) {
		final var service = services.get(EntityId.from(1L)).orElseThrow();

		assertThatExceptionOfType(ProfileNotFoundException.class)
				.isThrownBy(() -> profiles.delete(service, "development"))
				.withNoCause();

		assertThat(events.ofType(ProfileEvent.class))
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to delete profile by name that does not exist")
	void shouldDeleteUnknownProfileByName(AssertablePublishedEvents events) {
		final var service = services.get(EntityId.from(1L)).orElseThrow();

		assertThatExceptionOfType(ProfileNotFoundException.class)
				.isThrownBy(() -> profiles.delete(service, "unknown"))
				.withNoCause();

		assertThat(events.ofType(ProfileEvent.class))
				.isEmpty();
	}

}
