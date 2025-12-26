package com.konfigyr.kms;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to transition a {@link KeysetMetadata} to an unsuitable state.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public class KeysetTransitionException extends KeysetManagementException {

	@Serial
	private static final long serialVersionUID = 918522905298460950L;

	private final EntityId keyset;
	private final KeysetMetadataState current;
	private final KeysetMetadataState target;

	/**
	 * Creates a new {@link KeysetTransitionException} for the given {@link KeysetMetadataDefinition}.
	 *
	 * @param id the identifier of the keyset that was not found, cannot be {@literal null}.
	 * @param current the current state of the keyset, cannot be {@literal null}.
	 * @param target the target state of the keyset, cannot be {@literal null}.
	 */
	public KeysetTransitionException(EntityId id, KeysetMetadataState current, KeysetMetadataState target) {
		super(HttpStatus.BAD_REQUEST, "Failed to transition keyset " + id.serialize() + " from " + current + " to " + target + ".");
		this.keyset = id;
		this.current = current;
		this.target = target;
	}

	/**
	 * The entity identifier of the keyset that was attempted to transition.
	 *
	 * @return keyset identifier, never {@literal null}.
	 */
	public EntityId getKeyset() {
		return keyset;
	}

	/**
	 * Returns the current {@link KeysetMetadataState} of the keyset.
	 *
	 * @return current keyset state, never {@literal null}
	 */
	public KeysetMetadataState getCurrentState() {
		return current;
	}

	/**
	 * Returns the target {@link KeysetMetadataState} that was requested.
	 *
	 * @return target keyset state, never {@literal null}
	 */
	public KeysetMetadataState getTargetState() {
		return target;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { current, target, keyset.serialize() };
	}
}
