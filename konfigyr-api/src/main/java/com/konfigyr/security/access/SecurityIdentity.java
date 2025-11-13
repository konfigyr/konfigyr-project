package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * A security identity recognized by the Konfigur access control security system.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface SecurityIdentity extends Supplier<String>, Serializable {

	/**
	 * Creates a new {@link SecurityIdentity} from an entity identifier of an {@link com.konfigyr.account.Account}.
	 *
	 * @param id account entity identifier, can't be {@literal null}
	 * @return security identity based on the given account entity identifier, never {@literal null}
	 * @throws IllegalArgumentException when entity identifier is null
	 */
	@NonNull
	static SecurityIdentity account(EntityId id) {
		Assert.notNull(id, "Entity identifier cannot be null");
		return new SecurityEntityIdentity(id);
	}

	/**
	 * Creates a new {@link SecurityIdentity} from an OAuth 2.0 client identifier.
	 *
	 * @param clientId the {@code client_id}, can't be {@literal null}
	 * @return security identity based on the OAuth Client, never {@literal null}
	 * @throws IllegalArgumentException when the client identifier is null
	 */
	@NonNull
	static SecurityIdentity oauthClient(String clientId) {
		Assert.hasText(clientId, "OAuth client_id cannot be blank");
		return new OAuthClientSecurityIdentity(clientId);
	}

	/**
	 * Returns the identifier that is used by this {@link SecurityIdentity}.
	 *
	 * @return the identifier value, never {@literal null}
	 */
	@NonNull
	@Override
	String get();

}
