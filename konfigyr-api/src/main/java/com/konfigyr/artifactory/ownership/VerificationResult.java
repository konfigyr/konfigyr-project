package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NonNull;

public sealed interface VerificationResult permits VerificationResult.Success, VerificationResult.Failure {

	@NonNull
	static Success success(@NonNull VerificationMethod method) {
		return new Success(method);
	}

	@NonNull
	static Failure failure(@NonNull FailureReason reason) {
		return new Failure(reason.name());
	}

	@NonNull
	static Failure failure(@NonNull String reason) {
		return new Failure(reason);
	}

	record Success(@NonNull VerificationMethod method) implements VerificationResult {
	}

	record Failure(@NonNull String reason) implements VerificationResult {
	}

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
