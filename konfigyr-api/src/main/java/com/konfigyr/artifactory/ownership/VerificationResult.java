package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NonNull;

/**
 * Represents the outcome of a verification attempt performed by a {@link VerificationStrategy}.
 * <p>
 * A verification result is either a {@link Success} or a {@link Failure}.
 */
public sealed interface VerificationResult permits VerificationResult.Success, VerificationResult.Failure {

	/**
	 * Creates a successful verification result.
	 *
	 * @param method the {@link VerificationMethod} that successfully verified ownership
	 * @return a successful verification result
	 */
	@NonNull
	static Success success(@NonNull VerificationMethod method) {
		return new Success(method);
	}

	/**
	 * Creates a failed verification result using a predefined {@link FailureReason}.
	 *
	 * @param reason the failure reason enum value
	 * @return a failed verification result
	 */
	@NonNull
	static Failure failure(@NonNull FailureReason reason) {
		return new Failure(reason.name());
	}

	/**
	 * Creates a failed verification result using a custom reason string.
	 * <p>
	 * This overload allows strategies to provide additional context beyond the predefined
	 * {@link FailureReason} values.
	 *
	 * @param reason a failure reason
	 * @return a failed verification result
	 */
	@NonNull
	static Failure failure(@NonNull String reason) {
		return new Failure(reason);
	}

	/**
	 * Represents a successful verification outcome.
	 *
	 * @param method the verification method that succeeded
	 */
	record Success(@NonNull VerificationMethod method) implements VerificationResult {
	}

	/**
	 * Represents a failed verification outcome.
	 * <p>
	 * The failure is represented as a string to allow both structured
	 * {@link FailureReason} values and custom failure descriptions.
	 *
	 * @param reason machine-readable reason for failure
	 */
	record Failure(@NonNull String reason) implements VerificationResult {
	}

	/**
	 * Standardized set of failure reasons that can occur during verification.
	 * <p>
	 * Strategies should map external errors (HTTP, DNS, API responses, etc.)
	 * into these values whenever possible to ensure consistent behavior across
	 * verification mechanisms.
	 */
	enum FailureReason {
		/** The challenge token was not found in the verification target. */
		TOKEN_MISMATCH,
		/** The verification target (e.g. DNS record, file) could not be found. */
		TARGET_NOT_FOUND,
		/** The verification service is temporarily unavailable. */
		SERVICE_UNAVAILABLE,
		/** An unexpected error occurred during verification. */
		INTERNAL_ERROR,
	}

}
