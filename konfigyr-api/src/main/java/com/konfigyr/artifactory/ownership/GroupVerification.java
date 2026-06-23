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

/**
 * Verification claim for a group identifier owned by a namespace.
 * <p>
 * A group verification tracks the namespace that owns the claim, the claimed {@code groupId}, and the
 * current lifecycle {@link VerificationState}. Timestamps are populated as the claim moves through the
 * verification flow.
 *
 * @param id         the persistent verification identifier; may be {@literal null} before the claim is saved
 * @param owner      the namespace owner that claims the group; never {@literal null}
 * @param groupId    the claimed group identifier; never {@literal null}
 * @param state      the current verification state; never {@literal null}
 * @param createdAt  timestamp when the claim was created; may be {@literal null} before persistence
 * @param verifiedAt timestamp when the claim was verified; may be {@literal null}
 * @param revokedAt  timestamp when the claim was revoked; may be {@literal null}
 * @author Vitalii Kushnir
 */
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
