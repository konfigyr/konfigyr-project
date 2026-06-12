package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link NamespaceTrustedIssuer} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class NamespaceTrustedIssuerNotFoundException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = -1782345890987654321L;

	/**
	 * Creates a new instance when no {@link NamespaceTrustedIssuer} matches the given {@link EntityId}.
	 *
	 * @param id trusted issuer entity identifier, can't be {@code null}
	 */
	public NamespaceTrustedIssuerNotFoundException(@NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find a namespace trusted issuer with the following identifier: " + id.serialize());
	}

}
