package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.support.SearchQuery;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Maven {@code groupId} ownership claims and their verification attempts.
 * <p>
 * This service is the enforcement gate that prevents a namespace from publishing artifact metadata
 * for a {@code groupId} it does not own. Before any artifact can be ingested through the
 * Artifactory, the publishing namespace must hold an {@link VerificationState#ACTIVE} claim on the
 * artifact's {@code groupId}. Use {@link #findActiveCovering(Owner, String)} to perform that check.
 * <p>
 * A claim follows this lifecycle:
 * <ol>
 *     <li>A namespace submits a claim via {@link #claim(Owner, String, VerificationMethod)}, which
 *     creates a {@link VerificationState#PENDING} {@link GroupVerification} and issues an initial
 *     {@link VerificationChallenge} for the chosen method.</li>
 *     <li>The namespace publishes the challenge token to the verification target (e.g. a DNS
 *     {@code TXT} record) and then calls {@link #verify(Owner, String)}. The appropriate
 *     {@link VerificationStrategy} checks the target and transitions the claim to
 *     {@link VerificationState#ACTIVE} on success.</li>
 *     <li>A transient failure (network error, token not yet propagated) leaves the challenge
 *     {@link ChallengeState#UNVERIFIED} and the claim {@link VerificationState#PENDING} so the
 *     namespace can retry.</li>
 *     <li>An {@link VerificationState#ACTIVE} or {@link VerificationState#PENDING} claim can be
 *     explicitly revoked via {@link #revoke(GroupVerification)}.</li>
 * </ol>
 * <p>
 * Ownership is prefix-based: a claim on {@code com.mycompany} covers {@code com.mycompany.utils}
 * and all other descendants. Accordingly, no two active claims may overlap in either direction,
 * see {@link #findAnyOverlapping(String)}.
 *
 * @author Vitalii Kushnir
 * @since 1.0.0
 * @see GroupVerification
 * @see VerificationChallenge
 * @see VerificationStrategy
 * @see VerificationState
 */
@NullMarked
public interface GroupVerifications {

	/**
	 * Finds an active claim owned by the given namespace that covers the supplied {@code groupId}.
	 * <p>
	 * The lookup uses prefix-based matching: it returns a claim whose {@code groupId} is either an
	 * exact match or a prefix of the requested value. A claim on {@code com.mycompany} therefore
	 * covers {@code com.mycompany}, {@code com.mycompany.utils}, and any other
	 * {@code com.mycompany.*} descendant.
	 * <p>
	 * This is the primary check used by the publishing enforcement gate: if this method returns empty,
	 * the namespace is not authorized to publish the artifact.
	 *
	 * @param owner   the namespace owner to search within
	 * @param groupId the artifact group identifier to check
	 * @return the active claim covering the groupId if one exists; otherwise an empty optional
	 */
	Optional<GroupVerification> findActiveCovering(Owner owner, String groupId);

	/**
	 * Finds any active claim that overlaps the supplied {@code groupId} in either direction.
	 * <p>
	 * Two claims overlap when one is a prefix of the other. Both directions are checked:
	 * <ul>
	 *     <li>A new claim for {@code com.mycompany.utils} conflicts with an existing claim for
	 *     {@code com.mycompany}; the child is already covered by the parent's prefix claim.</li>
	 *     <li>A new claim for {@code com.mycompany} conflicts with an existing claim for
	 *     {@code com.mycompany.utils}; the parent would silently absorb a child already owned by
	 *     another namespace.</li>
	 * </ul>
	 * This method must be called before {@link #claim(Owner, String, VerificationMethod)} to detect
	 * conflicts that the database unique index on exact {@code groupId} values cannot catch.
	 *
	 * @param groupId the group identifier to inspect
	 * @return an overlapping active claim if one exists; otherwise an empty optional
	 */
	Optional<GroupVerification> findAnyOverlapping(String groupId);

	/**
	 * Returns a page of verification claims owned by the supplied namespace matching the query.
	 * <p>
	 * Supports filtering by {@link VerificationState} via {@link GroupVerification#STATE_CRITERIA}
	 * and substring matching on the {@code groupId} via {@link com.konfigyr.support.SearchQuery#TERM}.
	 * Results can be sorted by {@code group} (the {@code groupId} column) or {@code date}
	 * (creation timestamp); the default order is creation timestamp descending.
	 *
	 * @param owner the namespace owner
	 * @param query the search query containing filter and pagination instructions
	 * @return paged claims for that owner matching the supplied search query
	 */
	Page<GroupVerification> findByOwner(Owner owner, SearchQuery query);

	/**
	 * Finds a claim for the supplied {@code groupId} within the given namespace.
	 *
	 * @param owner   the namespace owner
	 * @param groupId the group identifier to look up
	 * @return the matching claim if present; otherwise an empty optional
	 */
	Optional<GroupVerification> findByGroupId(Owner owner, String groupId);

	/**
	 * Finds the single {@link ChallengeState#UNVERIFIED} challenge attached to a verification claim.
	 * <p>
	 * At most one challenge is active at a time. When no unverified challenge exists, either because
	 * none has been issued yet or all previous attempts have been resolved, this returns empty.
	 *
	 * @param verification the verification claim
	 * @return the active challenge if one exists; otherwise an empty optional
	 */
	Optional<VerificationChallenge> findActiveChallenge(GroupVerification verification);

	/**
	 * Returns all challenges attached to a verification, ordered by creation time ascending.
	 * <p>
	 * This is the full attempt history: both resolved ({@link ChallengeState#VERIFIED} or
	 * {@link ChallengeState#EXPIRED}) and the current {@link ChallengeState#UNVERIFIED} challenge,
	 * if any.
	 *
	 * @param verification the verification for which verification challenges are retrieved
	 * @return the challenge history for the claim, in creation order
	 */
	List<VerificationChallenge> findChallenges(GroupVerification verification);

	/**
	 * Submits a new ownership claim for the supplied {@code groupId} on behalf of the namespace.
	 * <p>
	 * Creates a {@link VerificationState#PENDING} {@link GroupVerification} and an initial
	 * {@link ChallengeState#UNVERIFIED} {@link VerificationChallenge} for the chosen method, persists
	 * both records, and returns the {@link GroupVerification}. The challenge token, which the
	 * namespace must publish to the verification target, is not included in the returned record; use
	 * {@link #findActiveChallenge(GroupVerification)} to retrieve it.
	 * <p>
	 * {@link #findAnyOverlapping(String)} must be checked before calling this method. If an
	 * overlapping active claim exists, implementations must throw
	 * {@link GroupIdAlreadyClaimedException} before persisting anything.
	 *
	 * @param owner   the namespace owner that is claiming the group
	 * @param groupId the Maven group identifier to claim
	 * @param method  the verification method used to prove ownership
	 * @return the created {@link VerificationState#PENDING} verification claim
	 * @throws GroupIdAlreadyClaimedException when an active claim already covers the groupId
	 */
	GroupVerification claim(Owner owner, String groupId, VerificationMethod method);

	/**
	 * Reclaims an existing ownership claim for the supplied {@code groupId} using the given method.
	 * <p>
	 * The existing verification is loaded for the namespace, its active challenge is reset or
	 * recreated for the new {@link VerificationMethod}, and the claim is moved back to
	 * {@link VerificationState#PENDING}. Implementations only allow reclaiming claims that are in a
	 * reclaimable terminal state, such as {@link VerificationState#FAILED} or
	 * {@link VerificationState#REVOKED}.
	 *
	 * @param owner   the namespace owner that holds the claim
	 * @param groupId the group identifier to reclaim
	 * @param method  the verification method to use for the reclaimed claim
	 * @return the updated {@link VerificationState#PENDING} verification claim
	 * @throws GroupVerificationNotFoundException when no claim exists for the owner and groupId
	 * @throws IllegalStateException              when the claim is not in a reclaimable state
	 */
	GroupVerification claimAgain(Owner owner, String groupId, VerificationMethod method);

	/**
	 * Attempts to verify the ownership claim for the given {@code groupId} on behalf of the namespace.
	 * <p>
	 * Loads the claim and its active {@link ChallengeState#UNVERIFIED} challenge, then delegates to
	 * the {@link VerificationStrategy} matching the challenge's {@link VerificationMethod}. On a
	 * successful result the claim transitions to {@link VerificationState#ACTIVE} and
	 * {@code verifiedAt} is recorded. On a transient failure, like network errors, or token is not yet
	 * propagated, the challenge remains {@link ChallengeState#UNVERIFIED} and the claim remains
	 * {@link VerificationState#PENDING} so the namespace can retry without resubmitting.
	 *
	 * @param owner   the namespace owner that holds the claim
	 * @param groupId the group identifier to verify
	 * @return the updated verification claim reflecting the outcome
	 * @throws VerificationChallengeNotFoundException when no claim or no active challenge is found
	 */
	GroupVerification verify(Owner owner, String groupId);

	/**
	 * Revokes the supplied verification claim.
	 * <p>
	 * Only claims in {@link VerificationState#ACTIVE} or {@link VerificationState#PENDING} state
	 * can be revoked. Revoking an {@link VerificationState#ACTIVE} claim immediately removes
	 * publish authorization for the associated {@code groupId}: subsequent calls to
	 * {@link #findActiveCovering(Owner, String)} will return empty until a new claim is activated.
	 *
	 * @param verification the claim to revoke
	 * @return the revoked claim with {@code revokedAt} populated
	 * @throws IllegalStateException when the claim is not in a revocable state
	 */
	GroupVerification revoke(GroupVerification verification);
}
