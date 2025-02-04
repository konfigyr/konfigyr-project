package com.konfigyr.hateoas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class CollectionModelTest {

	Set<EntityModel<String>> foo = Collections.singleton(EntityModel.of("foo"));
	Set<EntityModel<String>> bar = Collections.singleton(EntityModel.of("bar"));

	@Test
	@DisplayName("should create empty collection models")
	void emptyCollectionModels() {
		CollectionModel<EntityModel<String>> empty = CollectionModel.empty();

		assertThat(empty)
				.isEqualTo(CollectionModel.empty(Collections.emptyList()))
				.hasSameHashCodeAs(CollectionModel.empty(Collections.emptyList()));

		assertThat(empty)
				.isEqualTo(CollectionModel.of(Collections.emptyList(), Collections.emptySet()))
				.hasSameHashCodeAs(CollectionModel.of(Collections.emptyList(), Collections.emptySet()));
	}

	@Test
	@DisplayName("collection models with same content should be considered equal")
	void equalsWithEqualContent() {
		CollectionModel<EntityModel<String>> left = CollectionModel.of(foo);
		CollectionModel<EntityModel<String>> right = CollectionModel.of(foo);

		assertThat(left)
				.isEqualTo(right)
				.hasSameHashCodeAs(right)
				.hasToString("CollectionModel(content=%s, links=[])", foo);

		assertThat(right)
				.isEqualTo(left)
				.hasSameHashCodeAs(left)
				.hasToString("CollectionModel(content=%s, links=[])", foo);
	}

	@Test
	@DisplayName("collection models with different content should not be considered equal")
	void notEqualForDifferentContent() {
		CollectionModel<EntityModel<String>> left = CollectionModel.of(foo);
		CollectionModel<EntityModel<String>> right = CollectionModel.of(bar);

		assertThat(left)
				.containsExactly(EntityModel.of("foo"))
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right);

		assertThat(right)
				.containsExactly(EntityModel.of("bar"))
				.isNotEqualTo(left)
				.doesNotHaveSameHashCodeAs(left);
	}

	@Test
	@DisplayName("collection models with same content but different links should not be considered equal")
	void notEqualForDifferentLinks() {
		CollectionModel<EntityModel<String>> left = CollectionModel.of(foo);
		CollectionModel<EntityModel<String>> right = CollectionModel.of(foo, Link.of("localhost"));

		assertThat(left)
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right)
				.hasToString("CollectionModel(content=%s, links=[])", foo);

		assertThat(right)
				.isNotEqualTo(left)
				.doesNotHaveSameHashCodeAs(left)
				.hasToString("CollectionModel(content=%s, links=%s)", foo, right.getLinks());
	}

}
