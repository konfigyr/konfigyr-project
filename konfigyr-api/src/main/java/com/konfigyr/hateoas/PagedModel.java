package com.konfigyr.hateoas;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.data.domain.Page;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation model that contains a pageable collection of entities.
 *
 * @param <T> the domain object type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@JsonPropertyOrder({ "data", "metadata", "links" })
public class PagedModel<T> extends CollectionModel<T> {

	private static final PagedModel<?> EMPTY = new PagedModel<>();

	private final PageMetadata metadata;

	protected PagedModel() {
		this(Collections.emptyList(), null);
	}

	protected PagedModel(Collection<T> content, @Nullable PageMetadata metadata) {
		this(content, metadata, Collections.emptyList());
	}

	@JsonCreator
	protected PagedModel(
			@JsonProperty("data") Collection<T> content,
			@JsonProperty("metadata") @Nullable PageMetadata metadata,
			@JsonProperty("links") Iterable<Link> links
	) {
		super(content, links);

		this.metadata = metadata;
	}

	/**
	 * Creates an empty {@link PagedModel}.
	 *
	 * @param <T> generic paged model type
	 * @return an empty paged model, never {@literal null}.
	 */
	@NonNull
	@SuppressWarnings("unchecked")
	public static <T> PagedModel<T> empty() {
		return (PagedModel<T>) EMPTY;
	}

	/**
	 * Creates an empty {@link PagedModel} with the given links.
	 *
	 * @param <T> generic paged model type
	 * @param links must not be {@literal null}.
	 * @return an empty paged model, never {@literal null}.
	 */
	@NonNull
	public static <T> PagedModel<T> empty(Link... links) {
		return empty(Arrays.asList(links));
	}

	/**
	 * Creates an empty {@link PagedModel} with the given links.
	 *
	 * @param <T> generic paged model type
	 * @param links must not be {@literal null}.
	 * @return an empty paged model, never {@literal null}.
	 */
	@NonNull
	public static <T> PagedModel<T> empty(Iterable<Link> links) {
		return new PagedModel<>(Collections.emptyList(), null, links);
	}

	/**
	 * Creates a new {@link PagedModel} from the given page content.
	 *
	 * @param <T> generic paged model type
	 * @param content must not be {@literal null}.
	 * @return paged model representation, never {@literal null}.
	 */
	@NonNull
	public static <T> PagedModel<T> of(Page<@NonNull T> content) {
		return of(content, Collections.emptyList());
	}

	/**
	 * Creates a new {@link PagedModel} from the given content and {@link Link}s (optional).
	 *
	 * @param <T> generic paged model type
	 * @param content must not be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @return paged model representation, never {@literal null}.
	 */
	@NonNull
	public static <T> PagedModel<T> of(Page<@NonNull T> content, Link... links) {
		return of(content, Arrays.asList(links));
	}

	/**
	 * Creates a new {@link PagedModel} from the given content {@link PageMetadata} and {@link Link}s.
	 *
	 * @param <T> generic paged model type
	 * @param content must not be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @return paged model representation, never {@literal null}.
	 */
	@NonNull
	public static <T> PagedModel<T> of(Page<@NonNull T> content, Iterable<Link> links) {
		Assert.notNull(content, "Page content must not be null");
		return new PagedModel<>(content.getContent(), new PageMetadata(content), links);
	}

	/**
	 * Returns the pagination metadata.
	 *
	 * @return the metadata
	 */
	@Nullable
	@JsonProperty("metadata")
	public PageMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Returns the Link pointing to the next page (if set).
	 *
	 * @return the next link
	 */
	@JsonIgnore
	public Optional<Link> getNextLink() {
		return getLink(LinkRelation.NEXT);
	}

	/**
	 * Returns the Link pointing to the previous page (if set).
	 *
	 * @return the previous link
	 */
	@JsonIgnore
	public Optional<Link> getPreviousLink() {
		return getLink(LinkRelation.PREV);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}

		PagedModel<?> that = (PagedModel<?>) obj;

		return Objects.equals(this.metadata, that.metadata) && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(metadata);
	}

	@Override
	public String toString() {
		return String.format("PagedModel(content=%s, metadata=%s, links=%s)", getContent(), metadata, getLinks());
	}

	/**
	 * Value object for pagination metadata.
	 *
	 * @param size the requested size of the page.
	 * @param number zero-indexed page number, must be less than total pages.
	 * @param totalElements the total number of elements available.
	 * @param totalPages the total number of pages.
	 */
	public record PageMetadata(
			@JsonProperty long size,
			@JsonProperty long number,
			@JsonProperty("total") long totalElements,
			@JsonProperty("pages") long totalPages
	) {

		public PageMetadata {
			Assert.isTrue(size > -1, "Size must not be negative!");
			Assert.isTrue(number > -1, "Number must not be negative!");
			Assert.isTrue(totalElements > -1, "Total elements must not be negative!");
			Assert.isTrue(totalPages > -1, "Total pages must not be negative!");
		}

		/**
		 * Creates a new metadata instance from the given page object.
		 *
		 * @param page the page object containing elements and page metadata, can't be {@literal null}.
		 */
		public PageMetadata(@NonNull Page<?> page) {
			this(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages());
		}
	}
}
