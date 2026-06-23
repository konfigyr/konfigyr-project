package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Verification challenge attached to a group verification claim.
 * <p>
 * A challenge stores the issued verification method, the generated token, and its current lifecycle
 * state. The challenge is created in an unverified state and then transitions to verified or expired
 * depending on the verification outcome.
 *
 * @param id             the persistent challenge identifier; may be {@literal null} before the challenge is saved
 * @param verificationId the owning verification identifier; may be {@literal null} before persistence
 * @param method         the verification method used to issue and validate the challenge; never {@literal null}
 * @param token          the challenge token exposed to the external verification target; never {@literal null}
 * @param state          the current challenge state; never {@literal null}
 * @param createdAt      timestamp when the challenge was created; may be {@literal null} before persistence
 * @param verifiedAt     timestamp when the challenge was verified; may be {@literal null}
 * @param expiresAt      timestamp when the challenge expires; may be {@literal null}
 */
@Builder(toBuilder = true)
public record VerificationChallenge(
        @Nullable EntityId id,
        @Nullable EntityId verificationId,
        @NonNull VerificationMethod method,
        @NonNull String token,
        @NonNull ChallengeState state,
        @Nullable OffsetDateTime createdAt,
        @Nullable OffsetDateTime verifiedAt,
        @Nullable OffsetDateTime expiresAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8383658221756321454L;

}
