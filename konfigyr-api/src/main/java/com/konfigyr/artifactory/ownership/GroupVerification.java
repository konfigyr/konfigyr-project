package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import lombok.Builder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@AggregateRoot
@Builder(toBuilder = true)
public record GroupVerification(
		@Nullable @Identity EntityId id,
		@NonNull Owner owner,
		@NonNull String groupId,
		@NonNull VerificationState state,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime verifiedAt,
		@Nullable OffsetDateTime revokedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = -8982245386554212335L;

	@NonNull
	public static GroupVerification claim(@NonNull Owner owner, @NonNull String groupId) {
		Assert.notNull(owner, "Owner is required");
		Assert.hasText(groupId, "GroupId is required");

		return GroupVerification.builder()
				.owner(owner)
				.groupId(groupId)
				.state(VerificationState.PENDING)
				.createdAt(OffsetDateTime.now())
				.build();
	}

	@NonNull
	public GroupVerification activate() {
		if (state != VerificationState.PENDING) {
			throw new VerificationChallengeNotFoundException("Cannot activate a " + state + " verification");
		}

		return toBuilder()
				.state(VerificationState.ACTIVE)
				.verifiedAt(OffsetDateTime.now())
				.build();
	}

	@NonNull
	public GroupVerification revoke() {
		if (state != VerificationState.ACTIVE) {
			throw new VerificationChallengeNotFoundException("Cannot revoke a " + state + " verification");
		}

		return toBuilder()
				.state(VerificationState.REVOKED)
				.revokedAt(OffsetDateTime.now())
				.build();
	}

}
