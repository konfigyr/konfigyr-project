package com.konfigyr.kms;

import org.jspecify.annotations.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Enumeration that defines the possible states of a {@link KeysetMetadata} within the KMS.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum KeysetMetadataState {

	/**
	 * Marks the {@link KeysetMetadata} as active, meaning that it can be used to perform cryptographic operations.
	 */
	ACTIVE,

	/**
	 * State that indicates that the {@link KeysetMetadata} is not active and can't be used for cryptographic operations.
	 */
	INACTIVE,

	/**
	 * State that indicates that the {@link KeysetMetadata} is scheduled for destruction. The actual removal of
	 * cryptographic material will be executed after the configured retention period. During this period, users
	 * will be able to read the keyset metadata but not perform any cryptographic operations.
	 */
	PENDING_DESTRUCTION,

	/**
	 * State that indicates that the {@link KeysetMetadata} has been permanently removed from the KMS.
	 */
	DESTROYED;

	private static final MultiValueMap<@NonNull KeysetMetadataState, KeysetMetadataState> transitions;

	static {
		transitions = new LinkedMultiValueMap<>();
		transitions.add(ACTIVE, INACTIVE);
		transitions.add(ACTIVE, PENDING_DESTRUCTION);
		transitions.add(INACTIVE, ACTIVE);
		transitions.add(INACTIVE, PENDING_DESTRUCTION);
		transitions.add(PENDING_DESTRUCTION, ACTIVE);
		transitions.add(PENDING_DESTRUCTION, DESTROYED);
	}

	/**
	 * Checks whether the current state can transition to the given target state.
	 *
	 * @param target the target state to check, can't be {@literal null}.
	 * @return {@literal true} if the transition is possible, {@literal false} otherwise.
	 */
	boolean canTransitionTo(@NonNull KeysetMetadataState target) {
		final List<KeysetMetadataState> candidates = transitions.get(this);
		return candidates != null && candidates.contains(target);
	}
}
