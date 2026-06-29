package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Verification challenge attached to a {@link GroupVerification} claim.
 * <p>
 * A challenge holds the proof token that the namespace owner must publish in the verification
 * target appropriate for the chosen {@link VerificationMethod}. The {@link VerificationStrategy}
 * is responsible for checking whether the token is present at that target to confirm ownership.
 * <p>
 * The lifecycle starts at {@link ChallengeState#UNVERIFIED}. A successful strategy check
 * transitions the challenge to {@link ChallengeState#VERIFIED}; a failed check leaves it at
 * {@link ChallengeState#UNVERIFIED} so the owner can retry. A challenge that is no longer
 * eligible for verification transitions to {@link ChallengeState#EXPIRED}.
 * <p>
 * {@code expiresAt} is set only for time-limited challenges (e.g. {@link VerificationMethod#DNS});
 * it is {@literal null} for open-ended methods such as {@link VerificationMethod#SOURCE_CODE}.
 *
 * @param id             the persistent challenge identifier; never {@literal null}
 * @param verificationId the identifier of the owning {@link GroupVerification}; never {@literal null}
 * @param method         the verification method used to issue and validate this challenge; never {@literal null}
 * @param token          the opaque token the owner must publish in the verification target; never {@literal null}
 * @param state          the current challenge lifecycle state; never {@literal null}
 * @param createdAt      timestamp when the challenge was issued; {@literal null} before persistence
 * @param verifiedAt     timestamp when the challenge transitioned to {@link ChallengeState#VERIFIED}; {@literal null} otherwise
 * @param expiresAt      timestamp after which the challenge is no longer valid; {@literal null} for open-ended methods
 * @author Vitalii Kushnir
 * @since 1.0.0
 * @see GroupVerification
 * @see VerificationStrategy
 * @see ChallengeState
 */
@NullMarked
public record VerificationChallenge(
	UUID id,
	EntityId verificationId,
	VerificationMethod method,
	String token,
	ChallengeState state,
	@Nullable OffsetDateTime createdAt,
	@Nullable OffsetDateTime verifiedAt,
	@Nullable OffsetDateTime expiresAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 8383658221756321454L;

	/**
	 * Creates a new {@link Builder fluent builder} instance used to construct a {@link VerificationChallenge}.
	 *
	 * @return challenge builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create a {@link VerificationChallenge}.
	 */
	public static final class Builder {

		private @Nullable UUID id;
		private @Nullable EntityId verificationId;
		private @Nullable VerificationMethod method;
		private @Nullable String token;
		private @Nullable ChallengeState state;
		private @Nullable OffsetDateTime createdAt;
		private @Nullable OffsetDateTime verifiedAt;
		private @Nullable OffsetDateTime expiresAt;

		private Builder() {
		}

		/**
		 * Specify the persistent identifier of this {@link VerificationChallenge}.
		 *
		 * @param id challenge identifier; may be {@literal null} before persistence
		 * @return challenge builder
		 */
		public Builder id(@Nullable UUID id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the identifier of the {@link GroupVerification} this challenge belongs to.
		 *
		 * @param verificationId owning verification identifier; may be {@literal null} before persistence
		 * @return challenge builder
		 */
		public Builder verificationId(@Nullable EntityId verificationId) {
			this.verificationId = verificationId;
			return this;
		}

		/**
		 * Specify the {@link VerificationMethod} used to issue and validate this challenge.
		 *
		 * @param method verification method; must not be {@literal null}
		 * @return challenge builder
		 */
		public Builder method(VerificationMethod method) {
			this.method = method;
			return this;
		}

		/**
		 * Specify the opaque token the owner must publish in the verification target.
		 *
		 * @param token challenge token; must not be blank
		 * @return challenge builder
		 */
		public Builder token(String token) {
			this.token = token;
			return this;
		}

		/**
		 * Specify the current {@link ChallengeState} of this challenge.
		 *
		 * @param state challenge lifecycle state; must not be {@literal null}
		 * @return challenge builder
		 */
		public Builder state(ChallengeState state) {
			this.state = state;
			return this;
		}

		/**
		 * Specify when this challenge was issued.
		 *
		 * @param createdAt creation timestamp; may be {@literal null}
		 * @return challenge builder
		 */
		public Builder createdAt(@Nullable OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this challenge was verified.
		 *
		 * @param verifiedAt verification timestamp; may be {@literal null}
		 * @return challenge builder
		 */
		public Builder verifiedAt(@Nullable OffsetDateTime verifiedAt) {
			this.verifiedAt = verifiedAt;
			return this;
		}

		/**
		 * Specify when this challenge expires. Set only for time-limited methods; pass
		 * {@literal null} for open-ended methods such as {@link VerificationMethod#SOURCE_CODE}.
		 *
		 * @param expiresAt expiry timestamp; may be {@literal null}
		 * @return challenge builder
		 */
		public Builder expiresAt(@Nullable OffsetDateTime expiresAt) {
			this.expiresAt = expiresAt;
			return this;
		}

		/**
		 * Creates a new {@link VerificationChallenge} from the values set on this builder.
		 *
		 * @return verification challenge, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public VerificationChallenge build() {
			Assert.notNull(id, "Verification challenge identifier is required");
			Assert.notNull(verificationId, "Verification challenge must have a group verification id");
			Assert.notNull(method, "Verification challenge method must not be null");
			Assert.hasText(token, "Verification challenge token must not be blank");
			Assert.notNull(state, "Verification challenge state must not be null");

			return new VerificationChallenge(id, verificationId, method, token, state, createdAt, verifiedAt, expiresAt);
		}
	}
}
