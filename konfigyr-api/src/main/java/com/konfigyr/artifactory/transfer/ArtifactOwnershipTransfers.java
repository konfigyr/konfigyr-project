package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * Service for requesting and resolving {@link ArtifactOwnershipTransfer} claims between namespaces.
 * <p>
 * A namespace that holds an {@link com.konfigyr.artifactory.ownership.VerificationState#ACTIVE} claim
 * on a {@code groupId} may request that another namespace's existing artifacts under that {@code groupId}
 * be transferred to it, resolving the permanent {@link com.konfigyr.artifactory.ArtifactOwnershipMismatchException}
 * that would otherwise result when a {@code groupId} claim is revoked and later re-claimed by a different
 * namespace, or {@link Owner}.
 * <p>
 * A transfer is a two-party handshake:
 * <ol>
 *     <li>The new claimant submits a request via {@link #request(Owner, String, Owner)}, creating a
 *     {@link TransferState#PENDING} {@link ArtifactOwnershipTransfer}.</li>
 *     <li>The current owner of the affected artifacts either {@link #accept(ArtifactOwnershipTransfer)}s the
 *     request, moving ownership of every artifact it holds under the {@code groupId} in one atomic action, or
 *     {@link #reject(ArtifactOwnershipTransfer)}s it.</li>
 *     <li>The requesting namespace may withdraw its own request via {@link #cancel(ArtifactOwnershipTransfer)}
 *     while it is still {@link TransferState#PENDING}.</li>
 * </ol>
 * Ownership never moves without an explicit acceptance from the current owner.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ArtifactOwnershipTransfer
 * @see TransferState
 */
@NullMarked
public interface ArtifactOwnershipTransfers {

	/**
	 * Returns a page of transfer requests where the supplied namespace is the current owner being asked to
	 * release artifacts.
	 *
	 * @param from the namespace that currently owns the artifacts in scope of the request
	 * @param query the search query containing filter and pagination instructions
	 * @return paged incoming transfer requests for that namespace matching the supplied search query
	 */
	Page<ArtifactOwnershipTransfer> findIncoming(Owner from, SearchQuery query);

	/**
	 * Returns a page of transfer requests submitted by the supplied namespace as the requesting new claimant.
	 *
	 * @param to the namespace that submitted the transfer requests
	 * @param query the search query containing filter and pagination instructions
	 * @return paged outgoing transfer requests for that namespace matching the supplied search query
	 */
	Page<ArtifactOwnershipTransfer> findOutgoing(Owner to, SearchQuery query);

	/**
	 * Finds a transfer request by its identifier, visible only to a namespace that is a party to it, either
	 * as the current owner ({@code from}) or the requesting namespace ({@code to}).
	 *
	 * @param owner the namespace looking up the transfer request
	 * @param id the transfer request identifier to look up
	 * @return the matching transfer request if present and visible to {@code owner}; otherwise an empty optional
	 */
	Optional<ArtifactOwnershipTransfer> findById(Owner owner, EntityId id);

	/**
	 * Requests that the artifacts a namespace holds under a {@code groupId} be transferred to a different
	 * namespace that holds an active claim on that {@code groupId}.
	 * <p>
	 * The request only takes effect once the current owner explicitly {@link #accept(ArtifactOwnershipTransfer) accepts}
	 * it.
	 *
	 * @param to the namespace requesting the transfer of ownership; must hold an active claim on {@code groupId}
	 * @param groupId the Maven group identifier whose artifacts should be transferred
	 * @param from the namespace that currently owns the affected artifacts
	 * @return the created {@link TransferState#PENDING} transfer request
	 * @throws com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException when {@code to} does not hold an
	 *         active claim covering {@code groupId}
	 * @throws NoArtifactsToTransferException when {@code from} owns no artifacts under {@code groupId}
	 * @throws ArtifactOwnershipTransferAlreadyRequestedException when a pending request already exists for the
	 *         same {@code groupId}, {@code from} and {@code to} combination
	 */
	@DomainEventPublisher(publishes = "artifactory.ownership-transfer.requested")
	ArtifactOwnershipTransfer request(Owner to, String groupId, Owner from);

	/**
	 * Accepts a pending transfer request.
	 * <p>
	 * Moves every artifact the {@code from} namespace holds under the transfer's {@code groupId} to the
	 * {@code to} namespace in one atomic action. Artifact
	 * {@link com.konfigyr.artifactory.ArtifactVisibility visibility} is untouched.
	 *
	 * @param transfer the transfer request to accept
	 * @return the accepted transfer with {@code resolvedAt} populated
	 * @throws IllegalStateException when the transfer is not in a state that can transition to {@link TransferState#ACCEPTED}
	 */
	@DomainEventPublisher(publishes = "artifactory.ownership-transfer.accepted")
	ArtifactOwnershipTransfer accept(ArtifactOwnershipTransfer transfer);

	/**
	 * Rejects a pending transfer request. No artifact ownership is changed.
	 *
	 * @param transfer the transfer request to reject
	 * @return the rejected transfer with {@code resolvedAt} populated
	 * @throws IllegalStateException when the transfer is not in a state that can transition to {@link TransferState#REJECTED}
	 */
	@DomainEventPublisher(publishes = "artifactory.ownership-transfer.rejected")
	ArtifactOwnershipTransfer reject(ArtifactOwnershipTransfer transfer);

	/**
	 * Cancels a pending transfer request on behalf of the requesting namespace. No artifact ownership is changed.
	 *
	 * @param transfer the transfer request to cancel
	 * @return the cancelled transfer with {@code resolvedAt} populated
	 * @throws IllegalStateException when the transfer is not in a state that can transition to {@link TransferState#CANCELLED}
	 */
	@DomainEventPublisher(publishes = "artifactory.ownership-transfer.cancelled")
	ArtifactOwnershipTransfer cancel(ArtifactOwnershipTransfer transfer);

}
