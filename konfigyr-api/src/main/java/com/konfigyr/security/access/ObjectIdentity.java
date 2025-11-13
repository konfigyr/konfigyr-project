package com.konfigyr.security.access;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the identity of an individual domain object instance for which the {@link AccessGrant} are
 * assigned.
 *
 * @param type the type of the domain object, can't be {@literal null}
 * @param id the actual domain object identifier. This identifier must not be reused to represent other domain
 *           objects with the same type, This value can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record ObjectIdentity(@NonNull String type, @NonNull Serializable id) implements Serializable {

	@Serial
	private static final long serialVersionUID = 2251039160466881435L;

	/**
	 * The type used by {@link com.konfigyr.namespace.Namespace} domain object.
	 */
	public static final String NAMESPACE_TYPE = "namespace";

	/**
	 * Creates a new {@link ObjectIdentity} for a {@link com.konfigyr.namespace.Namespace} domain object
	 * using its unique slug.
	 *
	 * @param namespace namespace slug, can not be blank
	 * @return the namespace object identity, never {@literal null}
	 * @throws IllegalArgumentException when slug is blank
	 */
	@NonNull
	public static ObjectIdentity namespace(String namespace) {
		Assert.hasText(namespace, "Namespace slug cannot be blank");
		return new ObjectIdentity(NAMESPACE_TYPE, namespace);
	}

	@NonNull
	@Override
	public String toString() {
		return "ObjectIdentity(" + type + ":" + id + ")";
	}
}
