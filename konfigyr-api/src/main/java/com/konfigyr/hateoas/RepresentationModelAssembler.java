package com.konfigyr.hateoas;

import org.springframework.data.domain.Page;
import org.jspecify.annotations.NonNull;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Interface for components that convert a domain type into a {@link RepresentationModel}.
 *
 * @param <T> the domain object type
 * @param <R> the representation type
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see RepresentationModel
 */
@FunctionalInterface
public interface RepresentationModelAssembler<T, R extends RepresentationModel<?>> {

	/**
	 * Converts the given entity into a {@code R}, which extends {@link RepresentationModel}.
	 *
	 * @param entity entity to be converted, can't be {@literal null}
	 * @return the representation model, never {@literal null}
	 */
	@NonNull R assemble(@NonNull T entity);

	/**
	 * Converts {@link Iterable iterable of entities} into a {@link CollectionModel} instance.
	 *
	 * @param entities entities to be converted, must not be {@literal null}.
	 * @return the collection model, never {@literal null}.
	 */
	@NonNull
	default CollectionModel<R> assemble(@NonNull Iterable<? extends T> entities) {
		return StreamSupport.stream(entities.spliterator(), false)
				.map(this::assemble)
				.collect(Collectors.collectingAndThen(Collectors.toList(), CollectionModel::of));
	}

	/**
	 * Converts {@link Page page of entities} into a {@link PagedModel} instance.
	 *
	 * @param entities entities to be converted, must not be {@literal null}.
	 * @return the paged model, never {@literal null}.
	 */
	@NonNull
	default PagedModel<R> assemble(@NonNull Page<? extends @NonNull T> entities) {
		final PagedModel<R> model = PagedModel.of(entities.map(this::assemble));
		model.add(
				Link.builder()
						.query("page", 1)
						.rel(LinkRelation.FIRST),
				Link.builder()
						.query("page", entities.getTotalPages())
						.rel(LinkRelation.LAST)
		);

		if (entities.hasPrevious()) {
			model.add(Link.builder()
					.query("page", entities.previousPageable().getPageNumber() + 1)
					.rel(LinkRelation.PREVIOUS)
			);
		}

		if (entities.hasNext()) {
			model.add(Link.builder()
					.query("page", entities.nextPageable().getPageNumber() + 1)
					.rel(LinkRelation.NEXT)
			);
		}

		return model;
	}

}
