package com.konfigyr.security.access;

import org.springframework.lang.NonNull;

/**
 * Interface that represents a mutable {@link AccessControl} object that can be used to update
 * it's {@link AccessGrant access grants}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface MutableAccessControl extends AccessControl {

	/**
	 * Adds a new {@link AccessGrant} to this access control object.
	 *
	 * @param grant grant to be added, can't be {@literal null}
	 */
	void add(@NonNull AccessGrant grant);

	/**
	 * Removes an existing {@link AccessGrant} from this access control object.
	 *
	 * @param grant grant to be removed, can't be {@literal null}
	 */
	void remove(@NonNull AccessGrant grant);

	/**
	 * Removes all {@link AccessGrant access grants} from this access control object that
	 * are assigned to this {@link SecurityIdentity}.
	 *
	 * @param identity identity for grants are to be removed, can't be {@literal null}
	 */
	default void remove(@NonNull SecurityIdentity identity) {
		for (final AccessGrant grant : this) {
			if (identity.equals(grant.identity())) {
				remove(grant);
			}
		}
	}

}
