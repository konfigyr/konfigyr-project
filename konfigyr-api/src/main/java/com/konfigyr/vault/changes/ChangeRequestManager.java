package com.konfigyr.vault.changes;

import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.vault.*;
import lombok.RequiredArgsConstructor;
import org.jmolecules.ddd.annotation.Repository;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.konfigyr.data.tables.VaultChangeRequestEvents.VAULT_CHANGE_REQUEST_EVENTS;
import static com.konfigyr.data.tables.VaultChangeRequestProperties.VAULT_CHANGE_REQUEST_PROPERTIES;
import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;
import static com.konfigyr.data.tables.VaultProfiles.VAULT_PROFILES;

/**
 * Primary service responsible for user-driven operations on {@link ChangeRequest change requests}.
 * <p>
 * The {@link ChangeRequestManager} exposes the primary interaction surface for querying change
 * requests and performing review-related actions such as approvals, change requests, and comments.
 * It also supports lightweight metadata updates that do not require coordination with external
 * systems.
 * <p>
 * This component operates purely within the application and persistence boundary. It does not perform
 * infrastructure-heavy operations such as merging or discarding change requests, which require
 * coordination with {@link com.konfigyr.vault.state.StateRepository} and locking mechanisms. Those
 * responsibilities are delegated to dedicated services.
 * <p>
 * All mutating operations are expected to produce corresponding domain events, enabling the system to
 * maintain a consistent history and derive review state when required.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@Repository
@RequiredArgsConstructor
public class ChangeRequestManager {

	private static final PageableExecutor pageableExecutor = PageableExecutor.builder()
			.defaultSortField(VAULT_CHANGE_REQUESTS.CREATED_AT.desc())
			.sortField("date", VAULT_CHANGE_REQUESTS.CREATED_AT)
			.sortField("updated", VAULT_CHANGE_REQUESTS.UPDATED_AT)
			.build();

	private final DSLContext context;

	/**
	 * Searches for change requests of a given {@link Service} matching the provided query.
	 *
	 * @param service the service that owns the change requests, can't be {@literal null}
	 * @param query the search criteria, can't be {@literal null}
	 * @return matching change requests, never {@literal null}
	 */
	@Transactional(readOnly = true, label = "vault.change-requests.search")
	public Page<ChangeRequest> search(Service service, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(VAULT_CHANGE_REQUESTS.SERVICE_ID.eq(service.id().get()));

		query.criteria(ChangeRequest.PROFILE_CRITERIA).ifPresent(slug -> conditions.add(
				VAULT_PROFILES.SLUG.eq(slug)
		));

		query.criteria(ChangeRequest.STATE_CRITERIA).ifPresent(state -> conditions.add(
				VAULT_CHANGE_REQUESTS.STATE.eq(state.name())
		));

		query.term().ifPresent(term -> conditions.add(DSL.or(
				VAULT_CHANGE_REQUESTS.SUBJECT.containsIgnoreCase(term),
				VAULT_CHANGE_REQUESTS.DESCRIPTION.containsIgnoreCase(term)
		)));

		return pageableExecutor.execute(
				createChangeRequestQuery(DSL.and(conditions)),
				record -> toChangeRequest(service, record),
				query.pageable(),
				() -> context.fetchCount(createChangeRequestQuery(DSL.and(conditions)))
		);
	}

	/**
	 * Retrieves a change request by {@link Service} and number.
	 * <p>
	 * This method supports human-friendly addressing of change requests, where the number is
	 * unique within a {@link Service} scope.
	 *
	 * @param service the service that owns the change request, can't be {@literal null}
	 * @param number the change request number, can't be {@literal null}
	 * @return the corresponding change request, never {@literal null}
	 */
	@Transactional(readOnly = true, label = "vault.change-requests.retrieve")
	public Optional<ChangeRequest> get(Service service, Long number) {
		return lookupChangeRequest(service, toChangeRequestNumberCondition(service, number));
	}

	/**
	 * Retrieves all property changes, or transitions, that should be performed on the target
	 * {@link Profile} when this change request is approved and merged.
	 *
	 * @param request the request for which transitions are retrieved, can't be {@literal null}
	 * @return the list of property transitions, never {@literal null}
	 */
	@Transactional(readOnly = true, label = "vault.change-requests.retrieve-changes")
	public List<PropertyTransition> changes(ChangeRequest request) {
		return context.select()
				.from(VAULT_CHANGE_REQUEST_PROPERTIES)
				.where(VAULT_CHANGE_REQUEST_PROPERTIES.CHANGE_REQUEST_ID.eq(request.id().get()))
				.orderBy(VAULT_CHANGE_REQUEST_PROPERTIES.PROPERTY_NAME)
				.fetch(ChangeRequestManager::toPropertyTransition);
	}

	/**
	 * Retrieves all event records that occurred during this change request lifecycle.
	 *
	 * @param request the request for which history are retrieved, can't be {@literal null}
	 * @return the list of change request history records, never {@literal null}
	 */
	@Transactional(readOnly = true, label = "vault.change-requests.retrieve-history")
	public List<ChangeRequestHistory> history(ChangeRequest request) {
		return context.select()
				.from(VAULT_CHANGE_REQUEST_EVENTS)
				.where(VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID.eq(request.id().get()))
				.orderBy(VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP.asc())
				.fetch(ChangeRequestManager::toChangeRequestHistory);
	}

	/**
	 * Retrieves the current change request revision snapshot.
	 *
	 * @param request the request for which revision is retrieved, can't be {@literal null}
	 * @return the change request revision snapshot, never {@literal null}
	 */
	@Transactional(readOnly = true, label = "vault.change-requests.retrieve-revision")
	public ChangeRequestRevision revision(ChangeRequest request) {
		final Record snapshot = context.select(
						VAULT_CHANGE_REQUESTS.BASE_REVISION,
						VAULT_CHANGE_REQUESTS.HEAD_REVISION,
						VAULT_CHANGE_REQUESTS.BRANCH_NAME
				)
				.from(VAULT_CHANGE_REQUESTS)
				.where(VAULT_CHANGE_REQUESTS.ID.eq(request.id().get()))
				.fetchOne();

		if (snapshot == null) {
			throw new ChangeRequestNotFoundException(request.id());
		}

		return new ChangeRequestRevision(
				snapshot.get(VAULT_CHANGE_REQUESTS.BASE_REVISION),
				snapshot.get(VAULT_CHANGE_REQUESTS.HEAD_REVISION),
				snapshot.get(VAULT_CHANGE_REQUESTS.BRANCH_NAME),
				changes(request)
		);
	}

	/**
	 * Creates a new change request based on the provided command.
	 * <p>
	 * This method registers a change request that has already been materialized in the underlying
	 * {@link com.konfigyr.vault.state.StateRepository}. It persists the change request metadata,
	 * establishes its initial state, and records a creation event in the history.
	 * <p>
	 * This method performs the following:
	 * <ul>
	 *     <li>Extract base and head revisions from the provided {@link ApplyResult}</li>
	 *     <li>Associate the change request with the given {@code service} and {@code profile}</li>
	 *     <li>Assign a new {@code number} scoped to the target profile</li>
	 *     <li>Persist the change request in its initial state (typically OPEN)</li>
	 *     <li>Record a corresponding creation event for audit and history tracking</li>
	 * </ul>
	 * <p>
	 * This operation does not perform any {@link com.konfigyr.vault.state.StateRepository} actions
	 * itself. It assumes that the branch creation and change application have already been completed
	 * and that the provided data reflects a consistent repository state.
	 *
	 * @param command the command to create the change request, can't be {@literal null}
	 * @return the newly created change request, never {@literal null}
	 */
	@Transactional(label = "vault.change-requests.create")
	public ChangeRequest create(ChangeRequestCreateCommand command) {
		final ApplyResult result = command.result();
		final AuthenticatedPrincipal author = result.author();

		final long number = context.select(DSL.max(VAULT_CHANGE_REQUESTS.NUMBER))
				.from(VAULT_CHANGE_REQUESTS)
				.where(VAULT_CHANGE_REQUESTS.SERVICE_ID.eq(command.service().id().get()))
				.groupBy(VAULT_CHANGE_REQUESTS.SERVICE_ID)
				.fetchOptional(DSL.max(VAULT_CHANGE_REQUESTS.NUMBER))
				.orElse(0L);

		final Long id = context.insertInto(VAULT_CHANGE_REQUESTS)
				.set(
						SettableRecord.of(context, VAULT_CHANGE_REQUESTS)
								.set(VAULT_CHANGE_REQUESTS.ID, EntityId.generate().map(EntityId::get))
								.set(VAULT_CHANGE_REQUESTS.SERVICE_ID, command.service().id().get())
								.set(VAULT_CHANGE_REQUESTS.PROFILE_ID, command.profile().id().get())
								.set(VAULT_CHANGE_REQUESTS.NUMBER, number + 1)
								.set(VAULT_CHANGE_REQUESTS.STATE, ChangeRequestState.OPEN.name())
								.set(VAULT_CHANGE_REQUESTS.MERGE_STATUS, ChangeRequestMergeStatus.NOT_APPROVED.name())
								.set(VAULT_CHANGE_REQUESTS.CHANGE_COUNT, result.changes().size())
								.set(VAULT_CHANGE_REQUESTS.SUBJECT, result.subject())
								.set(VAULT_CHANGE_REQUESTS.DESCRIPTION, result.description())
								.set(VAULT_CHANGE_REQUESTS.BRANCH_NAME, command.branch())
								.set(VAULT_CHANGE_REQUESTS.BASE_REVISION, result.previousRevision())
								.set(VAULT_CHANGE_REQUESTS.HEAD_REVISION, result.revision())
								.set(VAULT_CHANGE_REQUESTS.CREATED_BY, author.getDisplayName().orElseGet(author))
								.set(VAULT_CHANGE_REQUESTS.CREATED_AT, result.timestamp())
								.set(VAULT_CHANGE_REQUESTS.UPDATED_AT, result.timestamp())
								.get()
				)
				.returning(VAULT_CHANGE_REQUESTS.ID)
				.fetchOne(VAULT_CHANGE_REQUESTS.ID);

		Assert.state(id != null, () -> "Failed to create change requests for: " + command);

		InsertValuesStepN<Record> insert = context.insertInto(VAULT_CHANGE_REQUEST_PROPERTIES)
				.columns(List.of(
						VAULT_CHANGE_REQUEST_PROPERTIES.CHANGE_REQUEST_ID,
						VAULT_CHANGE_REQUEST_PROPERTIES.PROPERTY_NAME,
						VAULT_CHANGE_REQUEST_PROPERTIES.CHANGE_OPERATION,
						VAULT_CHANGE_REQUEST_PROPERTIES.NEW_VALUE_CHECKSUM,
						VAULT_CHANGE_REQUEST_PROPERTIES.NEW_VALUE_CIPHER,
						VAULT_CHANGE_REQUEST_PROPERTIES.OLD_VALUE_CHECKSUM,
						VAULT_CHANGE_REQUEST_PROPERTIES.OLD_VALUE_CIPHER
				));

		for (PropertyTransition transition : result) {
			final PropertyValue from = transition.from();
			final PropertyValue to = transition.to();

			insert = insert.values(
					id,
					transition.name(),
					transition.type().name(),
					to != null ? to.checksum() : null,
					to != null ? to.get() : null,
					from != null ? from.checksum() : null,
					from != null ? from.get() : null
			);
		}

		insert.execute();

		context.insertInto(VAULT_CHANGE_REQUEST_EVENTS)
				.set(VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID, id)
				.set(VAULT_CHANGE_REQUEST_EVENTS.TYPE, ChangeRequestHistory.Type.CREATED.name())
				.set(VAULT_CHANGE_REQUEST_EVENTS.INITIATOR, author.getDisplayName().orElseGet(author))
				.set(VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP, result.timestamp())
				.execute();

		return new ChangeRequest(
				EntityId.from(id),
				command.service(),
				command.profile(),
				number + 1,
				ChangeRequestState.OPEN,
				ChangeRequestMergeStatus.NOT_APPROVED,
				result.subject(),
				result.description(),
				result.changes().size(),
				author.getDisplayName().orElseGet(author),
				result.timestamp(),
				result.timestamp()
		);
	}

	/**
	 * Updates the subject and/or description of a change request.
	 * <p>
	 * This operation is considered a metadata update and does not affect the review state. History
	 * event may be created in case the subject of the change request is changed.
	 * <p>
	 * The returned {@link ChangeRequest} reflects the latest persisted state after the command has
	 * been applied.
	 *
	 * @param command the update command, can't be {@literal null}
	 * @return the updated change request, never {@literal null}
	 * @throws ChangeRequestNotFoundException if the change request does not exist
	 */
	public ChangeRequest update(ChangeRequestUpdateCommand command) {
		final Record record = createChangeRequestQuery(
				toChangeRequestNumberCondition(command.service(), command.number())
		).fetchOptional().orElseThrow(() -> new ChangeRequestNotFoundException(command.service(), command.number()));

		final Long id = record.get(VAULT_CHANGE_REQUESTS.ID);
		boolean transitioned = false;
		boolean renamed = false;

		if (command.state() != null) {
			final ChangeRequestState state = record.get(VAULT_CHANGE_REQUESTS.STATE, ChangeRequestState.class);

			if (state == ChangeRequestState.OPEN && command.state() != ChangeRequestState.OPEN) {
				transitioned = true;
				record.set(VAULT_CHANGE_REQUESTS.STATE, command.state().name());
			}
		}

		if (command.subject() != null) {
			renamed = !command.subject().equals(record.get(VAULT_CHANGE_REQUESTS.SUBJECT));
			record.set(VAULT_CHANGE_REQUESTS.SUBJECT, command.subject());
		}

		if (command.description() != null) {
			record.set(VAULT_CHANGE_REQUESTS.DESCRIPTION, command.description());
		}

		if (record.changed()) {
			record.set(VAULT_CHANGE_REQUESTS.UPDATED_AT, OffsetDateTime.now());
		}

		context.update(VAULT_CHANGE_REQUESTS)
				.set(record)
				.where(VAULT_CHANGE_REQUESTS.ID.eq(id))
				.execute();

		if (renamed || transitioned) {
			final AuthenticatedPrincipal initiator = command.principal();
			final Stream.Builder<ChangeRequestHistory.Type> events = Stream.builder();

			if (renamed) {
				events.add(ChangeRequestHistory.Type.RENAMED);
			}

			if (transitioned) {
				events.add(switch (command.state()) {
					case MERGED -> ChangeRequestHistory.Type.MERGED;
					case DISCARDED ->  ChangeRequestHistory.Type.DISCARDED;
					default -> throw new IllegalStateException(
							"Change request state transition should only occur when change request is merged or discarded"
					);
				});
			}

			context.insertInto(VAULT_CHANGE_REQUEST_EVENTS, List.of(
							VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID,
							VAULT_CHANGE_REQUEST_EVENTS.TYPE,
							VAULT_CHANGE_REQUEST_EVENTS.INITIATOR,
							VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP
					))
					.valuesOfRecords(events.build().map(type -> {
						final var insert = context.newRecord(
								VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID,
								VAULT_CHANGE_REQUEST_EVENTS.TYPE,
								VAULT_CHANGE_REQUEST_EVENTS.INITIATOR,
								VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP
						);
						insert.set(VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID, id);
						insert.set(VAULT_CHANGE_REQUEST_EVENTS.TYPE, type.name());
						insert.set(VAULT_CHANGE_REQUEST_EVENTS.INITIATOR, initiator.getDisplayName().orElseGet(initiator));
						insert.set(VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP, record.get(VAULT_CHANGE_REQUESTS.UPDATED_AT));
						return insert;
					}).toList())
					.execute();
		}

		return toChangeRequest(command.service(), record);
	}

	/**
	 * Applies a review operation to a change request.
	 * <p>
	 * This method processes the provided {@link ChangeRequestReviewCommand} and records the
	 * corresponding review action (approval, change request, or comment) against the targeted
	 * change request.
	 * <p>
	 * The operation is resolved based on the {@code service} and {@code number} contained within
	 * the command, and executed in the context of the provided {@code principal}.
	 * <p>
	 * This method would try to:
	 * <ul>
	 *     <li>Validate the existence and current state of the change request</li>
	 *     <li>Persist a corresponding review event for audit and history tracking</li>
	 *     <li>Update any derived review state if maintained eagerly</li>
	 * </ul>
	 * <p>
	 * The returned {@link ChangeRequest} reflects the latest persisted state after the operation
	 * defined by the command has been applied.
	 *
	 * @param command the review command describing the operation to perform, can't be {@literal null}
	 * @return the updated change request, never {@literal null}
	 * @throws ChangeRequestNotFoundException if the change request does not exist
	 */
	@Transactional(label = "vault.change-requests.submit-review")
	public ChangeRequest review(ChangeRequestReviewCommand command) {
		final AuthenticatedPrincipal initiator = command.principal();

		final ChangeRequest request = lookupChangeRequest(
				command.service(),
				toChangeRequestNumberCondition(command.service(), command.number())
		).orElseThrow(() -> new ChangeRequestNotFoundException(command.service(), command.number()));

		context.insertInto(VAULT_CHANGE_REQUEST_EVENTS)
					.set(VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID, request.id().get())
					.set(VAULT_CHANGE_REQUEST_EVENTS.TYPE, command.type().name())
					.set(VAULT_CHANGE_REQUEST_EVENTS.INITIATOR, initiator.getDisplayName().orElseGet(initiator))
					.set(VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP, OffsetDateTime.now())
					.execute();

		return request;
	}

	private SelectConditionStep<? extends Record> createChangeRequestQuery(Condition condition) {
		return context.select(
						VAULT_PROFILES.ID,
						VAULT_PROFILES.SERVICE_ID,
						VAULT_PROFILES.SLUG,
						VAULT_PROFILES.NAME,
						VAULT_PROFILES.DESCRIPTION,
						VAULT_PROFILES.POLICY,
						VAULT_PROFILES.POSITION,
						VAULT_PROFILES.CREATED_AT,
						VAULT_PROFILES.UPDATED_AT,
						VAULT_CHANGE_REQUESTS.ID,
						VAULT_CHANGE_REQUESTS.NUMBER,
						VAULT_CHANGE_REQUESTS.STATE,
						VAULT_CHANGE_REQUESTS.MERGE_STATUS,
						VAULT_CHANGE_REQUESTS.CHANGE_COUNT,
						VAULT_CHANGE_REQUESTS.SUBJECT,
						VAULT_CHANGE_REQUESTS.DESCRIPTION,
						VAULT_CHANGE_REQUESTS.CREATED_BY,
						VAULT_CHANGE_REQUESTS.CREATED_AT,
						VAULT_CHANGE_REQUESTS.UPDATED_AT
				)
				.from(VAULT_CHANGE_REQUESTS)
				.innerJoin(VAULT_PROFILES)
				.on(VAULT_PROFILES.ID.eq(VAULT_CHANGE_REQUESTS.PROFILE_ID))
				.where(condition);
	}

	private Optional<ChangeRequest> lookupChangeRequest(Service service, Condition condition) {
		return createChangeRequestQuery(condition)
				.fetchOptional(record -> toChangeRequest(service, record));
	}

	private static Condition toChangeRequestNumberCondition(Service service, Long number) {
		return DSL.and(
				VAULT_CHANGE_REQUESTS.SERVICE_ID.eq(service.id().get()),
				VAULT_CHANGE_REQUESTS.NUMBER.eq(number)
		);
	}

	private static ChangeRequest toChangeRequest(Service service, Record record) {
		return new ChangeRequest(
				record.get(VAULT_CHANGE_REQUESTS.ID, EntityId.class),
				service,
				toProfile(record),
				record.get(VAULT_CHANGE_REQUESTS.NUMBER),
				record.get(VAULT_CHANGE_REQUESTS.STATE, ChangeRequestState.class),
				record.get(VAULT_CHANGE_REQUESTS.MERGE_STATUS, ChangeRequestMergeStatus.class),
				record.get(VAULT_CHANGE_REQUESTS.SUBJECT),
				record.get(VAULT_CHANGE_REQUESTS.DESCRIPTION),
				record.get(VAULT_CHANGE_REQUESTS.CHANGE_COUNT),
				record.get(VAULT_CHANGE_REQUESTS.CREATED_BY),
				record.get(VAULT_CHANGE_REQUESTS.CREATED_AT),
				record.get(VAULT_CHANGE_REQUESTS.UPDATED_AT)
		);
	}

	private static Profile toProfile(Record record) {
		return Profile.builder()
				.id(record.get(VAULT_PROFILES.ID, EntityId.class))
				.service(record.get(VAULT_PROFILES.SERVICE_ID, EntityId.class))
				.slug(record.get(VAULT_PROFILES.SLUG))
				.name(record.get(VAULT_PROFILES.NAME))
				.description(record.get(VAULT_PROFILES.DESCRIPTION))
				.policy(record.get(VAULT_PROFILES.POLICY, ProfilePolicy.class))
				.position(record.get(VAULT_PROFILES.POSITION))
				.createdAt(record.get(VAULT_PROFILES.CREATED_AT))
				.updatedAt(record.get(VAULT_PROFILES.UPDATED_AT))
				.build();
	}

	private static ChangeRequestHistory toChangeRequestHistory(Record record) {
		return new ChangeRequestHistory(
				record.get(VAULT_CHANGE_REQUEST_EVENTS.ID, String.class),
				record.get(VAULT_CHANGE_REQUEST_EVENTS.TYPE, ChangeRequestHistory.Type.class),
				null,
				record.get(VAULT_CHANGE_REQUEST_EVENTS.INITIATOR),
				record.get(VAULT_CHANGE_REQUEST_EVENTS.TIMESTAMP)
		);
	}

	private static PropertyTransition toPropertyTransition(Record record) {
		return new PropertyTransition(
				record.get(VAULT_CHANGE_REQUEST_PROPERTIES.PROPERTY_NAME),
				record.get(VAULT_CHANGE_REQUEST_PROPERTIES.CHANGE_OPERATION, PropertyTransitionType.class),
				toPropertyValue(
						record.get(VAULT_CHANGE_REQUEST_PROPERTIES.OLD_VALUE_CHECKSUM),
						record.get(VAULT_CHANGE_REQUEST_PROPERTIES.OLD_VALUE_CIPHER)
				),
				toPropertyValue(
						record.get(VAULT_CHANGE_REQUEST_PROPERTIES.NEW_VALUE_CHECKSUM),
						record.get(VAULT_CHANGE_REQUEST_PROPERTIES.NEW_VALUE_CIPHER)
				)
		);
	}

	@Nullable
	private static PropertyValue toPropertyValue(@Nullable ByteArray checksum, @Nullable ByteArray cipher) {
		if (checksum == null || cipher == null) {
			return null;
		}
		return PropertyValue.sealed(cipher, checksum);
	}

}
