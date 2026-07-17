package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.artifactory.ownership.GroupVerifications;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.data.tables.Namespaces;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.konfigyr.data.tables.ArtifactOwnershipTransfers.ARTIFACT_OWNERSHIP_TRANSFERS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/*
 * Every read or write against ARTIFACTS.NAMESPACE_ID that this transfer workflow needs is performed
 * directly by this class rather than delegated to Artifactory. That interface is the general-purpose,
 * broadly-held entry point to the artifact repository domain; a bulk UPDATE ARTIFACTS reassigning
 * ownership has no business being reachable from it outside the two-party consent handshake modeled here.
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultArtifactOwnershipTransfers implements ArtifactOwnershipTransfers {

	static final Namespaces FROM_NAMESPACES = NAMESPACES.as("from_namespace");
	static final Namespaces TO_NAMESPACES = NAMESPACES.as("to_namespace");

	private static final PageableExecutor transferPageableExecutor = PageableExecutor.builder()
			.defaultSortField(ARTIFACT_OWNERSHIP_TRANSFERS.REQUESTED_AT.desc())
			.sortField("date", ARTIFACT_OWNERSHIP_TRANSFERS.REQUESTED_AT)
			.sortField("group", ARTIFACT_OWNERSHIP_TRANSFERS.GROUP_ID)
			.build();

	private final DSLContext context;
	private final GroupVerifications groupVerifications;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true, label = "artifact-ownership-transfers.find-incoming")
	public Page<ArtifactOwnershipTransfer> findIncoming(Owner from, SearchQuery query) {
		return findByCondition(ARTIFACT_OWNERSHIP_TRANSFERS.FROM_NAMESPACE_ID.eq(from.id().get()), query);
	}

	@Override
	@Transactional(readOnly = true, label = "artifact-ownership-transfers.find-outgoing")
	public Page<ArtifactOwnershipTransfer> findOutgoing(Owner to, SearchQuery query) {
		return findByCondition(ARTIFACT_OWNERSHIP_TRANSFERS.TO_NAMESPACE_ID.eq(to.id().get()), query);
	}

	@Override
	@Transactional(readOnly = true, label = "artifact-ownership-transfers.find-by-id")
	public Optional<ArtifactOwnershipTransfer> findById(Owner owner, EntityId id) {
		return createTransferQuery(DSL.and(
				ARTIFACT_OWNERSHIP_TRANSFERS.ID.eq(id.get()),
				ARTIFACT_OWNERSHIP_TRANSFERS.FROM_NAMESPACE_ID.eq(owner.id().get())
						.or(ARTIFACT_OWNERSHIP_TRANSFERS.TO_NAMESPACE_ID.eq(owner.id().get()))
		)).fetchOptional(DefaultArtifactOwnershipTransfers::toArtifactOwnershipTransfer);
	}

	@Override
	@Transactional(label = "artifact-ownership-transfers.request")
	public ArtifactOwnershipTransfer request(Owner to, String groupId, Owner from) {
		log.debug("Attempting to request an artifact ownership transfer: [to={}, groupId={}, from={}]", to, groupId, from);

		groupVerifications.findActiveCovering(to, groupId)
				.orElseThrow(() -> new GroupIdNotVerifiedException(groupId, to));

		if (!findArtifactOwners(groupId, to).contains(from)) {
			throw new NoArtifactsToTransferException(groupId, from);
		}

		try {
			final ArtifactOwnershipTransfer transfer = context.insertInto(ARTIFACT_OWNERSHIP_TRANSFERS)
					.set(
							SettableRecord.of(context, ARTIFACT_OWNERSHIP_TRANSFERS)
									.set(ARTIFACT_OWNERSHIP_TRANSFERS.ID, EntityId.generate().map(EntityId::get))
									.set(ARTIFACT_OWNERSHIP_TRANSFERS.GROUP_ID, groupId)
									.set(ARTIFACT_OWNERSHIP_TRANSFERS.FROM_NAMESPACE_ID, from.id().get())
									.set(ARTIFACT_OWNERSHIP_TRANSFERS.TO_NAMESPACE_ID, to.id().get())
									.set(ARTIFACT_OWNERSHIP_TRANSFERS.STATE, TransferState.PENDING.name())
									.set(ARTIFACT_OWNERSHIP_TRANSFERS.REQUESTED_AT, OffsetDateTime.now())
									.get()
					)
					.returning(ARTIFACT_OWNERSHIP_TRANSFERS.fields())
					.fetchOne(record -> toArtifactOwnershipTransfer(record, from, to));

			Assert.state(transfer != null, () -> "Could not create artifact ownership transfer for: [to=%s, groupId=%s, from=%s]"
					.formatted(to.slug(), groupId, from.slug()));

			log.info("Successfully requested artifact ownership transfer {} for groupId '{}' from namespace {} ({}) to namespace {} ({})",
					transfer.id(), groupId, from.slug(), from.id(), to.slug(), to.id());

			return transfer;
		} catch (DataIntegrityViolationException ex) {
			throw new ArtifactOwnershipTransferAlreadyRequestedException(groupId, from, to);
		}
	}

	@Override
	@Transactional(label = "artifact-ownership-transfers.accept")
	public ArtifactOwnershipTransfer accept(ArtifactOwnershipTransfer transfer) {
		final ArtifactOwnershipTransfer accepted = resolve(transfer, TransferState.ACCEPTED);

		transferArtifacts(accepted.from(), accepted.to(), accepted.groupId());

		eventPublisher.publishEvent(new ArtifactoryEvent.OwnershipTransferAccepted(
				accepted.id(), accepted.groupId(), accepted.from(), accepted.to()
		));

		log.info("Successfully accepted artifact ownership transfer {}, moved artifacts under groupId '{}' from namespace {} ({}) to namespace {} ({})",
				accepted.id(), accepted.groupId(), accepted.from().slug(), accepted.from().id(), accepted.to().slug(), accepted.to().id());

		return accepted;
	}

	@Override
	@Transactional(label = "artifact-ownership-transfers.reject")
	public ArtifactOwnershipTransfer reject(ArtifactOwnershipTransfer transfer) {
		final ArtifactOwnershipTransfer rejected = resolve(transfer, TransferState.REJECTED);

		log.info("Successfully rejected artifact ownership transfer {} for groupId '{}'", rejected.id(), rejected.groupId());

		return rejected;
	}

	@Override
	@Transactional(label = "artifact-ownership-transfers.cancel")
	public ArtifactOwnershipTransfer cancel(ArtifactOwnershipTransfer transfer) {
		final ArtifactOwnershipTransfer cancelled = resolve(transfer, TransferState.CANCELLED);

		log.info("Successfully cancelled artifact ownership transfer {} for groupId '{}'", cancelled.id(), cancelled.groupId());

		return cancelled;
	}

	private ArtifactOwnershipTransfer resolve(ArtifactOwnershipTransfer transfer, TransferState state) {
		Assert.state(
				transfer.state().canTransitionTo(state),
				() -> "Can only transition a pending artifact ownership transfer, but it was in a '%s' state".formatted(transfer.state())
		);

		final ArtifactOwnershipTransfer resolved = transfer.toBuilder()
				.state(state)
				.resolvedAt(OffsetDateTime.now())
				.build();

		context.update(ARTIFACT_OWNERSHIP_TRANSFERS)
				.set(ARTIFACT_OWNERSHIP_TRANSFERS.STATE, resolved.state().name())
				.set(ARTIFACT_OWNERSHIP_TRANSFERS.RESOLVED_AT, resolved.resolvedAt())
				.where(ARTIFACT_OWNERSHIP_TRANSFERS.ID.eq(resolved.id().get()))
				.execute();

		return resolved;
	}

	/*
	 * Returns the distinct namespaces, other than excluding, that own at least one artifact under the
	 * given groupId. Used to validate a transfer request: the requested 'from' namespace must appear in
	 * this set before a transfer can be created.
	 */
	private Set<Owner> findArtifactOwners(String groupId, Owner excluding) {
		return context.selectDistinct(ARTIFACTS.NAMESPACE_ID, NAMESPACES.SLUG)
				.from(ARTIFACTS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(ARTIFACTS.NAMESPACE_ID))
				.where(ARTIFACTS.GROUP_ID.eq(groupId))
				.and(ARTIFACTS.NAMESPACE_ID.ne(excluding.id().get()))
				.fetchSet(record -> new Owner(EntityId.from(record.get(ARTIFACTS.NAMESPACE_ID)), record.get(NAMESPACES.SLUG)));
	}

	/*
	 * Moves ownership of every artifact the 'from' namespace holds under the given groupId to the 'to'
	 * namespace, in a single bulk operation. Artifact visibility is untouched. Only ever called from
	 * accept(), after the transition assertion above has already confirmed both parties consented.
	 */
	private void transferArtifacts(Owner from, Owner to, String groupId) {
		context.update(ARTIFACTS)
				.set(ARTIFACTS.NAMESPACE_ID, to.id().get())
				.set(ARTIFACTS.UPDATED_AT, OffsetDateTime.now())
				.where(ARTIFACTS.GROUP_ID.eq(groupId))
				.and(ARTIFACTS.NAMESPACE_ID.eq(from.id().get()))
				.execute();
	}

	private Page<ArtifactOwnershipTransfer> findByCondition(Condition ownerCondition, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(ownerCondition);

		query.term().ifPresent(term -> conditions.add(ARTIFACT_OWNERSHIP_TRANSFERS.GROUP_ID.containsIgnoreCase(term)));

		return transferPageableExecutor.execute(
				createTransferQuery(DSL.and(conditions)),
				DefaultArtifactOwnershipTransfers::toArtifactOwnershipTransfer,
				query.pageable(),
				() -> context.fetchCount(createTransferQuery(DSL.and(conditions)))
		);
	}

	private SelectConditionStep<? extends Record> createTransferQuery(Condition condition) {
		return context.select(ARTIFACT_OWNERSHIP_TRANSFERS.fields())
				.select(FROM_NAMESPACES.SLUG, TO_NAMESPACES.SLUG)
				.from(ARTIFACT_OWNERSHIP_TRANSFERS)
				.innerJoin(FROM_NAMESPACES).on(FROM_NAMESPACES.ID.eq(ARTIFACT_OWNERSHIP_TRANSFERS.FROM_NAMESPACE_ID))
				.innerJoin(TO_NAMESPACES).on(TO_NAMESPACES.ID.eq(ARTIFACT_OWNERSHIP_TRANSFERS.TO_NAMESPACE_ID))
				.where(condition);
	}

	private static ArtifactOwnershipTransfer toArtifactOwnershipTransfer(Record record) {
		return toArtifactOwnershipTransfer(
				record,
				new Owner(EntityId.from(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.FROM_NAMESPACE_ID)), record.get(FROM_NAMESPACES.SLUG)),
				new Owner(EntityId.from(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.TO_NAMESPACE_ID)), record.get(TO_NAMESPACES.SLUG))
		);
	}

	private static ArtifactOwnershipTransfer toArtifactOwnershipTransfer(Record record, Owner from, Owner to) {
		return ArtifactOwnershipTransfer.builder()
				.id(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.ID, EntityId.class))
				.groupId(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.GROUP_ID))
				.from(from)
				.to(to)
				.state(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.STATE, TransferState.class))
				.requestedAt(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.REQUESTED_AT))
				.resolvedAt(record.get(ARTIFACT_OWNERSHIP_TRANSFERS.RESOLVED_AT))
				.build();
	}

}
