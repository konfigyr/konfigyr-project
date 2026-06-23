package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

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
