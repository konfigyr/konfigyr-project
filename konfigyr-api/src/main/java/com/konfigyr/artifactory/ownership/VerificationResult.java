package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NonNull;

/**
 * Outcome of a verification attempt performed by a {@link VerificationStrategy}.
 * <p>
 * A result is either a {@link Success} or a {@link Failure}. {@link VerificationStrategy#verify}
 * always returns one of these two variants — it never throws. {@link GroupVerifications#verify}
 * consumes the result to drive two state transitions: the {@link VerificationChallenge} moves to
 * {@link ChallengeState#VERIFIED} on success or remains {@link ChallengeState#UNVERIFIED} on
 * failure; the {@link GroupVerification} moves to {@link VerificationState#ACTIVE} on success or
 * remains {@link VerificationState#PENDING} on failure.
 * <p>
 * Callers discriminate between the two variants using pattern matching on the sealed type:
 * <pre>{@code
 * VerificationResult result = strategy.verify(verification, challenge);
 * if (result instanceof VerificationResult.Success success) {
 *     // activate the claim
 * } else if (result instanceof VerificationResult.Failure failure) {
 *     // inspect failure.reason()
 * }
 * }</pre>
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 * @see VerificationStrategy
 * @see GroupVerifications
 * @see VerificationChallenge
 */
public sealed interface VerificationResult permits VerificationResult.Success, VerificationResult.Failure {

	/**
	 * Creates a successful verification result.
	 * <p>
	 * The {@code method} argument must match the {@link VerificationChallenge#method()} of the
	 * challenge being verified. {@link GroupVerifications} asserts this at the point of consumption
	 * to catch strategy implementations that accidentally return a result for the wrong method.
	 *
	 * @param method the {@link VerificationMethod} that confirmed ownership
	 * @return a successful verification result; never {@literal null}
	 */
	@NonNull
	static Success success(@NonNull VerificationMethod method) {
		return new Success(method);
	}

	/**
	 * Creates a failed verification result from a standardised {@link FailureReason}.
	 * <p>
	 * The reason is stored as {@link FailureReason#name()}, so callers can round-trip
	 * {@link Failure#reason()} back to a {@link FailureReason} via {@link FailureReason#valueOf}.
	 * Prefer this overload over {@link #failure(String)} whenever a predefined reason applies.
	 *
	 * @param reason the standardized failure reason
	 * @return a failed verification result; never {@literal null}
	 */
	@NonNull
	static Failure failure(@NonNull FailureReason reason) {
		return new Failure(reason.name());
	}

	/**
	 * Creates a failed verification result with a custom reason string.
	 * <p>
	 * Use this overload only when none of the predefined {@link FailureReason} values accurately
	 * describes the failure. A free-form reason is opaque to callers that attempt to parse it as a
	 * {@link FailureReason} — prefer {@link #failure(FailureReason)} whenever possible.
	 *
	 * @param reason a free-form, machine-readable description of the failure
	 * @return a failed verification result; never {@literal null}
	 */
	@NonNull
	static Failure failure(@NonNull String reason) {
		return new Failure(reason);
	}

	/**
	 * Successful verification outcome.
	 * <p>
	 * Carries the {@link VerificationMethod} that confirmed ownership so that
	 * {@link GroupVerifications} can assert the strategy's result is consistent with the
	 * {@link VerificationChallenge} it was asked to verify.
	 *
	 * @param method the verification method that confirmed ownership; never {@literal null}
	 */
	record Success(@NonNull VerificationMethod method) implements VerificationResult {
	}

	/**
	 * Failed verification outcome.
	 * <p>
	 * The reason is a machine-readable string. When constructed via {@link #failure(FailureReason)},
	 * it equals {@link FailureReason#name()} and can be round-tripped back to the enum via
	 * {@link FailureReason#valueOf}. When constructed via {@link #failure(String)}, it is an
	 * arbitrary string and no such guarantee holds.
	 *
	 * @param reason machine-readable description of the failure; never {@literal null}
	 */
	record Failure(@NonNull String reason) implements VerificationResult {
	}

	/**
	 * Standardised set of failure reasons returned by {@link VerificationStrategy} implementations.
	 * <p>
	 * Strategies must prefer these values over free-form strings so that callers can handle failures
	 * consistently regardless of which verification mechanism produced them. Values serialize to
	 * their {@link #name()} when stored in a {@link Failure#reason()}.
	 */
	enum FailureReason {
		/**
		 * The verification target was reachable but did not contain the expected challenge token.
		 * Distinguishable from {@link #TARGET_NOT_FOUND}: the target exists, the token is simply
		 * absent or wrong.
		 */
		TOKEN_MISMATCH,

		/**
		 * The verification target (e.g. DNS record, repository) could not be found. The namespace
		 * owner has not yet set up the required proof at the expected location.
		 */
		TARGET_NOT_FOUND,

		/**
		 * The external verification service is temporarily unavailable. This is a transient
		 * condition, retrying after a delay may succeed.
		 */
		SERVICE_UNAVAILABLE,

		/**
		 * An unexpected error occurred during verification that is not covered by the other reasons.
		 * Unlike {@link #SERVICE_UNAVAILABLE}, this does not indicate a retryable condition.
		 */
		INTERNAL_ERROR,
	}

}
