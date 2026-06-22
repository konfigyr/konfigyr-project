package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

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

	private static final SecureRandom RANDOM = new SecureRandom();

	@NonNull
	public static VerificationChallenge issue(@NonNull VerificationMethod method) {
		Assert.notNull(method, "Verification method is required");

		final byte[] seed = new byte[20];
		RANDOM.nextBytes(seed);

		return VerificationChallenge.builder()
				.method(method)
				.token(Base64.getUrlEncoder().withoutPadding().encodeToString(seed))
				.state(ChallengeState.UNVERIFIED)
				.createdAt(OffsetDateTime.now())
				.build();
	}

	@NonNull
	public VerificationChallenge applyResult(@NonNull VerificationResult result) {
		Assert.notNull(result, "Verification result is required");

		if (state != ChallengeState.UNVERIFIED) {
			throw new VerificationChallengeNotFoundException("Cannot apply a result to a " + state + " challenge");
		}

		if (result instanceof VerificationResult.Success success) {
			Assert.state(success.method() == method, "Cannot apply a " + success.method() + " success to a " + method + " challenge");

			return toBuilder()
					.state(ChallengeState.VERIFIED)
					.verifiedAt(OffsetDateTime.now())
					.build();
		}

		return toBuilder()
				.state(ChallengeState.EXPIRED)
				.verifiedAt(null)
				.build();
	}

}
