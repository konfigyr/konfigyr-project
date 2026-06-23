package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import lombok.Builder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
}
