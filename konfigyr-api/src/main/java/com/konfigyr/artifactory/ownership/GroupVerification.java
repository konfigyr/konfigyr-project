package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Ownership claim for a Maven {@code groupId} held by a namespace.
 * <p>
 * A group verification tracks which namespace owns the claim, the claimed {@code groupId}, and the
 * current lifecycle {@link VerificationState}. Ownership is confirmed by issuing one or more
 * {@link VerificationChallenge} attempts; each attempt is tracked separately and coordinated
 * by the {@link GroupVerifications} service.
 * <p>
 * The lifecycle starts at {@link VerificationState#PENDING} when the claim is first submitted.
 * A successful challenge transitions the claim to {@link VerificationState#ACTIVE}. A claim that
 * cannot be verified transitions to {@link VerificationState#FAILED}. An active or pending claim
 * can be explicitly revoked, transitioning to {@link VerificationState#REVOKED}.
 *
 * @param id         the persistent verification identifier; never {@literal null}
 * @param owner      the namespace that holds this claim; never {@literal null}
 * @param groupId    the claimed Maven group coordinate; never {@literal null}
 * @param state      the current verification lifecycle state; never {@literal null}
 * @param createdAt  timestamp when the claim was submitted; {@literal null} before persistence
 * @param verifiedAt timestamp when the claim transitioned to {@link VerificationState#ACTIVE}; {@literal null} otherwise
 * @param revokedAt  timestamp when the claim transitioned to {@link VerificationState#REVOKED}; {@literal null} otherwise
 * @author Vitalii Kushnir
 * @since 1.0.0
 * @see VerificationChallenge
 * @see VerificationState
 * @see GroupVerifications
 */
@NullMarked
@AggregateRoot
public record GroupVerification(
	@Identity EntityId id,
	Owner owner,
	String groupId,
	VerificationState state,
	@Nullable OffsetDateTime createdAt,
	@Nullable OffsetDateTime verifiedAt,
	@Nullable OffsetDateTime revokedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = -8982245386554212335L;

	/**
	 * Search criteria that can be used to filter group verifications by their state.
	 */
	public static final SearchQuery.Criteria<VerificationState> STATE_CRITERIA =
			SearchQuery.criteria("state", VerificationState.class);

	/**
	 * Creates a new {@link Builder fluent builder} instance used to construct a {@link GroupVerification}.
	 *
	 * @return group verification builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a new {@link Builder} pre-populated with all values from this verification, suitable
	 * for producing a modified copy.
	 *
	 * @return group verification builder, never {@literal null}
	 */
	public Builder toBuilder() {
		return new Builder()
				.id(this.id)
				.owner(this.owner)
				.groupId(this.groupId)
				.state(this.state)
				.createdAt(this.createdAt)
				.verifiedAt(this.verifiedAt)
				.revokedAt(this.revokedAt);
	}

	/**
	 * Fluent builder type used to create a {@link GroupVerification}.
	 */
	public static final class Builder {

		private @Nullable EntityId id;
		private @Nullable Owner owner;
		private @Nullable String groupId;
		private @Nullable VerificationState state;
		private @Nullable OffsetDateTime createdAt;
		private @Nullable OffsetDateTime verifiedAt;
		private @Nullable OffsetDateTime revokedAt;

		private Builder() {
		}

		/**
		 * Specify the persistent identifier of this {@link GroupVerification}.
		 *
		 * @param id verification identifier; may be {@literal null} before persistence
		 * @return group verification builder
		 */
		public Builder id(@Nullable Long id) {
			return id(id == null ? null : EntityId.from(id));
		}

		/**
		 * Specify the persistent identifier of this {@link GroupVerification}.
		 *
		 * @param id verification identifier; may be {@literal null} before persistence
		 * @return group verification builder
		 */
		public Builder id(@Nullable EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the {@link Owner} namespace that holds this claim.
		 *
		 * @param owner namespace owner; must not be {@literal null}
		 * @return group verification builder
		 */
		public Builder owner(Owner owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Specify the Maven group coordinate being claimed.
		 *
		 * @param groupId group identifier; must not be blank
		 * @return group verification builder
		 */
		public Builder groupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * Specify the current {@link VerificationState} of this claim.
		 *
		 * @param state verification lifecycle state; must not be {@literal null}
		 * @return group verification builder
		 */
		public Builder state(VerificationState state) {
			this.state = state;
			return this;
		}

		/**
		 * Specify when this claim was submitted.
		 *
		 * @param createdAt creation timestamp; may be {@literal null}
		 * @return group verification builder
		 */
		public Builder createdAt(@Nullable OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this claim was activated.
		 *
		 * @param verifiedAt activation timestamp; may be {@literal null}
		 * @return group verification builder
		 */
		public Builder verifiedAt(@Nullable OffsetDateTime verifiedAt) {
			this.verifiedAt = verifiedAt;
			return this;
		}

		/**
		 * Specify when this claim was revoked.
		 *
		 * @param revokedAt revocation timestamp; may be {@literal null}
		 * @return group verification builder
		 */
		public Builder revokedAt(@Nullable OffsetDateTime revokedAt) {
			this.revokedAt = revokedAt;
			return this;
		}

		/**
		 * Creates a new {@link GroupVerification} from the values set on this builder.
		 *
		 * @return group verification, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public GroupVerification build() {
			Assert.notNull(id, "Group verification identifier is required");
			Assert.notNull(owner, "Group verification owner must not be null");
			Assert.hasText(groupId, "Group verification group identifier must not be blank");
			Assert.notNull(state, "Group verification state must not be null");

			return new GroupVerification(id, owner, groupId, state, createdAt, verifiedAt, revokedAt);
		}
	}
}
