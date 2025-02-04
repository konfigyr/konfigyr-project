package com.konfigyr.hateoas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

class EntityModelTest {

	@Test
	@DisplayName("entity models with same content should be considered equal")
	void equalsWithEqualContent() {
		EntityModel<String> left = EntityModel.of("foo");
		EntityModel<String> right = EntityModel.of("foo");

		assertThat(left)
				.isEqualTo(right)
				.hasSameHashCodeAs(right)
				.hasToString("EntityModel(content=foo, links=[])");

		assertThat(right)
				.isEqualTo(left)
				.hasSameHashCodeAs(left)
				.hasToString("EntityModel(content=foo, links=[])");
	}

	@Test
	@DisplayName("entity models with different content should not be considered equal")
	void notEqualForDifferentContent() {
		EntityModel<String> left = EntityModel.of("foo");
		EntityModel<String> right = EntityModel.of("bar");

		assertThat(left)
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right)
				.hasToString("EntityModel(content=foo, links=[])");

		assertThat(right)
				.isNotEqualTo(left)
				.doesNotHaveSameHashCodeAs(left)
				.hasToString("EntityModel(content=bar, links=[])");
	}

	@Test
	@DisplayName("collection models with same content but different links should not be considered equal")
	void notEqualForDifferentLinks() {
		EntityModel<String> left = EntityModel.of("foo");
		EntityModel<String> right = EntityModel.of("foo", Link.of("localhost"));

		assertThat(left)
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right)
				.hasToString("EntityModel(content=foo, links=[])");

		assertThat(right)
				.isNotEqualTo(left)
				.doesNotHaveSameHashCodeAs(left)
				.hasToString("EntityModel(content=foo, links=[%s])", Link.of("localhost"));
	}

	@Test
	@DisplayName("should reject collections as entity model content")
	void rejectsCollectionContent() {
		assertThatIllegalArgumentException().isThrownBy(() -> EntityModel.of(Collections.emptyList()));
	}

}
