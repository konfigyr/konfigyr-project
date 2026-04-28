package com.konfigyr.queue;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;

/**
 * Registration descriptor that binds a logical queue to its processing logic and execution
 * configuration.
 * <p>
 * A {@link QueueProcessorRegistration} is declared as a Spring bean and serves as the integration
 * point between domain-specific work processing and the generic queue infrastructure. It provides
 * the necessary metadata for the system to:
 * <ul>
 *     <li>Identify which queue is being handled (via {@code queueName})</li>
 *     <li>Resolve the {@link QueueProcessor} responsible for executing work items</li>
 *     <li>Configure execution characteristics such as timeout, retry backoff, and concurrency</li>
 * </ul>
 * <p>
 * The {@code queueName} must uniquely identify a queue within the system and must correspond to
 * the name used when enqueueing tasks (e.g. {@code change-request-evaluation}, {@code manifest-builder}).
 * This value acts as the routing key between persisted queue entries and their associated processor.
 * <p>
 * The optional configuration properties allow each queue to be tuned independently:
 * <ul>
 *     <li>
 *         {@code timeout}: maximum duration allowed for processing a single scheduled task before
 *         it is considered failed
 *     </li>
 *     <li>{@code backoff}: duration applied when rescheduling failed tasks</li>
 *     <li>{@code taskExecutor}: dedicated executor used to process tasks for this queue</li>
 * </ul>
 * <p>
 * If any of these values are not explicitly provided, the infrastructure may fall back to
 * sensible defaults.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor(staticName = "of")
public class QueueProcessorRegistration {

	private final String queueName;
	private final QueueProcessor queueProcessor;

	private @Nullable Duration timeout;
	private @Nullable Duration backoff;
	private @Nullable TaskExecutor taskExecutor;

	/**
	 * The maximum duration a task is allowed to run before it is considered timed out.
	 * <p>
	 * The queue scheduler infrastructure may cancel the execution and mark the task
	 * as failed when this timeout is exceeded.
	 *
	 * @param timeout the timeout duration, can't be {@literal null}
	 * @return this registration, never {@literal null}
	 */
	public QueueProcessorRegistration timeout(Duration timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * The backoff duration applied when rescheduling a failed task.
	 * <p>
	 * This value is typically used to delay later execution attempts to avoid rapid
	 * retry loops and reduce the load on the system during failure conditions.
	 *
	 * @param backoff the backoff duration, can't be {@literal null}
	 * @return this registration, never {@literal null}
	 */
	public QueueProcessorRegistration backoff(Duration backoff) {
		this.backoff = backoff;
		return this;
	}

	/**
	 * The {@link TaskExecutor} responsible for executing tasks for this queue.
	 * <p>
	 * It is recommended to provide a dedicated executor per queue to isolate workloads
	 * and allow fine-grained control over concurrency and resource utilization.
	 *
	 * @param taskExecutor the executor to use, can't be {@literal null}
	 * @return this registration, never {@literal null}
	 */
	public QueueProcessorRegistration taskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	String queueName() {
		return queueName;
	}

	QueueConfiguration materialize() {
		if (taskExecutor == null) {
			taskExecutor = new SimpleAsyncTaskExecutorBuilder()
					.threadNamePrefix("WorkerQueue[" + queueName + "]#")
					.rejectTasksWhenLimitReached(false)
					.virtualThreads(true)
					.build();
		}

		return new QueueConfiguration(
				backoff == null ? Duration.ofSeconds(1) : backoff,
				timeout == null ? Duration.ofMillis(2) : timeout,
				taskExecutor,
				queueProcessor
		);
	}
}
