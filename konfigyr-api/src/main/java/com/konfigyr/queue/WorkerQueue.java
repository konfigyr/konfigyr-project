package com.konfigyr.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;

/**
 * A generic, database-backed work queue abstraction for consuming, executing, and managing scheduled
 * tasks identified by queue name and entity identifier pairs.
 * <p>
 * The {@code BuildQueue} represents a coordination mechanism between producers (enqueue operations)
 * and consumers (workers) in a distributed system. Tasks are persisted in our PostgreSQL database and
 * processed in a safe, concurrent manner using pessimistic locking semantics.
 * <p>
 * This class is following these core principles:
 * <p>
 * <strong>Single-consumer guarantee</strong><br>
 * Tasks must be consumed using a locking strategy such as {@code FOR UPDATE SKIP LOCKED}, ensuring
 * that each queued entity is processed by at most one worker at a time, even when multiple instances
 * of the application are running concurrently.
 * <p>
 * <strong>Scheduled execution</strong><br>
 * Tasks are not necessarily executed immediately upon enqueueing. Instead, they are scheduled for
 * execution at a specific point in time (e.g., via a {@code scheduled_at} column), allowing for
 * debouncing, batching, and controlled retry behavior.
 * <p>
 * <strong>Coalescing / deduplication</strong><br>
 * Multiple enqueue operations for the same scheduled task identity are expected to be coalesced into a
 * single queue entry. If a task is already being processed, subsequent enqueue operations should
 * mark the task for re-execution once the current run completes.
 * <p>
 * <strong>Retry and failure handling</strong><br>
 * Failed tasks should be rescheduled with an appropriate backoff strategy. This class tracks retry
 * counts and stores error information for observability and debugging purposes.
 * <p>
 * <strong>Idempotent processing</strong><br>
 * Consumers must assume that tasks can be retried and therefore should be implemented in an idempotent
 * manner.
 * <p>
 * This class defines the minimal contract required by a worker to consume and process queued
 * tasks. It intentionally does not expose enqueue operations, as those are typically performed
 * directly via SQL or other infrastructure-specific mechanisms.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see QueuedTask
 */
@Slf4j
@RequiredArgsConstructor
class WorkerQueue {

	static final Marker MARKER = MarkerFactory.getMarker("WORKER_QUEUE");

	private final DSLContext context;
	private final QueueRegistrar registrar;

