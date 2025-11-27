package com.konfigyr.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.namespace.NamespaceRole;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.util.Streamable;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a sorted {@link Streamable} collection of {@link Membership memberships} of an {@link Account}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
@EqualsAndHashCode
public final class Memberships implements Streamable<Membership>, Serializable {

	@Serial
	private static final long serialVersionUID = 1591242249270881071L;

	private static final Memberships EMPTY = new Memberships(Collections.emptyList());

	private final List<Membership> memberships;

	/**
	 * Creates an empty instance of {@link Memberships}.
	 *
	 * @return empty memberships.
	 */
	public static Memberships empty() {
		return EMPTY;
	}

	/**
	 * Creates a new {@link Memberships} instance out of the given {@link Membership} arguments.
	 *
	 * @param memberships Membership arguments
	 * @return the account memberships instance, never {@literal null}
	 */
	public static Memberships of(Membership... memberships) {
		if (memberships.length == 0) {
			return empty();
		}
		return new Memberships(Arrays.asList(memberships));
	}

	/**
	 * Creates a new {@link Memberships} instance out of the given {@link Membership} collection.
	 *
	 * @param memberships Membership collection, can be {@literal null} or empty
	 * @return the account memberships instance, never {@literal null}
	 */
	@JsonCreator
	public static Memberships of(@Nullable Collection<Membership> memberships) {
		return CollectionUtils.isEmpty(memberships) ? empty() : new Memberships(memberships);
	}

	/**
	 * Membership constructor that would create a sorted list of {@link Membership memberships}
	 * that can filtered, mapped or collected.
	 *
	 * @param memberships collection of membership objects, can be empty but never {@literal null}
	 */
	private Memberships(Collection<Membership> memberships) {
		this.memberships = new ArrayList<>(memberships);
		this.memberships.sort(Membership::compareTo);
	}

	/**
	 * Creates a filtered subset that contains only {@link Membership memberships} with a given
	 * {@link NamespaceRole}.
	 *
	 * @param role namespace role, can't be {@literal null}
	 * @return filtered {@link Memberships} instance, never {@literal null}
	 */
	public Memberships ofRole(NamespaceRole role) {
		return filter(membership -> role == membership.role());
	}

	/**
	 * Joins the {@link com.konfigyr.namespace.Namespace} slug values in a comma-separated string.
	 *
	 * @return comma-separated string of namespace slugs, never {@literal null}
	 */
	public String join() {
		return get()
				.map(Membership::namespace)
				.collect(Collectors.joining(", "));
	}

	@Override
	public Memberships filter(Predicate<? super Membership> predicate) {
		return of(Streamable.super.filter(predicate).toList());
	}

	@Override
	@JsonValue
	public Iterator<Membership> iterator() {
		return memberships.iterator();
	}

	@Override
	public String toString() {
		return memberships.toString();
	}
}
