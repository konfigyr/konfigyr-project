package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Base class for representation models that can contain links.
 *
 * @param <T> the domain object type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@EqualsAndHashCode
public class RepresentationModel<T extends RepresentationModel<? extends T>> {

	private final List<Link> links;

	public RepresentationModel() {
		this.links = new ArrayList<>();
	}

	public RepresentationModel(Link... links) {
		this(Arrays.asList(links));
	}

	public RepresentationModel(Iterable<Link> links) {
		Assert.notNull(links, "Links must not be null");

		this.links = new ArrayList<>();

		for (Link link : links) {
			this.links.add(link);
		}
	}

	/**
	 * Creates a new {@link RepresentationModel} for the given content object and no links.
	 *
	 * @param object can be {@literal null}.
	 * @param <T> the domain object type
	 * @return representation model from the given content, never {@literal null}.
	 * @see #of(Object, Iterable)
	 */
	public static <T> RepresentationModel<?> of(@Nullable T object) {
		return of(object, Collections.emptyList());
	}

	/**
	 * Creates a new {@link RepresentationModel} for the given content object and no links.
	 *
	 * @param object can be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @param <T> the domain object type
	 * @return representation model from the given content, never {@literal null}.
	 * @see #of(Object, Iterable)
	 */
	public static <T> RepresentationModel<?> of(@Nullable T object, Link... links) {
		return of(object, Arrays.asList(links));
	}

	/**
	 * Creates a new {@link RepresentationModel} for the given content object and links. Will return a simple
	 * {@link RepresentationModel} if the content is {@literal null}, a {@link CollectionModel} in case the given content
	 * object is a {@link Collection} or an {@link EntityModel} otherwise.
	 *
	 * @param object can be {@literal null}.
	 * @param links must not be {@literal null}.
	 * @param <T> the domain object type
	 * @return representation model from the given content, never {@literal null}.
	 */
	public static <T> RepresentationModel<?> of(@Nullable T object, Iterable<Link> links) {
		return switch (object) {
			case null -> new RepresentationModel<>(links);
			case Page<?> page -> PagedModel.of(page, links);
			case Collection<?> collection -> CollectionModel.of(collection, links);
			default -> EntityModel.of(object, links);
		};
	}

	/**
	 * Adds the given link to the resource.
	 *
	 * @param link must not be {@literal null}.
	 * @return the representation model, never {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public T add(Link link) {
		Assert.notNull(link, "Link must not be null!");

		this.links.add(link);

		return (T) this;
	}

	/**
	 * Adds all given {@link Link}s to the resource.
	 *
	 * @param links must not be {@literal null}.
	 * @return the representation model, never {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public T add(Iterable<Link> links) {
		Assert.notNull(links, "Given links must not be null!");

		links.forEach(this::add);

		return (T) this;
	}

	/**
	 * Adds all given {@link Link}s to the resource.
	 *
	 * @param links must not be {@literal null}.
	 * @return the representation model, never {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public T add(Link... links) {
		Assert.notNull(links, "Given links must not be null!");

		add(Arrays.asList(links));

		return (T) this;
	}

	/**
	 * Returns whether the resource contains {@link Link}s at all.
	 *
	 * @return the representation model, never {@literal null}.
	 */
	public boolean hasLinks() {
		return !this.links.isEmpty();
	}

	/**
	 * Returns all {@link Link links} contained in this resource.
	 *
	 * @return unmodifiable list of links, never {@literal null}.
	 */
	@NonNull
	@JsonProperty("links")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public List<Link> getLinks() {
		return Collections.unmodifiableList(links);
	}

	/**
	 * Removes all {@link Link}s added to the resource so far.
	 *
	 * @return the representation model instance, never {@literal null}.
	 */
	@NonNull
	@SuppressWarnings("unchecked")
	public T removeLinks() {
		this.links.clear();

		return (T) this;
	}

	/**
	 * Returns the link with the given relation.
	 *
	 * @param relation must not be {@literal null} or empty.
	 * @return the link with the given relation or {@link Optional#empty()} if none found.
	 */
	@NonNull
	public Optional<Link> getLink(String relation) {
		return getLink(LinkRelation.of(relation));
	}

	/**
	 * Returns the link with the given {@link LinkRelation}.
	 *
	 * @param relation link relation, can't be {@literal null}
	 * @return the link with the given relation or {@link Optional#empty()} if none found.
	 */
	@NonNull
	public Optional<Link> getLink(LinkRelation relation) {
		Assert.notNull(relation, "Link relation must not be null!");

		return links.stream() //
				.filter(it -> it.hasRel(relation)) //
				.findFirst();
	}

	/**
	 * Returns all {@link Link}s with the given relation.
	 *
	 * @param relation must not be {@literal null}.
	 * @return the links in a {@link List}, never {@literal null}.
	 */
	@NonNull
	public List<Link> getLinks(String relation) {
		Assert.hasText(relation, "Link relation must not be null or empty!");

		return getLinks(LinkRelation.of(relation));
	}

	/**
	 * Returns all {@link Link}s with the given relation.
	 *
	 * @param relation must not be {@literal null}.
	 * @return the links in a {@link List}, never {@literal null}.
	 */
	@NonNull
	public List<Link> getLinks(LinkRelation relation) {
		Assert.notNull(relation, "Link relation must not be null!");

		return links.stream() //
				.filter(link -> link.hasRel(relation)) //
				.toList();
	}

	@Override
	public String toString() {
		return String.format("links=%s", links.toString());
	}

}
