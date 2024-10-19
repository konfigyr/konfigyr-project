package com.konfigyr.namespace;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = NamespaceTestConfiguration.class)
@ExtendWith(PublishedEventsExtension.class)
class NamespaceSettingsServiceTest {

	@Autowired
	NamespaceManager manager;

	@Autowired
	NamespaceSettingsService service;

	@Test
	@Transactional
	@DisplayName("should update namespace name")
	void shouldUpdateName() {
		assertThatNoException().isThrownBy(() -> service.name("john-doe", "Updated name"));

		assertThat(manager.findById(EntityId.from(1)))
				.isPresent()
				.get()
				.returns("Updated name", Namespace::name)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should update namespace description")
	void shouldUpdateDescription() {
		assertThatNoException().isThrownBy(() -> service.description("john-doe", null));

		assertThat(manager.findById(EntityId.from(1)))
				.isPresent()
				.get()
				.returns(null, Namespace::description)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThatNoException().isThrownBy(() -> service.description("john-doe", "  "));

		assertThat(manager.findById(EntityId.from(1)))
				.isPresent()
				.get()
				.returns(null, Namespace::description)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThatNoException().isThrownBy(() -> service.description("john-doe", "Description"));

		assertThat(manager.findById(EntityId.from(1)))
				.isPresent()
				.get()
				.returns("Description", Namespace::description)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should update namespace slug")
	void shouldUpdateSlug(AssertablePublishedEvents events) {
		assertThatNoException().isThrownBy(() -> service.slug("john-doe", Slug.slugify("jane-doe")));

		assertThat(manager.findById(EntityId.from(1)))
				.isPresent()
				.get()
				.returns("jane-doe", Namespace::slug)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		events.assertThat()
				.contains(NamespaceEvent.Renamed.class)
				.matching(NamespaceEvent.Renamed::id, EntityId.from(1))
				.matching(NamespaceEvent.Renamed::from, Slug.slugify("john-doe"))
				.matching(NamespaceEvent.Renamed::to, Slug.slugify("jane-doe"));
	}

	@Test
	@DisplayName("should fail to update slug for unknown namespace")
	void shouldFailToUpdateSlug(AssertablePublishedEvents events) {
		assertThatThrownBy(() -> service.slug("unknown", Slug.slugify("jane-doe")))
				.isInstanceOf(NamespaceNotFoundException.class);

		assertThat(events.eventOfTypeWasPublished(NamespaceEvent.Renamed.class))
				.isFalse();
	}

}
