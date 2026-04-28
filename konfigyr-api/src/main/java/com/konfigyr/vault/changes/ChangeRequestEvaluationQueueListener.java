package com.konfigyr.vault.changes;

import com.konfigyr.queue.QueuedTaskState;
import com.konfigyr.vault.ChangeRequestEvent;
import com.konfigyr.vault.ChangeRequestState;
import com.konfigyr.vault.VaultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;
import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;

/**
 * Event listener responsible for scheduling change request evaluation tasks in response to
 * domain events.
 * <p>
 * The {@link ChangeRequestEvaluationQueueListener} bridges the event-driven domain layer and the background
 * processing infrastructure by translating relevant events into enqueue operations on the work
 * queue. It ensures that change requests are re-evaluated whenever their state or the state of
 * their target profile changes.
 * <p>
 * This listener does not perform any evaluation itself. Instead, it schedules work by inserting
 * or updating entries in the underlying worker queue using the standardized enqueue semantics
 * (e.g., upsert with coalescing). Actual processing is deferred to the queue scheduler and
 * associated {@link ChangeRequestEvaluator}.
 * <p>
 * <strong>Coalescing behavior</strong><br>
 * Multiple events affecting the same change request may occur in quick succession. The underlying
 * queue mechanism ensures that these are coalesced into a single scheduled task, preventing
 * redundant evaluations while guaranteeing eventual consistency.
 * <p>
 * <strong>Consistency model</strong><br>
 * The listener operates under an eventual consistency model. It guarantees that every relevant
 * change request will be re-evaluated after a triggering event but does not guarantee immediate
 * execution. Scheduling delays and batching are expected and handled by the queue.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class ChangeRequestEvaluationQueueListener {

	static final Duration BACKOFF_PERIOD = Duration.ofSeconds(10);
	static final Duration TIMEOUT_PERIOD = Duration.ofMinutes(1);

	static final String QUEUE_NAME = "vault.change-request-merge-state-evaluation";

	private final DSLContext context;

	/**
	 * Handles events indicating that changes have been applied to a profile and schedules
	 * evaluation tasks for all affected {@link com.konfigyr.vault.ChangeRequest change requests}.
	 * <p>
	 * When a {@link VaultEvent.ChangesApplied} event is received, it implies that the underlying
	 * state of a {@link com.konfigyr.vault.Profile} has changed. This may impact the mergeability
	 * or validity of any open change requests targeting that profile.
	 * <p>
	 * This method would identify all open change requests associated with the affected profile and
	 * enqueue an evaluation task for each affected change request.
	 * <p>
	 * The enqueue operation should rely on the queue's coalescing behavior to avoid redundant
	 * scheduling when multiple changes occur in rapid succession.
	 *
	 * @param event the event representing applied changes to a profile, must not be {@literal null}
	 */
	@Async
	@Transactional(
			propagation = Propagation.REQUIRES_NEW,
			isolation = Isolation.SERIALIZABLE,
			label = "vault.change-request.merge-state-task-scheduler"
	)
	@EventListener(id = "vault.change-request.merge-state-task-scheduler")
	void enqueue(VaultEvent.ChangesApplied event) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to schedule change request merge status evaluation tasks for profile: {}", event.id());
		}

		final OffsetDateTime timestamp = OffsetDateTime.now();

		final long rows = context.insertInto(WORKER_QUEUE,
						WORKER_QUEUE.QUEUE_NAME,
						WORKER_QUEUE.ENTITY_ID,
						WORKER_QUEUE.STATUS,
						WORKER_QUEUE.SCHEDULED_AT,
						WORKER_QUEUE.CREATED_AT
				)
				.select(
						DSL.selectDistinct(
										DSL.val(QUEUE_NAME),
										VAULT_CHANGE_REQUESTS.ID,
										DSL.val(QueuedTaskState.PENDING.name()),
										DSL.val(timestamp.plus(BACKOFF_PERIOD)),
										DSL.val(timestamp)
								)
								.from(VAULT_CHANGE_REQUESTS)
								.where(DSL.and(
										VAULT_CHANGE_REQUESTS.PROFILE_ID.eq(event.id().get()),
										VAULT_CHANGE_REQUESTS.STATE.eq(ChangeRequestState.OPEN.name())
								))
				)
				.onDuplicateKeyUpdate()
				.set(WORKER_QUEUE.STATUS, DSL.when(WORKER_QUEUE.STATUS.eq(QueuedTaskState.RUNNING.name()), QueuedTaskState.RUNNING.name())
						.otherwise(QueuedTaskState.PENDING.name()))
				.set(WORKER_QUEUE.SCHEDULED_AT, timestamp.plus(BACKOFF_PERIOD))
				.set(WORKER_QUEUE.NEEDS_RESCHEDULE, true)
				.execute();

		log.info("Scheduled {} change request merge status evaluation task(s) for profile: {}", rows, event.id());
	}

	/**
	 * Handles change request lifecycle events and schedules an evaluation task for the affected
	 * {@link com.konfigyr.vault.ChangeRequest change request}.
	 * <p>
	 * This method is invoked for events directly associated with a specific change request, such as:
	 * <ul>
	 *     <li>Creation</li>
	 *     <li>Approval or request for changes</li>
	 *     <li>Merge or discard operations</li>
	 * </ul>
	 * <p>
	 * Each of these events may alter the evaluation outcome (e.g., mergeability, approval status),
	 * and therefore requires re-processing by the queue.
	 * <p>
	 * This method should enqueue a task for the specific change request referenced by the event.
	 * The underlying queue will ensure that repeated events are coalesced and processed efficiently.
	 *
	 * @param event the change request event, must not be {@literal null}
	 */
	@Async
	@Transactional(
			propagation = Propagation.REQUIRES_NEW,
			isolation = Isolation.SERIALIZABLE,
			label = "vault.change-request.merge-state-task-scheduler"
	)
	@EventListener(
			id = "vault.change-request.merge-state-task-scheduler",
			classes = {
					ChangeRequestEvent.Opened.class,
					ChangeRequestEvent.Approved.class,
					ChangeRequestEvent.ChangesRequested.class,
					ChangeRequestEvent.Merged.class,
					ChangeRequestEvent.Discarded.class
			}
	)
	void enqueue(ChangeRequestEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to schedule change request merge state evaluation for: {}", event);
		}

		// check if the change request exists in the database, and if not, simply ignore the event
		if (!context.fetchExists(VAULT_CHANGE_REQUESTS, VAULT_CHANGE_REQUESTS.ID.eq(event.id().get()))) {
			return;
		}

		final OffsetDateTime timestamp = OffsetDateTime.now();

		long rows = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, QUEUE_NAME)
				.set(WORKER_QUEUE.ENTITY_ID, event.id().get())
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.PENDING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, timestamp.plus(BACKOFF_PERIOD))
				.set(WORKER_QUEUE.CREATED_AT, timestamp)
				.onDuplicateKeyUpdate()
				.set(WORKER_QUEUE.STATUS, DSL.when(WORKER_QUEUE.STATUS.eq(QueuedTaskState.RUNNING.name()), QueuedTaskState.RUNNING.name())
						.otherwise(QueuedTaskState.PENDING.name()))
				.set(WORKER_QUEUE.SCHEDULED_AT, timestamp.plus(BACKOFF_PERIOD))
				.set(WORKER_QUEUE.NEEDS_RESCHEDULE, true)
				.execute();

		log.info("Scheduled {} change request merge status evaluation task(s) for: {}", rows, event.id());
	}

}
