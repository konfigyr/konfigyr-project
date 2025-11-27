package com.konfigyr.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable type that is used as a wrapper to hold a unique set of {@link OAuthScope OAuth scopes}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuthScope
 */
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthScopes implements Iterable<OAuthScope>, Serializable {

	private static final Set<String> SCOPE_NAMES = Set.of("scope", "scp");
	private static final OAuthScopes EMPTY = new OAuthScopes(Collections.emptyList());

	private final List<OAuthScope> scopes;

	/**
	 * Creates an empty {@link OAuthScopes OAuth scope set}.
	 *
	 * @return empty scopes, never {@literal null}
	 */
	@NonNull
	public static OAuthScopes empty() {
		return EMPTY;
	}

	/**
	 * Creates a new {@link OAuthScopes OAuth Scope set} from the given {@link ClaimAccessor JWT Claims}.
	 * <p>
	 * The OAuth scopes should be present in either {@code scope} or {@code scp} claims in order to
	 * parse them and create the {@link OAuthScopes OAuth scope set}.
	 *
	 * @param claims claims that may contain the scope claim, can be {@literal null}
	 * @return OAuth scope set, never {@literal null}
	 */
	public static OAuthScopes from(@Nullable ClaimAccessor claims) {
		if (claims == null) {
			return EMPTY;
		}

		for (String scope : SCOPE_NAMES) {
			if (claims.hasClaim(scope)) {
				final List<String> values = claims.getClaimAsStringList(scope);

				if (CollectionUtils.isEmpty(values)) {
					return EMPTY;
				}

				final List<OAuthScope> scopes = collect(
						values.stream()
								.map(OAuthScope::parse)
								.filter(Objects::nonNull)
								.flatMap(Collection::stream)
				);

				return new OAuthScopes(scopes);
			}
		}

		return EMPTY;
	}

	/**
	 * Creates a new {@link OAuthScopes OAuth Scope set} from the scopes string.
	 *
	 * @param scopes scopes to be parsed and wrapped inside the scope set, can be {@literal null}
	 * @return OAuth scope set, never {@literal null}
	 */
	@JsonCreator
	public static OAuthScopes parse(@Nullable String scopes) {
		return StringUtils.hasText(scopes) ? of(OAuthScope.parse(scopes)) : EMPTY;
	}

	/**
	 * Creates a new {@link OAuthScopes OAuth Scope set} from the given scopes.
	 *
	 * @param scopes scopes to be wrapped inside the scope set, can be {@literal null}
	 * @return OAuth scope set, never {@literal null}
	 */
	public static OAuthScopes of(@Nullable OAuthScope... scopes) {
		return scopes == null ? EMPTY : new OAuthScopes(collect(Arrays.stream(scopes)));
	}

	/**
	 * Creates a new {@link OAuthScopes OAuth Scope set} from the given collection of scopes.
	 *
	 * @param scopes scopes to be wrapped inside the scope set, can be {@literal null}
	 * @return OAuth scope set, never {@literal null}
	 */
	@NonNull
	public static OAuthScopes of(@Nullable Collection<OAuthScope> scopes) {
		return CollectionUtils.isEmpty(scopes) ? EMPTY : new OAuthScopes(collect(scopes.stream()));
	}

	/**
	 * Method that would return a collection of {@link GrantedAuthority granted authorities} out of
	 * this {@link OAuthScopes OAuth scope set}.
	 *
	 * @return granted authorities, never {@literal null}
	 */
	public Collection<? extends GrantedAuthority> toAuthorities() {
		return scopes;
	}

	/**
	 * Checks if this scope is contained within the {@link OAuthScopes OAuth scope set}. Keep in mind that
	 * method would traverse the included scopes contained with each scope in the set.
	 * <p>
	 * For instance, if the set contains a {@link OAuthScope#WRITE_NAMESPACES namespaces:write} and you want
	 * to check if the {@link OAuthScope#READ_NAMESPACES namespaces:read} is contained, this method would
	 * return {@code true}. But when checking if {@link OAuthScope#DELETE_NAMESPACES namespaces:delete}
	 * scope is in the set, this method would return {@code false}.
	 *
	 * @param scope scope to be checked, can be {@literal null}
	 * @return {@code true} if this scope is contained within the scope set
	 */
	public boolean contains(@Nullable OAuthScope scope) {
		if (scope == null) {
			return false;
		}

		for (OAuthScope it : this) {
			if (it == scope) {
				return true;
			}

			for (OAuthScope included : it.getIncluded()) {
				if (included == scope) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks if this scope value is contained within the {@link OAuthScopes OAuth scope set}.
	 *
	 * @param scope scope to be checked, can be {@literal null}
	 * @return {@code true} if this scope is contained within the scope set
	 * @throws IllegalArgumentException when the given scope value is not a valid {@link OAuthScope}
	 * @see #contains(OAuthScope)
	 */
	public boolean contains(@Nullable String scope) {
		return StringUtils.hasText(scope) && contains(OAuthScope.from(scope));
	}

	/**
	 * Checks if this {@link GrantedAuthority} is contained within the {@link OAuthScopes OAuth scope set}.
	 * <p>
	 * If the {@link GrantedAuthority} is not an {@link OAuthScope}, the authority value would be used
	 * to create one. This method would try to remove the {@code SCOPE_} prefix as Spring Security appends
	 * it via it's default converters.
	 *
	 * @param authority authority to be checked, can be {@literal null}
	 * @return {@code true} if this authority has a matching {@link OAuthScope}
	 * @throws IllegalArgumentException when the granted authority value is not a valid {@link OAuthScope}
	 * @see #contains(OAuthScope)
	 */
	public boolean contains(@Nullable GrantedAuthority authority) {
		return forGrantedAuthority(authority, this::contains);
	}

	/**
	 * Checks if the requested scope is permitted by the scopes within this {@link OAuthScopes OAuth scope set}
	 * by checking if the given scope, or if any of its included scopes, is in this set.
	 * <p>
	 * For instance, if the set contains a {@link OAuthScope#WRITE_NAMESPACES namespaces:write} and you want
	 * to check if the {@link OAuthScope#READ_NAMESPACES namespaces:read} is permitted, this method would
	 * return {@code false}. But when checking if {@link OAuthScope#DELETE_NAMESPACES namespaces:delete}
	 * scope is in the set, this method would return {@code true}.
	 *
	 * @param scope scope to be checked, can be {@literal null}
	 * @return {@code true} if this scope is permitted by the scope set
	 */
	public boolean permits(@Nullable OAuthScope scope) {
		if (scope == null) {
			return false;
		}

		for (OAuthScope it : this) {
			if (it == scope) {
				return true;
			}

			for (OAuthScope included : scope.getIncluded()) {
				if (included == it || permits(included)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks if this scope value is contained within the {@link OAuthScopes OAuth scope set}.
	 *
	 * @param scope scope to be checked, can be {@literal null}
	 * @return {@code true} if this scope is contained within the scope set
	 * @throws IllegalArgumentException when the given scope value is not a valid {@link OAuthScope}
	 * @see #contains(OAuthScope)
	 */
	public boolean permits(@Nullable String scope) {
		return StringUtils.hasText(scope) && permits(OAuthScope.from(scope));
	}

	/**
	 * Checks if this {@link GrantedAuthority} is contained within the {@link OAuthScopes OAuth scope set}.
	 * <p>
	 * If the {@link GrantedAuthority} is not an {@link OAuthScope}, the authority value would be used
	 * to create one. This method would try to remove the {@code SCOPE_} prefix as Spring Security appends
	 * it via it's default converters.
	 *
	 * @param authority authority to be checked, can be {@literal null}
	 * @return {@code true} if this authority has a matching {@link OAuthScope}
	 * @throws IllegalArgumentException when the granted authority value is not a valid {@link OAuthScope}
	 * @see #contains(OAuthScope)
	 */
	public boolean permits(@Nullable GrantedAuthority authority) {
		return forGrantedAuthority(authority, this::permits);
	}

	/**
	 * Returns the number of scopes in this {@link OAuthScopes OAuth scope set}.
	 *
	 * @return the number of {@link OAuthScope scopes} in this collection.
	 */
	public int size() {
		return scopes.size();
	}

	/**
	 * Checks if this {@link OAuthScopes OAuth scope set} contains any {@link OAuthScope scopes}.
	 *
	 * @return {@code true} if this set contains no scope
	 */
	public boolean isEmpty() {
		return scopes.isEmpty();
	}

	/**
	 * Creates a {@link Stream} of {@link OAuthScope OAuth scopes}.
	 *
	 * @return OAuth scopes stream, never {@literal null}
	 */
	@NonNull
	public Stream<OAuthScope> stream() {
		return scopes.stream();
	}

	@NonNull
	@Override
	public Iterator<OAuthScope> iterator() {
		return scopes.iterator();
	}

	@Override
	@JsonValue
	public String toString() {
		return stream().map(OAuthScope::getAuthority).collect(Collectors.joining(" "));
	}

	static boolean forGrantedAuthority(GrantedAuthority authority, Predicate<OAuthScope> predicate) {
		if (authority == null) {
			return false;
		}

		if (authority instanceof OAuthScope scope) {
			return predicate.test(scope);
		}

		String value = authority.getAuthority();

		if (value == null) {
			return false;
		}

		if (value.startsWith("SCOPE_")) {
			value = value.substring(6);
		}

		if (StringUtils.hasText(value)) {
			return predicate.test(OAuthScope.from(value));
		}

		return false;
	}

	private static List<OAuthScope> collect(@NonNull Stream<OAuthScope> stream) {
		return stream.filter(Objects::nonNull).sorted().toList();
	}
}
