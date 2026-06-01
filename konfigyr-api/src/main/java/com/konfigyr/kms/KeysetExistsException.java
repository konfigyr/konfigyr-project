package com.konfigyr.kms;

import com.konfigyr.namespace.Namespace;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to create a new {@link KeysetMetadata} where there is already
 * one with the same name in the same namespace.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public class KeysetExistsException extends KeysetManagementException {

	@Serial
	private static final long serialVersionUID = 918522905298460950L;

	private final KeysetMetadataDefinition definition;

	/**
	 * Creates a new {@link KeysetExistsException} for the given {@link KeysetMetadataDefinition}.
	 *
	 * @param namespace the namespace where the keyset already exists, cannot be {@literal null}.
	 * @param definition the definition of the keyset that already exists, cannot be {@literal null}.
	 */
	public KeysetExistsException(Namespace namespace, KeysetMetadataDefinition definition) {
		super(HttpStatus.BAD_REQUEST, "Keyset with name '" + definition.name() + "' already exists in namespace:" + namespace.slug());
		this.definition = definition;
	}

	/**
	 * Returns the {@link KeysetMetadataDefinition} instance that was used when this exception was thrown.
	 *
	 * @return keyset definition, never {@literal null}
	 */
	public KeysetMetadataDefinition getDefinition() {
		return definition;
	}
}
