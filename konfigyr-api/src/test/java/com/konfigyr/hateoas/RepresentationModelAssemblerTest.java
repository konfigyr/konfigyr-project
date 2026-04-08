package com.konfigyr.hateoas;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RepresentationModelAssemblerTest {

	final RepresentationModelAssembler<String, EntityModel<String>> assembler = EntityModel::of;

	@Test
	@DisplayName("should assemble entity into an entity model")
	void assembleToEntityModel() {
		assertThat(assembler.assemble("foo"))
				.isEqualTo(EntityModel.of("foo"));
	}

	@Test
	@DisplayName("should assemble list of entities into a collection model")
	void assembleToCollectionModel() {
		assertThat(assembler.assemble(List.of("foo", "bar")))
				.hasSize(2)
				.containsExactly(EntityModel.of("foo"), EntityModel.of("bar"));
	}

	@Test
	@DisplayName("should assemble single page of entities into a paged model")
	void assembleToSinglePagedModel() {
		final var page = new PageImpl<>(List.of("foo", "bar"), PageRequest.of(0, 10), 2);
		final PagedModel<EntityModel<String>> model = assembler.assemble(page);

		assertThat(model)
				.hasSize(2)
				.containsExactly(EntityModel.of("foo"), EntityModel.of("bar"));

		assertThat(model.getLinks())
				.hasSize(2)
				.containsExactlyInAnyOrder(
						Link.of("/?page=1", LinkRelation.FIRST),
						Link.of("/?page=1", LinkRelation.LAST)
				);
	}

	@Test
	@DisplayName("should assemble page of entities into a paged model")
	void assembleToPagedModel() {
		final var page = new PageImpl<>(List.of("foo", "bar"), PageRequest.of(2, 2), 11);
		final PagedModel<EntityModel<String>> model = assembler.assemble(page);

		assertThat(model)
				.hasSize(2)
				.containsExactly(EntityModel.of("foo"), EntityModel.of("bar"));

		assertThat(model.getLinks())
				.hasSize(4)
				.containsExactlyInAnyOrder(
						Link.of("/?page=1", LinkRelation.FIRST),
						Link.of("/?page=4", LinkRelation.NEXT),
						Link.of("/?page=2", LinkRelation.PREVIOUS),
						Link.of("/?page=6", LinkRelation.LAST)
				);
	}

	@Test
	@DisplayName("should assemble page of entities into a cursor model")
	void assembleToCursorModel() {
		final var page = CursorPage.of(List.of("foo", "bar"), CursorPageable.of("next-token", 20));
		final CursorModel<EntityModel<String>> model = assembler.assemble(page);

		assertThat(model)
				.hasSize(2)
				.containsExactly(EntityModel.of("foo"), EntityModel.of("bar"));

		assertThat(model.getLinks())
				.hasSize(1)
				.containsExactly(
						Link.of("/?token=next-token", LinkRelation.NEXT)
				);
	}

}
