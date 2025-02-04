package com.konfigyr.hateoas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PagedModelTest {

	final EntityModel<String> foo = EntityModel.of("foo");
	final EntityModel<String> bar = EntityModel.of("bar");

	PagedModel<Object> resources;

	@BeforeEach
	void setup() {
		resources = PagedModel.of(Page.empty());
	}

	@Test
	@DisplayName("should create empty page models")
	void emptyPagedModels() {
		PagedModel<EntityModel<String>> empty = PagedModel.empty();

		assertThat(empty)
				.isEqualTo(PagedModel.empty(Collections.emptyList()))
				.hasSameHashCodeAs(PagedModel.empty(Collections.emptyList()));
	}

	@Test
	@DisplayName("should create paged models from page and links")
	void createPagedModel() {
		PagedModel<EntityModel<String>> model = PagedModel.of(createPage(5, foo, bar), Link.of("localhost"));

		assertThat(model)
				.hasSize(2)
				.containsExactly(foo, bar)
				.hasToString("PagedModel(content=[%s, %s], metadata=%s, links=[%s])",
						foo, bar, model.getMetadata(), Link.of("localhost"));

		assertThat(model.hasLinks())
				.isTrue();

		assertThat(model.getMetadata())
				.returns(2L, PagedModel.PageMetadata::size)
				.returns(0L, PagedModel.PageMetadata::number)
				.returns(5L, PagedModel.PageMetadata::totalElements)
				.returns(3L, PagedModel.PageMetadata::totalPages);
	}

	@Test
	@DisplayName("paged models with same content should be considered equal")
	void equalsWithEqualContent() {
		PagedModel<EntityModel<String>> left = PagedModel.of(createPage(foo));
		PagedModel<EntityModel<String>> right = PagedModel.of(createPage(foo));

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
		PagedModel<EntityModel<String>> left = PagedModel.of(createPage(foo));
		PagedModel<EntityModel<String>> right = PagedModel.of(createPage(bar));

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
		PagedModel<EntityModel<String>> left = PagedModel.of(createPage(foo));
		PagedModel<EntityModel<String>> right = PagedModel.of(createPage(foo), Link.of("localhost"));

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
	@DisplayName("should prevent negative metadata page size")
	void preventsNegativePageSize() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PagedModel.PageMetadata(-1, 0, 0, 0));
	}

	@Test
	@DisplayName("should prevent negative metadata page number")
	void preventsNegativePageNumber() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PagedModel.PageMetadata(0, -1, 0, 0));
	}

	@Test
	@DisplayName("should prevent negative metadata total elements")
	void preventsNegativeTotalElements() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PagedModel.PageMetadata(0, 0, -1, 0));
	}

	@Test
	@DisplayName("should prevent negative metadata total pages")
	void preventsNegativeTotalPages() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PagedModel.PageMetadata(0, 0, 0, -1));
	}

	@SafeVarargs
	static Page<EntityModel<String>> createPage(EntityModel<String>... contents) {
		return createPage(contents.length, contents);
	}

	@SafeVarargs
	static Page<EntityModel<String>> createPage(long total, EntityModel<String>... contents) {
		return new PageImpl<>(List.of(contents), Pageable.ofSize(contents.length), total);
	}

}
