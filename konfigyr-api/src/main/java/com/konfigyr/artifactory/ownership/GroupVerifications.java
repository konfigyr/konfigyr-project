package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.Repository;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing ownership claims and verification attempts.
 *
 * @author Vitalii Kushnir
 */
@NullMarked
@Repository
public interface GroupVerifications {

	/**
	 * Finds an active claim owned by the given namespace that covers the supplied groupId prefix.
	 *
	 * @param groupId the group identifier to resolve
	 * @param owner   the namespace owner to search within
	 * @return the active claim if one exists
	 */
	Optional<GroupVerification> findActiveCovering(String groupId, Owner owner);

	/**
	 * Finds any active claim that overlaps the supplied groupId in either direction.
	 *
	 * @param groupId the group identifier to inspect
	 * @return an overlapping active claim if one exists
	 */
	Optional<GroupVerification> findAnyOverlapping(String groupId);

	/**
	 * Lists all claims owned by the supplied namespace.
	 *
	 * @param owner the namespace owner
	 * @return all claims for that owner
	 */
	List<GroupVerification> findByOwner(Owner owner);

	/**
	 * Finds a claim for the supplied groupId within the given namespace.
	 *
	 * @param groupId the group identifier to look up
	 * @param owner   the namespace owner
	 * @return the matching claim if present
	 */
	Optional<GroupVerification> findByGroupId(String groupId, Owner owner);

	/**
	 * Finds the current unverified challenge attached to a verification claim.
	 *
	 * @param verification the verification claim
	 * @return the active challenge if one exists
	 */
	Optional<VerificationChallenge> findActiveChallenge(GroupVerification verification);

	/**
	 * Lists all challenges attached to a verification within the given namespace.
	 *
	 * @param verificationId the verification identifier to look up
	 * @param owner the namespace owner
	 * @return the challenge history for the claim
	 */
	List<VerificationChallenge> findChallenges(EntityId verificationId, Owner owner);

	/**
	 * Saves a new verification claim.
	 *
	 * @param verification the claim to save
	 * @return the saved claim
	 */
	GroupVerification save(GroupVerification verification);

	/**
	 * Saves a new verification challenge.
	 *
	 * @param challenge the challenge to save
	 * @return the saved challenge
	 */
	VerificationChallenge saveChallenge(VerificationChallenge challenge);
}
