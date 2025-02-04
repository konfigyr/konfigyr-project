package com.konfigyr.hateoas;

import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builder that is used to construct {@link Link} instances.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface LinkBuilder {

	/**
	 * Adds the given object's {@link String} representation as sub-resource to the current URI.
	 *
	 * @param object can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull LinkBuilder path(@Nullable Object object);

	/**
	 * Adds the query parameter to the current URI, the value of the parameter would be the
	 * object's {@link String} representation.
	 *
	 * @param name query parameter name, can't be {@literal null}
	 * @param value query parameter value, can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull LinkBuilder query(@NonNull String name, @Nullable Object value);

	/**
	 * Adds a list of query parameters to the current URI.
	 *
	 * @param name query parameter name, can't be {@literal null}
	 * @param values query parameter values, can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull
	default LinkBuilder query(@NonNull String name, @Nullable Object... values) {
		return query(name, values == null ? Collections.emptyList() : Arrays.asList(values));
	}

	/**
	 * Adds a list of query parameters to the current URI.
	 *
	 * @param name query parameter name, can't be {@literal null}
	 * @param values query parameter values, can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull LinkBuilder query(@NonNull String name, @Nullable List<Object> values);

	/**
	 * Creates a URI of the link built by the current builder instance.
	 *
	 * @return the link URI, never {@literal null}.
	 */
	@NonNull URI toUri();

	/**
	 * Creates the {@link Link} built by the current builder instance with the given link relation.
	 *
	 * @param rel must not be {@literal null}.
	 * @return the built {@link Link}, never {@literal null}.
	 */
	@NonNull
	default Link rel(String rel) {
		return rel(LinkRelation.of(rel));
	}

	/**
	 * Creates the {@link Link} built by the current builder instance with the given {@link LinkRelation}.
	 *
	 * @param rel must not be {@literal null} or empty.
	 * @return the built {@link Link}, never {@literal null}.
	 */
	@NonNull Link rel(@NonNull LinkRelation rel);

	/**
	 * Creates the {@link Link} built by the current builder instance with the default self link relation.
	 *
	 * @return the built {@link Link}, never {@literal null}.
	 * @see LinkRelation#SELF
	 */
	@NonNull
	default Link selfRel() {
		return rel(LinkRelation.SELF);
	}

	/**
	 * Adds which {@link HttpMethod} should be used for the {@link Link} that is being built.
	 *
	 * @param method can't be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull
	LinkBuilder method(@NonNull HttpMethod method);

	/**
	 * Adds the title attribute to the {@link Link} that is being built.
	 *
	 * @param title can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull
	LinkBuilder title(@Nullable String title);

	/**
	 * Adds the type attribute to the {@link Link} that is being built.
	 *
	 * @param type can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull
	LinkBuilder type(@Nullable String type);

	/**
	 * Adds the name attribute to the {@link Link} that is being built.
	 *
	 * @param name can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull
	LinkBuilder name(@Nullable String name);

	/**
	 * Adds the deprecation attribute to the {@link Link} that is being built.
	 *
	 * @param deprecation can be {@literal null}.
	 * @return link builder, never {@literal null}.
	 */
	@NonNull
	LinkBuilder deprecation(@Nullable String deprecation);

}
