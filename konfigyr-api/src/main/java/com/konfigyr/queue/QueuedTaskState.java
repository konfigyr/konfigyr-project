package com.konfigyr.queue;

/**
 * Represents the lifecycle state of a task stored in the work queue.
 * <p>
 * {@link QueuedTaskState} models the execution progress of a queued task and is used by the
 * queue infrastructure to coordinate scheduling, locking, execution, and retry behavior.
 * Each state reflects a distinct phase in the lifecycle of a task as it moves through the
 * system.
 * <p>
 * The queue implementation relies on these states in combination with database-level locking
 * (e.g. {@code FOR UPDATE SKIP LOCKED}) to ensure that tasks are processed exactly once per
 * execution attempt, even in distributed environments with multiple workers.
 * <p>
 * <strong>State transitions</strong>
 * <pre>
 * PENDING → RUNNING → (COMPLETED | FAILED)
 *                  ↘ retry ↗
 * </pre>
 * <p>
 * Completed tasks are typically removed from the queue or rescheduled depending on queue
 * semantics, while failed tasks may be retried using the configured backoff strategy.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum QueuedTaskState {
	/**
	 * Indicates that the task is scheduled for execution but has not yet been picked up by
	 * the responsible {@link QueueProcessor}.
	 * <p>
	 * Tasks in this state are eligible for consumption once their scheduled execution time
	 * has been reached. During consumption, they are selected and atomically transitioned
	 * to {@link #RUNNING} to prevent concurrent processing by multiple workers.
	 */
	PENDING,

	/**
	 * Indicates that the task is currently being processed by a {@link QueueProcessor}.
	 * <p>
	 * Tasks transition to this state when they are consumed from the queue and locked for
	 * execution. While in this state, the task is considered in-flight and should not be
	 * picked up by other workers.
	 * <p>
	 * If a worker fails or crashes while processing a task, additional mechanisms (such as
	 * timeouts or watchdogs) may be required to detect and recover stuck tasks.
	 */
	RUNNING,

	/**
	 * Indicates that the task execution has failed.
	 * <p>
	 * Tasks transition to this state when an exception occurs during processing. The queue
	 * infrastructure is responsible for recording failure details, incrementing retry counts,
	 * and rescheduling the task using a configured backoff strategy.
	 * <p>
	 * Depending on the retry policy, tasks in this state may either be retried automatically
	 * or require manual intervention after exceeding a maximum retry threshold.
	 */
	FAILED,
}
