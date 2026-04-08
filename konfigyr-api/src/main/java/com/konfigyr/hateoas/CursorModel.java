package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.konfigyr.data.CursorPage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Representation model that contains a cursored paged collection of entities.
 *
 * @param <T> the domain object type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@JsonPropertyOrder({ "data", "metadata", "links" })
public class CursorModel<T> extends CollectionModel<T> {

	private static final CursorModel<?> EMPTY = new CursorModel<>();

	private final CursorMetadata metadata;

	protected CursorModel() {
		this(Collections.emptyList(), null);
	}

	protected CursorModel(Collection<T> content, @Nullable CursorMetadata metadata) {
		this(content, metadata, Collections.emptyList());
	}

	@JsonCreator
	protected CursorModel(
			@JsonProperty("data") Collection<T> content,
			@JsonProperty("metadata") @Nullable CursorMetadata metadata,
			@JsonProperty("links") Iterable<Link> links
	) {
		super(content, links == null ? Collections.emptyList() : links);

		this.metadata = metadata;
	}

	/**
	 * Creates an empty {@link CursorModel}.
	 *
	 * @param <T> generic paged model type
	 * @return an empty paged model, never {@literal null}.
	 */
	@NonNull
	@SuppressWarnings("unchecked")
	public static <T> CursorModel<T> empty() {
		return (CursorModel<T>) EMPTY;
	}

	/**
	 * Creates an empty {@link CursorModel} with the given links.
	 *
	 * @param <T> generic paged model type
	 * @param links must not be {@literal null}.
	 * @return an empty paged model, never {@literal null}.
	 */
	@NonNull
	public static <T> CursorModel<T> empty(Link... links) {
		return empty(Arrays.asList(links));
	}

	/**
	 * Creates an empty {@link CursorModel} with the given links.
	 *
	 * @param <T> generic paged model type
	 * @param links must not be {@literal null}.
	 * @return an empty paged model, never {@literal null}.
	 */
	@NonNull
	public static <T> CursorModel<T> empty(Iterable<Link> links) {
		return new CursorModel<>(Collections.emptyList(), null, links);
	}

	/**
	 * Creates a new {@link CursorModel} from the given page content.
	 *
	 * @param <T> generic paged model type
	 * @param content must not be {@literal null}.
	 * @return paged model representation, never {@literal null}.
	 */
	@NonNull
	public static <T> CursorModel<T> of(CursorPage<@NonNull T> content) {
		return of(content, Collections.emptyList());
	}

	/**
	 * Creates a new {@link CursorModel} from the given content and {@link Link}s (optional).
	 *
	 * @param <T> generic paged model type
	 * @param content must not be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @return paged model representation, never {@literal null}.
	 */
	@NonNull
	public static <T> CursorModel<T> of(CursorPage<@NonNull T> content, Link... links) {
		return of(content, Arrays.asList(links));
	}

	/**
	 * Creates a new {@link CursorModel} from the given content {@link CursorMetadata} and {@link Link}s.
	 *
	 * @param <T> generic paged model type
	 * @param content must not be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @return paged model representation, never {@literal null}.
	 */
	@NonNull
	public static <T> CursorModel<T> of(CursorPage<@NonNull T> content, Iterable<Link> links) {
		Assert.notNull(content, "Page content must not be null");
		return new CursorModel<>(content.content(), new CursorMetadata(content), links);
	}

	/**
	 * Returns the pagination metadata.
	 *
	 * @return the metadata
	 */
	@Nullable
	@JsonProperty("metadata")
	public CursorMetadata getMetadata() {
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

		CursorModel<?> that = (CursorModel<?>) obj;

		return Objects.equals(this.metadata, that.metadata) && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(metadata);
	}

	@Override
	public String toString() {
		return String.format("CursorModel(content=%s, metadata=%s, links=%s)", getContent(), metadata, getLinks());
	}

	/**
	 * Value object for cursored based pagination metadata.
	 *
	 * @param size the requested size of the page.
	 * @param next the next page cursor, can be {@literal null}.
	 * @param previous the previous page cursor, can be {@literal null}.
	 */
	public record CursorMetadata(
			@JsonProperty long size,
			@JsonProperty("next") String next,
			@JsonProperty("previous") String previous
	) {

		public CursorMetadata {
			Assert.isTrue(size > -1, "Size must not be negative!");
		}

		/**
		 * Creates a new metadata instance from the given page object.
		 *
		 * @param page the page object containing elements and page metadata, can't be {@literal null}.
		 */
		public CursorMetadata(@NonNull CursorPage<?> page) {
			this(page.size(), page.hasNext() ? page.nextPageable().token() : null,
					page.hasPrevious() ? page.previousPageable().token() : null);
		}
	}
}
