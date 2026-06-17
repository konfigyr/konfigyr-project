package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NonNull;

public sealed interface VerificationResult permits VerificationResult.Success, VerificationResult.Failure {

    @NonNull
    static Success success(@NonNull VerificationMethod method) {
        return new Success(method);
    }

    @NonNull
    static Failure failure(@NonNull String reason) {
        return new Failure(reason);
    }

    record Success(@NonNull VerificationMethod method) implements VerificationResult {
    }

    record Failure(@NonNull String reason) implements VerificationResult {
    }

}
