package com.konfigyr.kms;

import com.konfigyr.crypto.KeyStatus;
import lombok.Getter;

import java.util.Set;

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
	ACTIVE(KeyStatus.ENABLED),

	/**
	 * State that indicates that the {@link KeysetMetadata} is not active and can't be used for cryptographic operations.
	 */
	INACTIVE(KeyStatus.INITIALIZING, KeyStatus.COMPROMISED, KeyStatus.DISABLED),

	/**
	 * State that indicates that the {@link KeysetMetadata} is scheduled for destruction. The actual removal of
	 * cryptographic material will be executed after the configured retention period. During this period, users
	 * will be able to read the keyset metadata but not perform any cryptographic operations.
	 */
	PENDING_DESTRUCTION(KeyStatus.PENDING_DESTRUCTION, KeyStatus.DESTRUCTION_FAILED),

	/**
	 * State that indicates that the {@link KeysetMetadata} has been permanently removed from the KMS.
	 */
	DESTROYED(KeyStatus.DESTROYED, KeyStatus.INITIALIZATION_FAILED);

	@Getter
	private final Set<KeyStatus> keyStatuses;

	KeysetMetadataState(KeyStatus... statuses) {
		this.keyStatuses = Set.of(statuses);
	}

	/**
	 * Attempts to resolve the {@link KeysetMetadataState} for the given {@link KeyStatus}.
	 *
	 * @param status the status to resolve
	 * @return the resolved {@link KeysetMetadataState} or {@link KeysetMetadataState#INACTIVE} if no match is found
	 */
	static KeysetMetadataState valueOf(KeyStatus status) {
		for (KeysetMetadataState state : values()) {
			if (state.keyStatuses.contains(status)) {
				return state;
			}
		}
		return KeysetMetadataState.INACTIVE;
	}

}
