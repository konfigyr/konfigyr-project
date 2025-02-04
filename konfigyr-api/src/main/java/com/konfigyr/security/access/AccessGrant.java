package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents an individual permission grants within an {@link AccessControl} that are assigned to an
 * {@link SecurityIdentity}.
 *
 * @param identity identifier of the security principal that is granted access to given domain object,
 *                 can't be {@literal null}
 * @param permission the permission granted to a security principal for a given domain object,
 *                   can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public record AccessGrant(@NonNull SecurityIdentity identity, @NonNull Serializable permission) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6179810569647384554L;

	/**
	 * Creates an {@link AccessGrant} for a permission to an {@link SecurityIdentity} that is based on the entity
	 * identifier with.
	 *
	 * @param identity identifier of the security principal, can't be {@literal null}
	 * @param permission the granted permission, can't be {@literal null}
	 * @param <P> permission type
	 * @return the access grant
	 */
	static <P extends Serializable> AccessGrant of(@NonNull EntityId identity, @NonNull P permission) {
		return new AccessGrant(SecurityIdentity.of(identity), permission);
	}

}
