package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.Owners;
import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.OptionalAssert;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static org.assertj.core.api.Assertions.*;

class DefaultArtifactOwnershipTransfersTest extends AbstractIntegrationTest {

	@Autowired
	ArtifactOwnershipTransfers transfers;

	@Autowired
	DSLContext context;

	@Test
	@Transactional
	@DisplayName("should request and accept a transfer, moving ownership of the affected artifact")
	void shouldRequestAndAcceptTransfer(AssertablePublishedEvents events) {
		final var to = Owners.konfigyr();
		final var from = Owners.johnDoe();
		final var groupId = "com.konfigyr";

		final var pending = transfers.request(to, groupId, from);
		assertThat(pending)
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.requestedAt()).isNotNull())
				.returns(groupId, ArtifactOwnershipTransfer::groupId)
				.returns(from, ArtifactOwnershipTransfer::from)
				.returns(to, ArtifactOwnershipTransfer::to)
				.returns(TransferState.PENDING, ArtifactOwnershipTransfer::state)
				.returns(null, ArtifactOwnershipTransfer::resolvedAt);

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("reclaimed-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("artifact should still be owned by the original namespace before accepting")
				.isEqualTo(from.id().get());

		final var accepted = transfers.accept(pending);
		assertThat(accepted)
				.returns(pending.id(), ArtifactOwnershipTransfer::id)
				.returns(TransferState.ACCEPTED, ArtifactOwnershipTransfer::state)
				.satisfies(it -> assertThat(it.resolvedAt()).isNotNull());

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("reclaimed-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("artifact ownership should have moved to the new claimant")
				.isEqualTo(to.id().get());

		events.assertThat()
				.contains(com.konfigyr.artifactory.ArtifactoryEvent.OwnershipTransferAccepted.class)
				.matching(ArtifactoryEvent.OwnershipTransferAccepted::groupId, groupId)
				.matching(ArtifactoryEvent.OwnershipTransferAccepted::from, from)
				.matching(ArtifactoryEvent.OwnershipTransferAccepted::to, to);
	}

	@Test
	@DisplayName("should fail to request a transfer when the requester has no active claim covering the groupId")
	void shouldRejectRequestWhenRequesterHasNoActiveClaim() {
		final var from = Owners.konfigyr();
		final var groupId = "com.konfigyr";

		assertThatExceptionOfType(GroupIdNotVerifiedException.class)
				.isThrownBy(() -> transfers.request(Owners.johnDoe(), groupId, from))
				.returns(groupId, GroupIdNotVerifiedException::getGroupId)
				.returns(Owners.johnDoe(), GroupIdNotVerifiedException::getOwner);
	}

	@Test
	@DisplayName("should fail to request a transfer when the current owner has no artifacts under the groupId")
	void shouldRejectRequestWhenFromOwnerHasNoArtifacts() {
		final var from = new Owner(EntityId.from(999L), "ghost-namespace");
		final var groupId = "com.konfigyr";

		assertThatExceptionOfType(NoArtifactsToTransferException.class)
				.isThrownBy(() -> transfers.request(Owners.konfigyr(), groupId, from))
				.returns(groupId, NoArtifactsToTransferException::getGroupId)
				.returns(from, NoArtifactsToTransferException::getFrom);
	}

	@Test
	@Transactional
	@DisplayName("should fail to request a duplicate pending transfer for the same groupId, from and to")
	void shouldRejectDuplicatePendingRequest() {
		final var to = Owners.konfigyr();
		final var from = Owners.johnDoe();
		final var groupId = "com.konfigyr";

		assertThatNoException().isThrownBy(() -> transfers.request(to, groupId, from));

		assertThatExceptionOfType(ArtifactOwnershipTransferAlreadyRequestedException.class)
				.isThrownBy(() -> transfers.request(to, groupId, from))
				.returns(groupId, ArtifactOwnershipTransferAlreadyRequestedException::getGroupId)
				.returns(from, ArtifactOwnershipTransferAlreadyRequestedException::getFrom)
				.returns(to, ArtifactOwnershipTransferAlreadyRequestedException::getTo);
	}

	@Test
	@DisplayName("should fail to accept a transfer that is already resolved")
	void shouldRejectAcceptingAlreadyResolvedTransfer() {
		final var accepted = transferFor(Owners.konfigyr(), 2);

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> transfers.accept(accepted));
	}

	@Test
	@Transactional
	@DisplayName("should reject a pending transfer")
	void shouldRejectPendingTransfer(AssertablePublishedEvents events) {
		final var pending = transferFor(Owners.ebf(), 1);

		assertThat(transfers.reject(pending))
				.returns(pending.id(), ArtifactOwnershipTransfer::id)
				.returns(TransferState.REJECTED, ArtifactOwnershipTransfer::state)
				.satisfies(it -> assertThat(it.resolvedAt()).isNotNull());

		events.assertThat()
				.contains(ArtifactoryEvent.OwnershipTransferRejected.class)
				.matching(ArtifactoryEvent.OwnershipTransferRejected::groupId, pending.groupId())
				.matching(ArtifactoryEvent.OwnershipTransferRejected::from, pending.from())
				.matching(ArtifactoryEvent.OwnershipTransferRejected::to, pending.to());
	}

	@Test
	@Transactional
	@DisplayName("should cancel a pending transfer")
	void shouldCancelPendingTransfer(AssertablePublishedEvents events) {
		final var pending = transferFor(Owners.ebf(), 1);

		assertThat(transfers.cancel(pending))
				.returns(pending.id(), ArtifactOwnershipTransfer::id)
				.returns(TransferState.CANCELLED, ArtifactOwnershipTransfer::state)
				.satisfies(it -> assertThat(it.resolvedAt()).isNotNull());

		events.assertThat()
				.contains(ArtifactoryEvent.OwnershipTransferCancelled.class)
				.matching(ArtifactoryEvent.OwnershipTransferCancelled::groupId, pending.groupId())
				.matching(ArtifactoryEvent.OwnershipTransferCancelled::from, pending.from())
				.matching(ArtifactoryEvent.OwnershipTransferCancelled::to, pending.to());
	}

	@Test
	@Transactional
	@DisplayName("should only move artifacts owned by the requested 'from' namespace, leaving other owners untouched")
	void shouldOnlyMoveArtifactsOwnedByRequestedFromNamespace() {
		final var groupId = "com.konfigyr";
		final var to = Owners.konfigyr();
		final var from = Owners.johnDoe();
		final var uninvolved = Owners.ebf();

		final var pending = transfers.request(to, groupId, from);
		transfers.accept(pending);

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("reclaimed-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("the requested 'from' namespace's artifact should have moved")
				.isEqualTo(to.id().get());

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("ebf-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("a third, uninvolved namespace's artifact under the same groupId must be untouched")
				.isEqualTo(uninvolved.id().get());
	}

	@Test
	@DisplayName("should find incoming transfer requests for the current owner")
	void shouldFindIncomingTransfers() {
		assertThat(transfers.findIncoming(Owners.konfigyr(), SearchQuery.of(Pageable.ofSize(10))).stream())
				.extracting(ArtifactOwnershipTransfer::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(3));
	}

	@Test
	@DisplayName("should find outgoing transfer requests for the requesting namespace")
	void shouldFindOutgoingTransfers() {
		assertThat(transfers.findOutgoing(Owners.ebf(), SearchQuery.of(Pageable.ofSize(10))).stream())
				.extracting(ArtifactOwnershipTransfer::id)
				.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(4));
	}

	@Test
	@DisplayName("should find a transfer request by id when visible to either party")
	void shouldFindTransferByIdForEitherParty() {
		assertTransfer(Owners.johnDoe(), 2).isPresent();
		assertTransfer(Owners.konfigyr(), 2).isPresent();
	}

	@Test
	@DisplayName("should not find a transfer request by id for an uninvolved namespace")
	void shouldNotFindTransferByIdForUninvolvedNamespace() {
		assertTransfer(Owners.ebf(), 2).isEmpty();
	}

	OptionalAssert<ArtifactOwnershipTransfer> assertTransfer(Owner owner, long id) {
		return assertThat(transfers.findById(owner, EntityId.from(id)));
	}

	ArtifactOwnershipTransfer transferFor(Owner owner, long id) {
		return assertTransfer(owner, id)
				.as("Artifact ownership transfer with identifier %s must be accessible to %s", id, owner.slug())
				.isPresent()
				.get()
				.actual();
	}

}
