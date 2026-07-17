package com.konfigyr.artifactory.transfer;

import org.jspecify.annotations.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Lifecycle state of an {@link ArtifactOwnershipTransfer} request.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum TransferState {
	/**
	 * The transfer was requested and is waiting for the current owner to accept or reject it.
	 */
	PENDING,

	/**
	 * The current owner accepted the request; ownership of the affected artifacts has moved.
	 */
	ACCEPTED,

	/**
	 * The current owner rejected the request.
	 */
	REJECTED,

	/**
	 * The requesting namespace cancelled its own request.
	 */
	CANCELLED;

	private static final MultiValueMap<TransferState, TransferState> transitions;

	static {
		transitions = new LinkedMultiValueMap<>();
		transitions.addAll(PENDING, List.of(ACCEPTED, REJECTED, CANCELLED));
	}

	/**
	 * Checks if this state can be transitioned into a next one.
	 *
	 * @param state transfer state to transition to
	 * @return {@literal true} when transition is supported
	 */
	public boolean canTransitionTo(@NonNull TransferState state) {
		return transitions.getOrDefault(this, List.of()).contains(state);
	}
}
