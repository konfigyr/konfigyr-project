package com.konfigyr.hateoas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Interface that defines available link relations based on standard IANA-based link relations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see <a href="https://www.iana.org/assignments/link-relations/link-relations.xhtml">Link relations</a>
 * @see <a href="https://tools.ietf.org/html/rfc8288">RFC-8288</a>
 * @see <a href="https://github.com/link-relations/registry">Link relation registry</a>
 */
public sealed interface LinkRelation extends Supplier<String> permits SimpleLinkRelation {

	/**
	 * Convert a string-based link relation to a {@link LinkRelation}. Per RFC8288, parsing of link relations
	 * not case-sensitive.
	 *
	 * @param relation as a string, can be {@literal null}.
	 * @return the link relation as a {@link LinkRelation}.
	 * @throws IllegalArgumentException when relation is {@literal null} or blank.
	 */
	@NonNull
	@JsonCreator
	static LinkRelation of(String relation) {
		Assert.hasText(relation, "Link relation value must not be blank");
		return new SimpleLinkRelation(relation.toLowerCase(Locale.US));
	}

	/**
	 * An IRI that refers to the furthest preceding resource in a series of resources.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc8288">rfc8288</a>
	 */
	LinkRelation FIRST = of("first");

	/**
	 * Refers to context-sensitive help.
	 *
	 * @see <a href="https://www.w3.org/TR/html5/links.html#link-type-help">Help link type</a>
	 */
	LinkRelation HELP = of("help");

	/**
	 * An IRI that refers to the furthest following resource in a series of resources.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc8288">rfc8288</a>
	 */
	LinkRelation LAST = of("last");

	/**
	 * Indicates that the link's context is a part of a series, and that the next in the series is the link target.
	 *
	 * @see <a href="https://www.w3.org/TR/html5/links.html#link-type-next">Next link type</a>
	 */
	LinkRelation NEXT = of("next");

	/**
	 * Indicates a resource where payment is accepted.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc8288">rfc8288</a>
	 */
	LinkRelation PAYMENT = of("payment");

	/**
	 * Indicates that the link's context is a part of a series, and that the previous in the series is the link target.
	 *
	 * @see <a href="https://www.w3.org/TR/html5/links.html#link-type-prev">Prev link type</a>
	 */
	LinkRelation PREV = of("prev");

	/**
	 * Refers to the previous resource in an ordered series of resources. Synonym for "prev".
	 *
	 * @see <a href="https://www.w3.org/TR/1999/REC-html401-19991224">REC-html401-19991224</a>
	 */
	LinkRelation PREVIOUS = of("previous");

	/**
	 * Identifies a related resource.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4287">rfc4287</a>
	 */
	LinkRelation RELATED = of("related");

	/**
	 * Refers to a resource that can be used to search through the link's context and related resources.
	 *
	 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/1.1">OpenSearch</a>
	 */
	LinkRelation SEARCH = of("search");

	/**
	 * Conveys an identifier for the link's context.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4287">rfc4287</a>
	 */
	LinkRelation SELF = of("self");

	/**
	 * Returns the actual {@link LinkRelation} value as a string.
	 *
	 * @return the relation value, never {@link null}
	 */
	@NonNull
	@JsonValue
	String get();

}
