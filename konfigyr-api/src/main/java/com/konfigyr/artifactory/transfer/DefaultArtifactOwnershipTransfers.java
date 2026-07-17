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

/**
 * Default {@link ArtifactOwnershipTransfers} implementation.
 * <p>
 * {@link #request(Owner, String, Owner)}, {@link #accept(ArtifactOwnershipTransfer)}, {@link #reject(ArtifactOwnershipTransfer)}
 * and {@link #cancel(ArtifactOwnershipTransfer)} drive the {@code artifact_ownership_transfers} table through
 * the {@link TransferState} lifecycle, following the same shape as
 * {@code com.konfigyr.artifactory.ownership.DefaultGroupVerifications}: a mapper method converts jOOQ
 * {@link Record records} into the aggregate, and each mutation re-reads the resulting state from the
 * database rather than trusting the in-memory copy.
 * <p>
 * Two of those methods also read or write {@code ARTIFACTS.NAMESPACE_ID} directly: {@link #findArtifactOwners(String, Owner)}
 * resolves the candidate owners a {@link #request(Owner, String, Owner) request} can be made against, and
 * {@link #transferArtifacts(Owner, Owner, String)} performs the bulk reassignment once
 * {@link #accept(ArtifactOwnershipTransfer)} has confirmed the transition is valid. Elsewhere in this module,
 * {@code com.konfigyr.artifactory.DefaultArtifactory} is the sole writer of that column; it is bypassed here
 * on purpose. Exposing a generic "move these artifacts" method on the public {@code Artifactory} interface
 * would let any holder of that interface reassign ownership outside this two-party consent flow entirely,
 * which is exactly the failure mode this aggregate exists to prevent. Keeping both operations private to this
 * class means the only path to a bulk ownership move is through an {@link ArtifactOwnershipTransfer} that has
 * actually been accepted.
 * <p>
 * {@link #accept(ArtifactOwnershipTransfer)} performs its state transition and the {@link #transferArtifacts(Owner, Owner, String)}
 * write in the same database transaction, and only publishes {@link ArtifactoryEvent.OwnershipTransferAccepted}
 * once both have succeeded. This is deliberate: if the two were split across a transaction boundary, for
 * instance by moving the artifacts from an {@code @TransactionalEventListener} reacting to the event instead,
 * a failure in the second step would leave a transfer permanently marked {@link TransferState#ACCEPTED} whose
 * artifacts never actually moved — the same unresolvable, stuck state this whole feature was built to eliminate.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ArtifactOwnershipTransfer
 * @see TransferState
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

	/**
	 * Returns the distinct namespaces, other than {@code excluding}, that own at least one artifact under
	 * the given {@code groupId}.
	 * <p>
	 * Called from {@link #request(Owner, String, Owner)} to validate that the requested {@code from}
	 * namespace actually owns something worth transferring before a {@link ArtifactOwnershipTransfer} is
	 * created for it.
	 *
	 * @param groupId the Maven group identifier to inspect
	 * @param excluding the namespace to exclude from the result, namely the requesting {@code to} namespace
	 * @return the distinct owning namespaces other than {@code excluding}, never {@literal null}, empty if none exist
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

	/**
	 * Moves ownership of every artifact the {@code from} namespace holds under the given {@code groupId} to
	 * the {@code to} namespace, in a single bulk operation. Artifact visibility is untouched.
	 * <p>
	 * Only ever called from {@link #accept(ArtifactOwnershipTransfer)}, after {@link #resolve(ArtifactOwnershipTransfer, TransferState)}
	 * has already confirmed the transfer was in a state that could transition to {@link TransferState#ACCEPTED},
	 * i.e., that both parties have consented.
	 *
	 * @param from the namespace that currently owns the affected artifacts
	 * @param to the namespace that should become the new owner
	 * @param groupId the artifact {@code groupId} coordinate whose artifacts should move
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
