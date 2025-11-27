package com.konfigyr.namespace;

import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to create a new {@link Namespace} where there is already
 * a {@link Namespace} with the same slug.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class NamespaceExistsException extends NamespaceException {
	@Serial
	private static final long serialVersionUID = -8267939138924273606L;

	/**
	 * The {@link NamespaceDefinition} that was used when this exception was thrown.
	 */
	@NonNull
	private final NamespaceDefinition definition;

	/**
	 * Create new instance for the {@link NamespaceDefinition} that triggered the unique constraint
	 * violation when creating an {@link Namespace}.
	 *
	 * @param definition definition that triggered this exception
	 */
	public NamespaceExistsException(@NonNull NamespaceDefinition definition) {
		super(HttpStatus.BAD_REQUEST, "Could not create namespace as one already exists with the following slug: " + definition.slug());
		this.definition = definition;
	}

	/**
	 * Create new instance for the {@link NamespaceDefinition} that triggered the unique constraint
	 * violation when creating an {@link Namespace}.
	 *
	 * @param definition definition that triggered this exception
	 * @param cause the actual cause that triggered this exception
	 */
	public NamespaceExistsException(@NonNull NamespaceDefinition definition, Throwable cause) {
		super(HttpStatus.BAD_REQUEST, "Could not create namespace as one already exists with the following slug: " + definition.slug(), cause);
		this.definition = definition;
	}

	/**
	 * Returns the {@link NamespaceDefinition} instance that was used when this exception was thrown.
	 *
	 * @return namespace definition, never {@literal null}
	 */
	@NonNull
	public NamespaceDefinition getDefinition() {
		return definition;
	}

}
