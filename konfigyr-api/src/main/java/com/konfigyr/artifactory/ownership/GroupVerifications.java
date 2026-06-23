package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing ownership claims and verification attempts.
 *
 * @author Vitalii Kushnir
 */
@NullMarked
public interface GroupVerifications {

	/**
	 * Finds an active claim owned by the given namespace that covers the supplied groupId prefix.
	 * <p>
	 * The lookup matches either the exact {@code groupId} or any descendant group identifier that
	 * starts with {@code groupId + "."}.
	 *
	 * @param groupId the group identifier to resolve
	 * @param owner   the namespace owner to search within
	 * @return the active claim if one exists; otherwise an empty optional
	 */
	Optional<GroupVerification> findActiveCovering(String groupId, Owner owner);

	/**
	 * Finds any active claim that overlaps the supplied groupId in either direction.
	 * <p>
	 * This method is used before creating a new claim to ensure no other active verification already
	 * owns the same group identifier or one of its parent or child identifiers.
	 *
	 * @param groupId the group identifier to inspect
	 * @return an overlapping active claim if one exists; otherwise an empty optional
	 */
	Optional<GroupVerification> findAnyOverlapping(String groupId);

	/**
	 * Lists all claims owned by the supplied namespace.
	 *
	 * @param owner the namespace owner
	 * @return all claims for that owner, ordered by the underlying query
	 */
	List<GroupVerification> findByOwner(Owner owner);

	/**
	 * Finds a claim for the supplied groupId within the given namespace.
	 *
	 * @param groupId the group identifier to look up
	 * @param owner   the namespace owner
	 * @return the matching claim if present; otherwise an empty optional
	 */
	Optional<GroupVerification> findByGroupId(String groupId, Owner owner);

	/**
	 * Finds the current unverified challenge attached to a verification claim.
	 *
	 * @param verification the verification claim
	 * @return the active challenge if one exists; otherwise an empty optional
	 */
	Optional<VerificationChallenge> findActiveChallenge(GroupVerification verification);

	/**
	 * Lists all challenges attached to a verification within the given namespace.
	 * <p>
	 * The returned list is ordered by creation time ascending.
	 *
	 * @param verificationId the verification identifier to look up
	 * @param owner          the namespace owner
	 * @return the challenge history for the claim, in creation order
	 */
	List<VerificationChallenge> findChallenges(EntityId verificationId, Owner owner);

	/**
	 * Claims a new group verification for the supplied namespace owner.
	 * <p>
	 * Implementations are expected to create a pending verification, create an initial challenge for
	 * the supplied method, persist both records, and return the stored verification claim.
	 *
	 * @param owner   the namespace owner that claims the group
	 * @param groupId the group identifier to claim
	 * @param method  the verification method used to prove ownership
	 * @return the created verification claim
	 */
	GroupVerification claim(Owner owner, String groupId, VerificationMethod method);

	/**
	 * Verifies the supplied group verification for the given namespace owner.
	 * <p>
	 * Implementations should load the claim, resolve the active challenge, apply the verification
	 * strategy, and activate the claim when the verification succeeds.
	 *
	 * @param owner the namespace owner that owns the claim
	 * @param groupId the group identifier to verify
	 * @return the updated verification claim
	 */
	GroupVerification verify(Owner owner, String groupId);

	/**
	 * Revokes the supplied verification claim.
	 * <p>
	 * Implementations should reject claims that are not in a revocable state and return the stored
	 * revoked claim when the transition succeeds.
	 *
	 * @param verification the claim to revoke
	 * @return the revoked claim
	 */
	GroupVerification revoke(GroupVerification verification);

	/**
	 * Resolves the group owner for the given namespace slug.
	 * <p>
	 * This is a lookup helper used by the controller layer to translate a namespace path segment into
	 * an {@link Owner} reference for verification operations.
	 *
	 * @param namespace the namespace slug to resolve
	 * @return the matching owner if one exists; otherwise an empty optional
	 */
	Optional<Owner> findOwner(String namespace);
}
