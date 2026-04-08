package com.konfigyr.hateoas;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CursorModelTest {

	final EntityModel<String> foo = EntityModel.of("foo");
	final EntityModel<String> bar = EntityModel.of("bar");

	CursorModel<Object> resources;

	@BeforeEach
	void setup() {
		resources = CursorModel.of(CursorPage.empty());
	}

	@Test
	@DisplayName("should create empty page models")
	void emptyPagedModels() {
		CursorModel<EntityModel<String>> empty = CursorModel.empty();

		assertThat(empty)
				.isEqualTo(CursorModel.empty(Collections.emptyList()))
				.hasSameHashCodeAs(CursorModel.empty(Collections.emptyList()));
	}

	@Test
	@DisplayName("should create paged models from page and links")
	void createPagedModel() {
		final var next = CursorPageable.of("next-token", 10);
		final var previous = CursorPageable.of("previous-token", 10);

		CursorModel<EntityModel<String>> model = CursorModel.of(
				CursorPage.of(List.of(foo, bar), next, previous),
				Link.of("localhost")
		);

		assertThat(model)
				.hasSize(2)
				.containsExactly(foo, bar)
				.hasToString("CursorModel(content=[%s, %s], metadata=%s, links=[%s])",
						foo, bar, model.getMetadata(), Link.of("localhost"));

		assertThat(model.hasLinks())
				.isTrue();

		final var metadata = model.getMetadata();

		assertThat(metadata)
				.isNotNull()
				.returns(2L, CursorModel.CursorMetadata::size)
				.returns(next.token(), CursorModel.CursorMetadata::next)
				.returns(previous.token(), CursorModel.CursorMetadata::previous);
	}

	@Test
	@DisplayName("paged models with same content should be considered equal")
	void equalsWithEqualContent() {
		CursorModel<EntityModel<String>> left = CursorModel.of(createPage(foo));
		CursorModel<EntityModel<String>> right = CursorModel.of(createPage(foo));

		assertThat(left)
				.isEqualTo(right)
				.hasSameHashCodeAs(right);

		assertThat(right)
				.isEqualTo(left)
				.hasSameHashCodeAs(left);
	}

	@Test
	@DisplayName("paged models with different content should not be considered equal")
	void notEqualForDifferentContent() {
		CursorModel<EntityModel<String>> left = CursorModel.of(createPage(foo));
		CursorModel<EntityModel<String>> right = CursorModel.of(createPage(bar));

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
	@DisplayName("paged models with same content but different links should not be considered equal")
	void notEqualForDifferentLinks() {
		CursorModel<EntityModel<String>> left = CursorModel.of(createPage(foo));
		CursorModel<EntityModel<String>> right = CursorModel.of(createPage(foo), Link.of("localhost"));

		assertThat(left)
				.isNotEqualTo(right)
				.doesNotHaveSameHashCodeAs(right);

		assertThat(right)
				.isNotEqualTo(left)
				.doesNotHaveSameHashCodeAs(left);
	}

	@Test
	@DisplayName("should discover next link")
	void discoversNextLink() {
		resources.add(Link.of("/next", LinkRelation.NEXT));

		assertThat(resources.getNextLink()).isNotNull();
	}

	@Test
	@DisplayName("should discover previous link")
	void discoversPreviousLink() {
		resources.add(Link.of("/previous", LinkRelation.PREV));

		assertThat(resources.getPreviousLink()).isNotNull();
	}

	@Test
	@DisplayName("should provide empty cursor metadata")
	void emptyMetadata() {
		final var metadata = resources.getMetadata();

		assertThat(metadata)
				.isNotNull()
				.returns(0L, CursorModel.CursorMetadata::size)
				.returns(null, CursorModel.CursorMetadata::next)
				.returns(null, CursorModel.CursorMetadata::previous);
	}

	@Test
	@DisplayName("should prevent negative metadata page size")
	void preventsNegativePageSize() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CursorModel.CursorMetadata(-1, null, null));
	}

	@SafeVarargs
	static CursorPage<@NonNull EntityModel<String>> createPage(EntityModel<String>... contents) {
		return CursorPage.of(List.of(contents), null, null);
	}

}
