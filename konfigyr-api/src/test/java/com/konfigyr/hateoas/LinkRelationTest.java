package com.konfigyr.hateoas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LinkRelationTest {

	@Test
	@DisplayName("should create link relation")
	void shouldCreateRelation() {
		assertThat(LinkRelation.of("related"))
				.isEqualTo(LinkRelation.RELATED)
				.hasSameHashCodeAs(LinkRelation.RELATED)
				.hasToString("LinkRelation(%s)", "related");
	}

	@Test
	@DisplayName("should create link relation using lower case values")
	void shouldCreateRelationWithLowercaseValues() {
		assertThat(LinkRelation.of("RelAteD"))
				.isEqualTo(LinkRelation.RELATED)
				.hasSameHashCodeAs(LinkRelation.RELATED)
				.hasToString("LinkRelation(%s)", "related");
	}

	@Test
	@DisplayName("should fail to create link relation using null or blank values")
	void shouldFailToCreateRelation() {
		assertThatThrownBy(() -> LinkRelation.of(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Link relation value must not be blank");

		assertThatThrownBy(() -> LinkRelation.of(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Link relation value must not be blank");

		assertThatThrownBy(() -> LinkRelation.of(" "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Link relation value must not be blank");
	}
}
