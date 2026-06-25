package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerResolverTest extends AbstractIntegrationTest {

	@Autowired
	OwnerResolver ownerResolver;

	@Test
	@DisplayName("should resolve owner for the given namespace slug")
	void shouldResolveOwnerBySlug() {
		assertThat(ownerResolver.findOwner("konfigyr"))
				.isPresent()
				.get()
				.returns(EntityId.from(2L), Owner::id)
				.returns("konfigyr", Owner::slug);
	}

	@Test
	@DisplayName("should resolve owner for the given namespace id")
	void shouldResolveOwnerById() {
		assertThat(ownerResolver.findOwner(EntityId.from(2L)))
				.isPresent()
				.get()
				.returns(EntityId.from(2L), Owner::id)
				.returns("konfigyr", Owner::slug);
	}

	@Test
	@DisplayName("should return empty result for an unknown namespace slug")
	void shouldReturnEmptyForUnknownSlug() {
		assertThat(ownerResolver.findOwner("unknown-namespace")).isEmpty();
	}

	@Test
	@DisplayName("should return empty result for an unknown namespace id")
	void shouldReturnEmptyForUnknownId() {
		assertThat(ownerResolver.findOwner(EntityId.from(9999L))).isEmpty();
	}

}
