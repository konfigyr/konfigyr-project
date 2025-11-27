package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to create a new {@link Namespace} for which the
 * {@link com.konfigyr.account.Account owner account} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class NamespaceOwnerException extends NamespaceException {
	@Serial
	private static final long serialVersionUID = -8267939138924273606L;

	/**
	 * The {@link NamespaceDefinition} that was used when this exception was thrown.
	 */
	@NonNull
	private final NamespaceDefinition definition;

	/**
	 * Create new instance for the {@link NamespaceDefinition} that triggered the constraint
	 * violation when creating an {@link Namespace}.
	 *
	 * @param definition definition that triggered this exception
	 */
	public NamespaceOwnerException(@NonNull NamespaceDefinition definition) {
		super("Could not create namespace as owner does not exists with: " + definition.owner());
		this.definition = definition;
	}

	/**
	 * Create new instance for the {@link NamespaceDefinition} that triggered the constraint
	 * violation when creating an {@link Namespace}.
	 *
	 * @param definition definition that triggered this exception
	 * @param cause the actual cause that triggered this exception
	 */
	public NamespaceOwnerException(@NonNull NamespaceDefinition definition, Throwable cause) {
		super("Could not create namespace as owner does not exists with: " + definition.owner(), cause);
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

	/**
	 * Returns the {@link EntityId} of the {@link com.konfigyr.account.Account owner} that was used when
	 * this exception was thrown.
	 *
	 * @return namespace definition, never {@literal null}
	 */
	@NonNull
	public EntityId getOwner() {
		return definition.owner();
	}
}
