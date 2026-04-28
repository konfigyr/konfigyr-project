package com.konfigyr.queue;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;

/**
 * Functional contract for processing a single work item consumed from a {@link WorkerQueue}.
 * <p>
 * A {@link QueueProcessor} represents the domain-specific execution logic associated with a
 * particular queue. It is invoked by the scheduling infrastructure for each {@link EntityId}
 * returned by {@link WorkerQueue#consume()} and is responsible for performing the actual work
 * associated with that entity.
 * <p>
 * Processors are not discovered directly, but are instead exposed through a {@link QueueProcessorRegistration}
 * which binds:
 * <ul>
 *     <li>The logical queue name (e.g. {@code change-request-evaluation})</li>
 *     <li>The {@link QueueProcessor} implementation</li>
 *     <li>The execution configuration, including the dedicated {@link org.springframework.core.task.TaskExecutor}</li>
 * </ul>
 * <p>
 * This indirection allows the scheduling infrastructure to dynamically route work items to the
 * appropriate processor based on the queue from which they were consumed, while also ensuring that
 * each queue can be executed with its own concurrency and resource constraints.
 * <p>
 * <strong>Execution semantics</strong><br>
 * Implementations are invoked asynchronously and may run concurrently across multiple threads
 * and application instances. As such, processors must be:
 * <ul>
 *     <li><strong>Thread-safe</strong></li>
 *     <li><strong>Idempotent</strong>, as retries may occur</li>
 *     <li><strong>Bounded in execution time</strong>, respecting any externally enforced timeouts</li>
 * </ul>
 * <p>
 * <strong>Error handling</strong><br>
 * Any exception thrown from this method will be interpreted as a processing failure and will result in
 * the associated queue entry being reported back to the worker queue. Successful completion of this
 * method will result in the worker queue being notified to remove this task from the queue.
 * <p>
 * Implementations should avoid performing their own retry logic and instead rely on the queue's retry
 * and backoff mechanisms.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see QueueProcessorRegistration
 */
@NullMarked
@FunctionalInterface
public interface QueueProcessor {

	/**
	 * Processes the work associated with the given {@link EntityId}.
	 * <p>
	 * This method is invoked for each consumed queue entry and represents the unit of work
	 * executed by the background processing infrastructure.
	 * <p>
	 * The supplied {@link EntityId} uniquely identifies the target entity within the context
	 * of the queue. Implementations are expected to resolve any additional data required for
	 * processing based on this identifier.
	 * <p>
	 * Any exception thrown from this method will be treated as a failure and trigger the queue's
	 * retry mechanism.
	 *
	 * @param entityId the identifier of the entity to process, must not be {@literal null}
	 */
	void process(EntityId entityId);

}
