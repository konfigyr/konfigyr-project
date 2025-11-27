package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Value object for links. You can use the {@link LinkBuilder} to create instances of this type if
 * static factory methods do not fit your needs.
 *
 * @param rel link relation, can't be {@literal null}
 * @param href link URI, can't be {@literal null}
 * @param method HTTP method to be used, can't be {@literal null}
 * @param title link title, can br {@literal null}
 * @param type link type, can br {@literal null}
 * @param name link name, can br {@literal null}
 * @param deprecation link deprecation reason, can br {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see LinkBuilder
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Link(
		@NonNull @JsonProperty LinkRelation rel,
		@NonNull @JsonProperty String href,
		@NonNull @JsonProperty String method,
		@Nullable @JsonProperty String title,
		@Nullable @JsonProperty String type,
		@Nullable @JsonProperty String name,
		@Nullable @JsonProperty String deprecation
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 9037755944661782121L;

	public Link {
		Assert.hasText(href, "Href must not be null or empty");
		Assert.notNull(rel, "Link relation must not be null");
	}

	/**
	 * Creates a new link to the given URI with the {@link LinkRelation#SELF self relation}.
	 *
	 * @param href must not be {@literal null} or empty.
	 * @return self link, never {@literal null}.
	 * @see LinkRelation#SELF
	 */
	@NonNull
	public static Link of(String href) {
		return of(href, LinkRelation.SELF);
	}

	/**
	 * Creates a new {@link Link} to the given href with the given relation.
	 *
	 * @param href must not be {@literal null} or empty.
	 * @param relation must not be {@literal null} or empty.
	 * @return link with relation, never {@literal null}.
	 */
	public static Link of(String href, String relation) {
		return of(href, LinkRelation.of(relation));
	}

	/**
	 * Creates a new {@link Link} to the given href with the given relation and HTTP method.
	 *
	 * @param href must not be {@literal null} or empty.
	 * @param relation must not be {@literal null} or empty.
	 * @return link with relation, never {@literal null}.
	 */
	public static Link of(String href, String relation, String method) {
		return of(href, LinkRelation.of(relation));
	}

	/**
	 * Creates a new {@link Link} to the given href and {@link LinkRelation}.
	 *
	 * @param href must not be {@literal null} or empty.
	 * @param relation must not be {@literal null}.
	 * @return link with relation, never {@literal null}.
	 */
	public static Link of(String href, LinkRelation relation) {
		return new Link(relation, href, HttpMethod.GET.name(), null, null, null, null);
	}

	/**
	 * Creates a new {@link Link} to the given href and {@link LinkRelation} and method.
	 *
	 * @param href must not be {@literal null} or empty.
	 * @param relation must not be {@literal null}.
	 * @return link with relation, never {@literal null}.
	 */
	public static Link of(String href, LinkRelation relation, String method) {
		return new Link(relation, href, HttpMethod.valueOf(method).name(), null, null, null, null);
	}

	/**
	 * Creates a new {@link LinkBuilder} to customize and construct new {@link Link links}.
	 *
	 * @return link builder, never {@literal null}.
	 */
	public static LinkBuilder builder() {
		return new UriComponentsLinkBuilder();
	}

	/**
	 * Returns whether the current {@link Link} has the given link relation.
	 *
	 * @param rel must not be {@literal null} or empty.
	 * @return {@literal true} if link has the given link relation.
	 */
	public boolean hasRel(String rel) {
		Assert.hasText(rel, "Link relation must not be null or empty!");

		return hasRel(LinkRelation.of(rel));
	}

	/**
	 * Returns whether the {@link Link} has the given {@link LinkRelation}.
	 *
	 * @param rel must not be {@literal null}.
	 * @return {@literal true} if link has the given link relation.
	 */
	public boolean hasRel(LinkRelation rel) {
		Assert.notNull(rel, "Link relation must not be null!");

		return Objects.equals(this.rel, rel);
	}

}
