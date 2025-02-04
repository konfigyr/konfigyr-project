package com.konfigyr.security.access;

import lombok.Getter;
import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Exception that is thrown when {@link ObjectIdentity} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Getter
public class ObjectIdentityNotFound extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 8903903432016141816L;

	private final ObjectIdentity objectIdentity;

	/**
	 * Constructs an {@link ObjectIdentityNotFound} with an {@link ObjectIdentity} for which the
	 * data lookup was performed.
	 *
	 * @param identity the missing object identity
	 */
	public ObjectIdentityNotFound(@NonNull ObjectIdentity identity) {
		super("Can not find a '" + identity.type() + "' domain object with identifier: " + identity.id());
		this.objectIdentity = identity;
	}

}
