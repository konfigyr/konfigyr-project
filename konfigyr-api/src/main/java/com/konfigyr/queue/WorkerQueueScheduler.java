package com.konfigyr.queue;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates the consumption and execution of scheduled work items from a {@link WorkerQueue}.
 * <p>
 * The {@link WorkerQueueScheduler} acts as the orchestration layer between the queue and the
 * corresponding queue processor. It is responsible for:
 * <p>
 * <strong>Work consumption</strong><br>
 * Periodically polling the queue for eligible tasks and retrieving a batch of entity identifiers
 * that are ready for execution. The underlying queue implementation guarantees that each task is
 * exclusively assigned to a single scheduler instance using database-level locking semantics.
 * <p>
 * <strong>Task execution</strong><br>
 * Delegating the processing of each consumed entity to the responsible {@link QueueProcessor},
 * using the asynchronous {@link org.springframework.core.task.TaskExecutor} that is tailored for
 * the queue. Processor implementations must assume that processing may occur concurrently across
 * multiple threads and instances.
 * <p>
 * <strong>Lifecycle management</strong><br>
 * Reporting the outcome of each execution back to the {@link WorkerQueue} by invoking either
 * {@link WorkerQueue#complete(QueuedTask, Duration)} or
 * {@link WorkerQueue#fail(QueuedTask, Duration, Throwable)}. The queue is responsible for determining
 * whether the task should be removed, retried, or rescheduled based on its internal state
 * (e.g., retry count, reschedule flags).
 * <p>
 * <strong>Timeout handling</strong><br>
 * Implementations may enforce an execution timeout for individual tasks. If a task exceeds
 * the configured timeout, it should be treated as failed and reported back to the queue,
 * allowing it to be retried according to the configured backoff policy.
 * <p>
 * The scheduler itself is stateless and can be safely executed across multiple application
 * instances. Horizontal scalability is achieved through the queue's locking mechanism rather
 * than coordination within the scheduler.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
class WorkerQueueScheduler implements InitializingBean, DisposableBean {

	private final Logger logger;
	private final WorkerQueue workerQueue;
	private final QueueRegistrar queueRegistrar;

	WorkerQueueScheduler(WorkerQueue workerQueue, QueueRegistrar queueRegistrar) {
		this(LoggerFactory.getLogger(WorkerQueueScheduler.class), workerQueue, queueRegistrar);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (QueueConfiguration configuration : queueRegistrar) {
			if (configuration.taskExecutor() instanceof InitializingBean bean) {
				bean.afterPropertiesSet();
			}
		}
	}

	/**
	 * Consumes pending tasks from the queue and executes them via registered {@link QueueProcessor}s.
	 * <p>
	 * Failures result in the task being rescheduled with the backoff, while successful executions
	 * remove the entry from the queue.
	 */
	@Scheduled(cron = "${konfigyr.scheduler.cron-expression:0 * * * * *}")
	void schedule() {
		final List<QueuedTask> tasks = workerQueue.consume();

		if (logger.isTraceEnabled()) {
			logger.trace("Received {} tasks from the queue", tasks.size());
		}

		if (CollectionUtils.isEmpty(tasks)) {
			return;
		}

		for (QueuedTask task : tasks) {
			final QueueConfiguration configuration = queueRegistrar.get(task.queueName());

			if (logger.isTraceEnabled()) {
				logger.trace("Executing {} with configuration: {}", task, configuration);
			}

			final QueueProcessor processor = configuration.queueProcessor();

			CompletableFuture.runAsync(() -> processor.process(task.entityId()), configuration.taskExecutor())
					.orTimeout(configuration.timeout().toMillis(), TimeUnit.MILLISECONDS)
					.thenRun(() -> {
						if (logger.isTraceEnabled()) {
							logger.trace("Marking the task execution as complete for: {}", task);
						}

						workerQueue.complete(task);
					})
					.exceptionally(ex -> {
						if (logger.isTraceEnabled()) {
							logger.trace("Unexpected error occurred while executing task: {}", task, ex);
						}

						Throwable cause = ex;

						if (ex instanceof CompletionException && ex.getCause() != null) {
							cause = ex.getCause();
						}

						workerQueue.fail(task, cause);
						return null;
					});
		}
	}

	@Override
	public void destroy() throws Exception {
		for (QueueConfiguration configuration : queueRegistrar) {
			if (configuration.taskExecutor() instanceof DisposableBean bean) {
				bean.destroy();
			}
			if (configuration.taskExecutor() instanceof AutoCloseable closeable) {
				closeable.close();
			}
		}
	}
}
