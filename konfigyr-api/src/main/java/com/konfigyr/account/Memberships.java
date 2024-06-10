package com.konfigyr.account;

import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.namespace.NamespaceType;
import lombok.EqualsAndHashCode;
import org.springframework.data.util.Streamable;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a sorted {@link Streamable} collection of {@link Membership memberships} of an {@link Account}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EqualsAndHashCode
public final class Memberships implements Streamable<Membership> {

	private static final Memberships EMPTY = new Memberships(Collections.emptyList());

	@NonNull
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
	@NonNull
	public static Memberships of(Membership... memberships) {
		if (memberships == null || memberships.length == 0) {
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
	@NonNull
	public static Memberships of(Collection<Membership> memberships) {
		return CollectionUtils.isEmpty(memberships) ? empty() : new Memberships(memberships);
	}

	/**
	 * Memberships constructor that would create a sorted list of {@link Membership memberships}
	 * that can filtered, mapped or collected.
	 *
	 * @param memberships collection of memberships, can be empty but never {@literal null}
	 */
	private Memberships(@NonNull Collection<Membership> memberships) {
		this.memberships = new ArrayList<>(memberships);
		this.memberships.sort(Membership::compareTo);
	}

	/**
	 * Creates a filtered subset that contains only {@link Membership memberships} of a given
	 * {@link NamespaceType}.
	 *
	 * @param type namespace type, can't be {@literal null}
	 * @return filtered {@link Memberships} instance, never {@literal null}
	 */
	@NonNull
	public Memberships ofType(@NonNull NamespaceType type) {
		return filter(membership -> type == membership.type());
	}

	/**
	 * Creates a filtered subset that contains only {@link Membership memberships} with a given
	 * {@link NamespaceRole}.
	 *
	 * @param role namespace role, can't be {@literal null}
	 * @return filtered {@link Memberships} instance, never {@literal null}
	 */
	@NonNull
	public Memberships ofRole(@NonNull NamespaceRole role) {
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

	@NonNull
	@Override
	public Memberships filter(@NonNull Predicate<? super Membership> predicate) {
		return of(Streamable.super.filter(predicate).toList());
	}

	@NonNull
	@Override
	public Iterator<Membership> iterator() {
		return memberships.iterator();
	}
}
