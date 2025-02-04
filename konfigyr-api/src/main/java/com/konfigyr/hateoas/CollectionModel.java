package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Representation model that contains a collection of entities.
 *
 * @param <T> the domain object type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class CollectionModel<T> extends RepresentationModel<CollectionModel<T>> implements Iterable<T> {

	private final Collection<T> content;

	protected CollectionModel(Iterable<T> content, Iterable<Link> links) {
		Assert.notNull(content, "Content must not be null!");
		Assert.notNull(links, "Links must not be null!");

		this.content = new ArrayList<>();

		for (T element : content) {
			this.content.add(element);
		}

		this.add(links);
	}

	/**
	 * Creates a new empty collection model.
	 *
	 * @param <T> generic collection model type
	 * @return an empty collection model, never {@literal null}
	 */
	@NonNull
	public static <T> CollectionModel<T> empty() {
		return of(Collections.emptyList());
	}

	/**
	 * Creates a new empty collection model with the given links.
	 *
	 * @param <T> generic collection model type
	 * @param links must not be {@literal null}.
	 * @return an empty collection model, never {@literal null}
	 */
	@NonNull
	public static <T> CollectionModel<T> empty(Link... links) {
		return of(Collections.emptyList(), links);
	}

	/**
	 * Creates a new empty collection model with the given links.
	 *
	 * @param <T> generic collection model type
	 * @param links must not be {@literal null}.
	 * @return an empty collection model, never {@literal null}
	 */
	@NonNull
	public static <T> CollectionModel<T> empty(Iterable<Link> links) {
		return of(Collections.emptyList(), links);
	}

	/**
	 * Creates a {@link CollectionModel} instance with the given content.
	 *
	 * @param content must not be {@literal null}.
	 * @param <T> the domain object type
	 * @return the collection model, never {@literal null}
	 */
	@NonNull
	public static <T> CollectionModel<T> of(Iterable<T> content) {
		return of(content, Collections.emptyList());
	}

	/**
	 * Creates a {@link CollectionModel} instance with the given content and {@link Link}s (optional).
	 *
	 * @param content must not be {@literal null}.
	 * @param links the links to be added to the {@link CollectionModel}.
	 * @param <T> the domain object type
	 * @return the collection model, never {@literal null}
	 */
	@NonNull
	public static <T> CollectionModel<T> of(Iterable<T> content, Link... links) {
		return of(content, Arrays.asList(links));
	}

	/**
	 * s Creates a {@link CollectionModel} instance with the given content and {@link Link}s.
	 *
	 * @param content must not be {@literal null}.
	 * @param links the links to be added to the {@link CollectionModel}.
	 * @param <T> the domain object type
	 * @return the collection model, never {@literal null}
	 */
	@NonNull
	public static <T> CollectionModel<T> of(Iterable<T> content, Iterable<Link> links) {
		return new CollectionModel<>(content, links);
	}

	/**
	 * Returns the underlying elements of this representation.
	 *
	 * @return the content will never be {@literal null}.
	 */
	@NonNull
	@JsonProperty("data")
	public Collection<T> getContent() {
		return Collections.unmodifiableCollection(content);
	}

	@NonNull
	@Override
	public Iterator<T> iterator() {
		return content.iterator();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}

		CollectionModel<?> that = (CollectionModel<?>) obj;

		return Objects.equals(this.content, that.content) && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + Objects.hash(content);
	}

	@Override
	public String toString() {
		return String.format("CollectionModel(content=%s, %s)", getContent(), super.toString());
	}

}
