package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.Owners;
import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
				.matching(event -> event.groupId().equals(groupId)
						&& event.from().equals(from)
						&& event.to().equals(to));
	}

	@Test
	@DisplayName("should fail to request a transfer when the requester has no active claim covering the groupId")
	void shouldRejectRequestWhenRequesterHasNoActiveClaim() {
		final var to = Owners.johnDoe();
		final var from = Owners.konfigyr();
		final var groupId = "com.konfigyr";

		assertThatExceptionOfType(GroupIdNotVerifiedException.class)
				.isThrownBy(() -> transfers.request(to, groupId, from))
				.returns(groupId, GroupIdNotVerifiedException::getGroupId)
				.returns("john-doe", ex -> ex.getOwner().slug());
	}

	@Test
	@DisplayName("should fail to request a transfer when the current owner has no artifacts under the groupId")
	void shouldRejectRequestWhenFromOwnerHasNoArtifacts() {
		final var to = Owners.konfigyr();
		final var from = new Owner(EntityId.from(999L), "ghost-namespace");
		final var groupId = "com.konfigyr";

		assertThatExceptionOfType(NoArtifactsToTransferException.class)
				.isThrownBy(() -> transfers.request(to, groupId, from))
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

		transfers.request(to, groupId, from);

		assertThatExceptionOfType(ArtifactOwnershipTransferAlreadyRequestedException.class)
				.isThrownBy(() -> transfers.request(to, groupId, from));
	}

	@Test
	@Transactional
	@DisplayName("should fail to accept a transfer that is already resolved")
	void shouldRejectAcceptingAlreadyResolvedTransfer() {
		final var pending = transfers.request(Owners.konfigyr(), "com.konfigyr", Owners.johnDoe());
		final var accepted = transfers.accept(pending);

		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> transfers.accept(accepted));
	}

	@Test
	@Transactional
	@DisplayName("should reject a pending transfer without changing artifact ownership")
	void shouldRejectPendingTransfer() {
		final var groupId = "com.konfigyr";
		final var from = Owners.johnDoe();

		final var pending = transfers.request(Owners.konfigyr(), groupId, from);
		final var rejected = transfers.reject(pending);

		assertThat(rejected)
				.returns(pending.id(), ArtifactOwnershipTransfer::id)
				.returns(TransferState.REJECTED, ArtifactOwnershipTransfer::state)
				.satisfies(it -> assertThat(it.resolvedAt()).isNotNull());

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("reclaimed-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("artifact ownership must be untouched by a rejected transfer")
				.isEqualTo(from.id().get());
	}

	@Test
	@Transactional
	@DisplayName("should cancel a pending transfer without changing artifact ownership")
	void shouldCancelPendingTransfer() {
		final var groupId = "com.konfigyr";
		final var from = Owners.johnDoe();

		final var pending = transfers.request(Owners.konfigyr(), groupId, from);
		final var cancelled = transfers.cancel(pending);

		assertThat(cancelled)
				.returns(pending.id(), ArtifactOwnershipTransfer::id)
				.returns(TransferState.CANCELLED, ArtifactOwnershipTransfer::state)
				.satisfies(it -> assertThat(it.resolvedAt()).isNotNull());

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("reclaimed-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("artifact ownership must be untouched by a cancelled transfer")
				.isEqualTo(from.id().get());
	}

	@Test
	@Transactional
	@DisplayName("should only move artifacts owned by the requested 'from' namespace, leaving other owners untouched")
	void shouldOnlyMoveArtifactsOwnedByRequestedFromNamespace() {
		final var groupId = "com.konfigyr";
		final var to = Owners.konfigyr();
		final var from = Owners.johnDoe();

		final var thirdNamespaceId = context.insertInto(NAMESPACES)
				.set(NAMESPACES.ID, EntityId.generate().orElseThrow().get())
				.set(NAMESPACES.SLUG, "third-namespace")
				.set(NAMESPACES.NAME, "Third Namespace")
				.set(NAMESPACES.CREATED_AT, OffsetDateTime.now())
				.set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
				.returning(NAMESPACES.ID)
				.fetchOne(NAMESPACES.ID);

		context.insertInto(ARTIFACTS)
				.set(ARTIFACTS.ID, EntityId.generate().orElseThrow().get())
				.set(ARTIFACTS.NAMESPACE_ID, thirdNamespaceId)
				.set(ARTIFACTS.GROUP_ID, groupId)
				.set(ARTIFACTS.ARTIFACT_ID, "third-namespace-artifact")
				.set(ARTIFACTS.VISIBILITY, "PRIVATE")
				.set(ARTIFACTS.CREATED_AT, OffsetDateTime.now())
				.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
				.execute();

		final var pending = transfers.request(to, groupId, from);
		transfers.accept(pending);

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("reclaimed-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("the requested 'from' namespace's artifact should have moved")
				.isEqualTo(to.id().get());

		assertThat(context.select(ARTIFACTS.NAMESPACE_ID).from(ARTIFACTS)
				.where(ARTIFACTS.GROUP_ID.eq(groupId), ARTIFACTS.ARTIFACT_ID.eq("third-namespace-artifact"))
				.fetchOne(ARTIFACTS.NAMESPACE_ID))
				.as("a third, uninvolved namespace's artifact under the same groupId must be untouched")
				.isEqualTo(thirdNamespaceId);
	}

	@Test
	@Transactional
	@DisplayName("should find incoming transfer requests for the current owner")
	void shouldFindIncomingTransfers() {
		final var from = Owners.johnDoe();
		final var pending = transfers.request(Owners.konfigyr(), "com.konfigyr", from);

		assertThat(transfers.findIncoming(from, SearchQuery.of(Pageable.ofSize(10))).stream())
				.extracting(ArtifactOwnershipTransfer::id)
				.containsExactly(pending.id());
	}

	@Test
	@Transactional
	@DisplayName("should find outgoing transfer requests for the requesting namespace")
	void shouldFindOutgoingTransfers() {
		final var to = Owners.konfigyr();
		final var pending = transfers.request(to, "com.konfigyr", Owners.johnDoe());

		assertThat(transfers.findOutgoing(to, SearchQuery.of(Pageable.ofSize(10))).stream())
				.extracting(ArtifactOwnershipTransfer::id)
				.containsExactly(pending.id());
	}

	@Test
	@Transactional
	@DisplayName("should find a transfer request by id when visible to either party")
	void shouldFindTransferByIdForEitherParty() {
		final var to = Owners.konfigyr();
		final var from = Owners.johnDoe();
		final var pending = transfers.request(to, "com.konfigyr", from);

		assertThat(transfers.findById(to, pending.id())).isPresent();
		assertThat(transfers.findById(from, pending.id())).isPresent();
	}

	@Test
	@Transactional
	@DisplayName("should not find a transfer request by id for an uninvolved namespace")
	void shouldNotFindTransferByIdForUninvolvedNamespace() {
		final var pending = transfers.request(Owners.konfigyr(), "com.konfigyr", Owners.johnDoe());
		final var uninvolved = new Owner(EntityId.from(999L), "uninvolved-namespace");

		assertThat(transfers.findById(uninvolved, pending.id())).isEmpty();
	}

}
