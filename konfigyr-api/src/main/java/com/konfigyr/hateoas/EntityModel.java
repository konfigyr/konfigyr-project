package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.EqualsAndHashCode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A simple {@link EntityModel} wrapping a domain object and adding links to it.
 *
 * @param <T> the domain object type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
public class EntityModel<T> extends RepresentationModel<EntityModel<T>> {

	private final T content;

	/**
	 * Creates a new {@link EntityModel} with the given content and {@link Link}s.
	 *
	 * @param content must not be {@literal null}.
	 * @param links the links to add to the {@link EntityModel}.
	 */
	@JsonCreator
	protected EntityModel(@JsonProperty("content") T content, @JsonProperty("links") Iterable<Link> links) {
		Assert.notNull(content, "Content must not be null!");
		Assert.isTrue(!(content instanceof Collection), "Content must not be a collection! Use CollectionModel instead!");

		this.content = content;
		this.add(links);
	}

	/**
	 * Creates a new {@link EntityModel} with the given content.
	 *
	 * @param content must not be {@literal null}.
	 * @param <T> the domain object type
	 * @return an entity model, never {@literal null}
	 */
	public static <T> EntityModel<T> of(T content) {
		return of(content, Collections.emptyList());
	}

	/**
	 * Creates a new {@link EntityModel} with the given content and {@link Link}s (optional).
	 *
	 * @param content must not be {@literal null}.
	 * @param links the links to add to the {@link EntityModel}.
	 * @param <T> the domain object type
	 * @return an entity model, never {@literal null}
	 */
	public static <T> EntityModel<T> of(T content, Link... links) {
		return of(content, Arrays.asList(links));
	}

	/**
	 * Creates a new {@link EntityModel} with the given content and {@link Link}s.
	 *
	 * @param content must not be {@literal null}.
	 * @param links the links to add to the {@link EntityModel}.
	 * @param <T> the domain object type
	 * @return an entity model, never {@literal null}
	 */
	public static <T> EntityModel<T> of(T content, Iterable<Link> links) {
		return new EntityModel<>(content, links);
	}

	/**
	 * Returns the underlying entity.
	 *
	 * @return the content, may be {@literal null} if the representation is empty.
	 */
	@Nullable
	@JsonUnwrapped
	@JsonSerialize
	public T getContent() {
		return content;
	}

	@Override
	public String toString() {
		return String.format("EntityModel(content=%s, %s)", getContent(), super.toString());
	}

}
