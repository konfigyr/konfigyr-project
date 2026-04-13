package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestMergeStatus;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the outcome of evaluating a {@link Gate}.
 * <p>
 * A gate either allows evaluation to continue ({@link Pass}) or blocks further processing by
 * providing a {@link ChangeRequestMergeStatus} and an explanatory reason.
 * <p>
 * This model reflects the conceptual behavior of a gate: it either lets the request pass
 * through or stops it with a clear justification.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
sealed interface GateResult extends Serializable permits GateResult.Pass, GateResult.Block {

	/**
	 * Creates a {@link GateResult} that indicates that the gate allows the change request to proceed.
	 *
	 * @return the passing gate result, never {@literal null}.
	 */
	static GateResult pass() {
		return Pass.INSTANCE;
	}

	/**
	 * Creates a {@link GateResult} that indicates that the gate blocks the change request from
	 * being processed further.
	 *
	 * @param status the resulting merge status, cannot be {@literal null}
	 * @param reason a human-readable explanation for the decision, can be {@literal null}
	 * @return the blocking gate result, never {@literal null}.
	 */
	static GateResult block(ChangeRequestMergeStatus status, String reason) {
		return new Block(status, reason);
	}

	/**
	 * Indicates that the current gate does not block the change request and
	 * evaluation should continue with the next gate.
	 */
	record Pass() implements GateResult {

		@Serial
		private static final long serialVersionUID = 1L;

		private static final Pass INSTANCE = new Pass();
	}

	/**
	 * Indicates that the change request is blocked and evaluation should stop.
	 *
	 * @param status the resulting merge status
	 * @param reason a human-readable explanation for the decision
	 */
	record Block(ChangeRequestMergeStatus status, String reason) implements GateResult {

		@Serial
		private static final long serialVersionUID = 1L;

	}
}
