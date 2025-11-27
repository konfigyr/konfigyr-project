package com.konfigyr.namespace;

import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to create a new {@link Service} where there is already
 * a {@link Namespace} with the same slug.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ServiceExistsException extends NamespaceException {
	@Serial
	private static final long serialVersionUID = 7175230493836391835L;

	/**
	 * The {@link ServiceDefinition} that was used when this exception was thrown.
	 */
	@NonNull
	private final ServiceDefinition definition;

	/**
	 * Create new instance for the {@link ServiceDefinition} that triggered the unique constraint
	 * violation when creating a {@link Service} for a {@link Namespace}.
	 *
	 * @param definition definition that triggered this exception
	 */
	public ServiceExistsException(@NonNull ServiceDefinition definition) {
		super(HttpStatus.BAD_REQUEST, "Could not create service as one already exists with the following slug: "
				+ definition.slug() + " for Namespace: " + definition.namespace());
		this.definition = definition;
	}

	/**
	 * Create new instance for the {@link ServiceDefinition} that triggered the unique constraint
	 * violation when creating a {@link Service} for a {@link Namespace}.
	 *
	 * @param definition definition that triggered this exception
	 * @param cause the actual cause that triggered this exception
	 */
	public ServiceExistsException(@NonNull ServiceDefinition definition, Throwable cause) {
		super(HttpStatus.BAD_REQUEST, "Could not create service as one already exists with the following slug: "
				+ definition.slug() + " for Namespace: " + definition.namespace(), cause);
		this.definition = definition;
	}

	/**
	 * Returns the {@link ServiceDefinition} instance that was used when this exception was thrown.
	 *
	 * @return service definition, never {@literal null}
	 */
	@NonNull
	public ServiceDefinition getDefinition() {
		return definition;
	}

}
