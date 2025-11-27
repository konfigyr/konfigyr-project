package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents individual permission grants within an {@link AccessControl} that are assigned to a single
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
	 * Creates an {@link AccessGrant} for a {@link com.konfigyr.namespace.Member} that would use
	 * {@link SecurityIdentity} that is based on the entity identifier of the {@link com.konfigyr.account.Account}
	 * associated with the membership with the given {@link NamespaceRole role}.
	 *
	 * @param id the entity identifier of the account that is a namespace member, can't be {@literal null}
	 * @param role the namespace role that the account is assigned to, can't be {@literal null}
	 * @return the access grant
	 */
	static AccessGrant forNamespaceMember(@NonNull EntityId id, @NonNull NamespaceRole role) {
		return new AccessGrant(SecurityIdentity.account(id), role);
	}

	/**
	 * Creates an {@link AccessGrant} for a {@link com.konfigyr.namespace.NamespaceApplication} that would use
	 * the {@link OAuthClientSecurityIdentity} based on the configured {@code client_id} that would use the
	 * {@link NamespaceRole#ADMIN} namespace role as permission.
	 *
	 * @param clientId the {@code client_id} of Namespace OAuth Application, can't be {@literal null}
	 * @return the access grant
	 */
	static AccessGrant forNamespaceApplication(@NonNull String clientId) {
		return new AccessGrant(SecurityIdentity.oauthClient(clientId), NamespaceRole.ADMIN);
	}

}