	/**
	 * Consumes pending tasks from the queue and marks them as in-progress.
	 * <p>
	 * This method is typically invoked on a fixed schedule and acts as a supplier of work for
	 * background workers. This is why this method:
	 * <ul>
	 *     <li>Selects tasks whose scheduled execution time has been reached</li>
	 *     <li>Locks them using {@code FOR UPDATE SKIP LOCKED}</li>
	 *     <li>Transitions their state to {@code RUNNING}</li>
	 * </ul>
	 * <p>
	 * The use of row-level locking ensures that multiple workers can safely consume from the same
	 * queue without processing the same task more than once.
	 * <p>
	 * The returned tasks represent tasks that must eventually be completed via
	 * {@link #complete(QueuedTask)} or {@link #fail(QueuedTask, Throwable)}.
	 *
	 * @return the list of queued tasks to be processed, never {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "work-queue.consume")
	List<QueuedTask> consume() {
		final OffsetDateTime timestamp = OffsetDateTime.now();

		if (log.isDebugEnabled()) {
			log.debug(MARKER, "Consuming pending service catalog build tasks from the queue with: [parallelism={}, timestamp={}]",
					20, timestamp);
		}

		return context.update(WORKER_QUEUE)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.STARTED_AT, OffsetDateTime.now())
				.where(WORKER_QUEUE.ID.in(
						DSL.select(WORKER_QUEUE.ID)
								.from(WORKER_QUEUE)
								.where(DSL.and(
										WORKER_QUEUE.STATUS.in(QueuedTaskState.PENDING.name(), QueuedTaskState.FAILED.name()),
										WORKER_QUEUE.SCHEDULED_AT.lessOrEqual(timestamp)
								))
								.orderBy(WORKER_QUEUE.SCHEDULED_AT)
								.limit(20)
								.forUpdate()
								.skipLocked()
				))
				.returning(WORKER_QUEUE.ID, WORKER_QUEUE.QUEUE_NAME, WORKER_QUEUE.ENTITY_ID)
				.fetch(QueuedTask::new);
	}

	/**
	 * Marks the given {@link QueuedTask} as successfully completed.
	 * <p>
	 * Upon completion, implementations should either:
	 * <ul>
	 *     <li>Remove the task from the queue, or</li>
	 *     <li>Re-schedule it for execution if additional enqueue operations occurred while the
	 *     task was being processe, the {@code needs_reschedule} flag is set.</li>
	 * </ul>
	 * <p>
	 * This mechanism ensures that no updates are lost while still maintaining a single active
	 * execution per entity.
	 *
	 * @param task the task to complete, must not be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "work-queue.complete")
	void complete(QueuedTask task) {
		final Boolean reschedule = context.select(WORKER_QUEUE.NEEDS_RESCHEDULE)
				.from(WORKER_QUEUE)
				.where(WORKER_QUEUE.ID.eq(task.id()))
				.fetchOne(WORKER_QUEUE.NEEDS_RESCHEDULE);

		if (Boolean.TRUE.equals(reschedule)) {
			final Duration backoff = registrar.get(task.queueName()).backoff();
			final OffsetDateTime scheduledAt = OffsetDateTime.now().plus(backoff);

			log.info(MARKER, "Rescheduling task: [queue={}, entity={}, at={}]", task.queueName(), task.entityId(), scheduledAt);

			context.update(WORKER_QUEUE)
					.set(WORKER_QUEUE.STATUS, QueuedTaskState.PENDING.name())
					.set(WORKER_QUEUE.NEEDS_RESCHEDULE, false)
					.set(WORKER_QUEUE.RETRY_COUNT, WORKER_QUEUE.RETRY_COUNT.plus(1))
					.set(WORKER_QUEUE.SCHEDULED_AT, scheduledAt)
					.where(WORKER_QUEUE.ID.eq(task.id()))
					.execute();
		} else {
			log.info(MARKER, "Task been successfully executed for queue '{}' and identifier: {}", task.queueName(), task.entityId());

			context.deleteFrom(WORKER_QUEUE)
					.where(WORKER_QUEUE.ID.eq(task.id()))
					.execute();
		}
	}

	/**
	 * Marks the given {@link QueuedTask} as failed.
	 * <p>
	 * This method performs the following:
	 * <ul>
	 *     <li>Increments the retry counter</li>
	 *     <li>Stores diagnostic information about the failure (if provided)</li>
	 *     <li>Re-schedules the task using the supplied backoff period</li>
	 * </ul>
	 * <p>
	 * The task should remain in the queue for future reprocessing attempts unless a terminal
	 * retry limit has been reached.
	 * <p>
	 * The supplied {@code cause} may be {@literal null}. If present, implementations may extract
	 * relevant information such as the exception message or stack trace for persistence.
	 *
	 * @param task the task to mark as failed, must not be {@literal null}
	 * @param cause the exception that caused the failure, may be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "work-queue.fail")
	void fail(QueuedTask task, @Nullable Throwable cause) {
		log.warn(MARKER, "Unexpected error occurred while executing task in queue '{}' and identifier: {}",
				task.queueName(), task.entityId(), cause);

		final Duration backoff = registrar.get(task.queueName()).backoff();

		context.update(WORKER_QUEUE)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.FAILED.name())
				.set(WORKER_QUEUE.RETRY_COUNT, WORKER_QUEUE.RETRY_COUNT.plus(1))
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now().plus(backoff))
				.set(WORKER_QUEUE.LAST_ERROR, ExceptionUtils.getStackTrace(cause))
				.where(WORKER_QUEUE.ID.eq(task.id()))
				.execute();
	}
}
