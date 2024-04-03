package com.konfigyr.namespace;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NamespaceTestConfiguration.class)
class NamespaceManagerTest {

	@Autowired
	NamespaceManager manager;

	@Test
	@DisplayName("should lookup namespace by entity identifier")
	void shouldLookupNamespaceById() {
		final var id = EntityId.from(1);

		assertThat(manager.findById(id))
				.isPresent()
				.get()
				.returns(id, Namespace::id)
				.returns("john-doe", Namespace::slug)
				.returns(NamespaceType.PERSONAL, Namespace::type)
				.returns("John Doe", Namespace::name)
				.returns("Personal namespace for John Doe", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(5))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(1))
				);
	}

	@Test
	@DisplayName("should lookup namespace by slug path")
	void shouldLookupNamespaceBySlug() {
		final var slug = "konfigyr";

		assertThat(manager.findBySlug(slug))
				.isPresent()
				.get()
				.returns(EntityId.from(2), Namespace::id)
				.returns(slug, Namespace::slug)
				.returns(NamespaceType.TEAM, Namespace::type)
				.returns("Konfigyr", Namespace::name)
				.returns("Konfigyr namespace", Namespace::description)
				.satisfies(it -> assertThat(it.createdAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(3))
				)
				.satisfies(it -> assertThat(it.updatedAt())
						.isNotNull()
						.isEqualToIgnoringHours(OffsetDateTime.now().minusDays(1))
				);
	}

	@Test
	@DisplayName("should check if namespaces exists by slug path")
	void shouldCheckIfNamespaceExists() {
		assertThat(manager.exists("john-doe")).isTrue();
		assertThat(manager.exists("konfigyr")).isTrue();
		assertThat(manager.exists("unknown")).isFalse();
	}

	@Test
	@DisplayName("should return empty optional when namespace is not found by entity identifier")
	void shouldFailToLookupNamespaceById() {
		assertThat(manager.findById(EntityId.from(991827464))).isEmpty();
	}

	@Test
	@DisplayName("should return empty optional when namespace is not found by path slug")
	void shouldFailToLookupNamespaceByEmail() {
		assertThat(manager.findBySlug("unknown")).isEmpty();
	}

}