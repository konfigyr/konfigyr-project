package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Lifecycle state of a group verification claim.
 *
 * @author Vitalii Kushnir
 */
public enum VerificationState {
	/**
	 * The claim was created and is waiting for a successful proof.
	 */
	PENDING,

	/**
	 * The claim has been verified and is now active.
	 */
	ACTIVE,

	/**
	 * The claim was manually revoked or disabled.
	 */
	REVOKED,

	/**
	 * The claim attempt failed and is no longer valid.
	 */
	FAILED;

	private static final MultiValueMap<VerificationState, VerificationState> transitions;

	static {
		transitions = new LinkedMultiValueMap<>();
		transitions.addAll(PENDING, List.of(ACTIVE, FAILED, REVOKED));
		transitions.add(ACTIVE, REVOKED);
	}

	/**
	 * Checks if this state can be transitioned into a next one.
	 *
	 * @param state verification state to transition to
	 * @return {@literal true} when transition is supported
	 */
	public boolean canTransitionTo(@NonNull VerificationState state) {
		return transitions.getOrDefault(this, List.of()).contains(state);
	}
}
