package com.konfigyr.security.access;

import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.Collection;

/**
 * Interface that represents an access control object that is granted on a domain object.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
interface AccessControl extends Iterable<AccessGrant>, Serializable {

	/**
	 * Obtains the {@link ObjectIdentity identity of the domain object} for which the access controls are configured.
	 *
	 * @return the object identity, never {@literal null}
	 */
	@NonNull ObjectIdentity objectIdentity();

	/**
	 * This is the actual authorization logic method that checks if the underlying domain object has
	 * granted permissions to the current {@link org.springframework.security.core.Authentication} object.
	 *
	 * @param identity the currency security identity, can't be {@literal null}
	 * @param permissions the permissions required (at least one entry required)
	 * @return {@literal true} if authorization is granted
	 */
	boolean isGranted(@NonNull SecurityIdentity identity, @NonNull Collection<Serializable> permissions);

}
