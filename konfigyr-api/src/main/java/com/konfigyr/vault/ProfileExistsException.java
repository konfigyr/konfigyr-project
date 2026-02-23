package com.konfigyr.vault;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to create a new {@link Profile} where there is already
 * a {@link Profile} with the same name in the same {@link com.konfigyr.namespace.Service}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ProfileExistsException extends VaultException {
	@Serial
	private static final long serialVersionUID = 7175230493836391835L;

	/**
	 * The {@link ProfileDefinition} that was used when this exception was thrown.
	 */
	@NonNull
	private final ProfileDefinition definition;

	/**
	 * Create new instance for the {@link ProfileDefinition} that triggered the unique constraint
	 * violation when creating a {@link Profile} for a {@link com.konfigyr.namespace.Service}.
	 *
	 * @param definition definition that triggered this exception
	 */
	public ProfileExistsException(@NonNull ProfileDefinition definition) {
		super(HttpStatus.BAD_REQUEST, "Could not create profile as one already exists with the following name: "
				+ definition.name() + " for service: " + definition.service());
		this.definition = definition;
	}

	/**
	 * Create new instance for the {@link ProfileDefinition} that triggered the unique constraint
	 * violation when creating a {@link Profile} for a {@link com.konfigyr.namespace.Service}.
	 *
	 * @param definition definition that triggered this exception
	 * @param cause the actual cause that triggered this exception
	 */
	public ProfileExistsException(@NonNull ProfileDefinition definition, Throwable cause) {
		super(HttpStatus.BAD_REQUEST, "Could not create profile as one already exists with the following name: "
				+ definition.name() + " for service: " + definition.service(), cause);
		this.definition = definition;
	}

	/**
	 * Returns the {@link ProfileDefinition} instance that was used when this exception was thrown.
	 *
	 * @return profile definition, never {@literal null}
	 */
	@NonNull
	public ProfileDefinition getDefinition() {
		return definition;
	}

}
