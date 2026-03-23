package com.konfigyr.namespace.catalog;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates scheduling and execution of service configuration catalog rebuilds by consuming
 * the scheduled tasks from a {@link ServiceCatalogQueue} and executing them using the provided
 * {@link ServiceCatalogWorker}.
 * <p>
 * This scheduler is responsible for handling the outcomes of the submitted rebuild tasks by
 * notifying the queue of their completion. Depending on the result, the service catalog queue
 * would then resolve if the corresponding release has been successfully rebuilt or if the rebuild
 * operation failed and should be retried.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
class ServiceCatalogScheduler {

	private final ServiceCatalogQueue queue;
	private final ServiceCatalogWorker worker;
	private final TaskExecutor executor;
	private final Duration timeout;

	/**
	 * Consumes pending rebuild tasks from the queue and executes catalog rebuilds.
	 * <p>
	 * This method is configured to run on based on a cron expression supplied by the
	 * {@code konfigyr.namespace.service-catalog.scheduler-cron-expression} property.
	 * It consumes a batch of releases from the queue and triggers their rebuild using
	 * the supplied {@link ServiceCatalogWorker}.
	 * <p>
	 * Failures result in the task being rescheduled with backoff, while successful executions
	 * remove the entry from the queue.
	 */
	@Scheduled(cron = "${konfigyr.namespace.service-catalog.scheduler-cron-expression:0 * * * * *}")
	void schedule() {
		final List<EntityId> releases = queue.consume();

		if (CollectionUtils.isEmpty(releases)) {
			return;
		}

		for (EntityId release : releases) {
			CompletableFuture.runAsync(() -> worker.build(release), executor)
					.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
					.thenRun(() -> queue.complete(release))
					.exceptionally(ex -> {
						Throwable cause = ex;

						if (ex instanceof CompletionException && ex.getCause() != null) {
							cause = ex.getCause();
						}

						queue.fail(release, cause);
						return null;
					});
		}
	}

}
