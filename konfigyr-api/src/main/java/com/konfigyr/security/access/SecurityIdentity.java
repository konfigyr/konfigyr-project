package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * A security identity recognised by the Konfigur access control security system.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface SecurityIdentity extends Supplier<String>, Serializable {

	/**
	 * Creates a new {@link SecurityIdentity} from an entity identifier.
	 *
	 * @param id entity identifier, can not be {@literal null}
	 * @return security identity based on the given entity identifier, never {@literal null}
	 * @throws IllegalArgumentException when entity identifier is null
	 */
	@NonNull
	static SecurityIdentity of(EntityId id) {
		Assert.notNull(id, "Entity identifier cannot be null");
		return new SecurityEntityIdentity(id);
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
