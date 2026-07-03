package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.queue.QueuedTaskState;
import lombok.extern.slf4j.Slf4j;
import org.jmolecules.ddd.annotation.Repository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.OffsetDateTime;

import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;
import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;

/**
 * Component that enqueues service catalog build tasks to the worker queue backed by a single database
 * table, the {@link com.konfigyr.data.tables.WorkerQueue}. Tasks are added to the queue from incoming
 * domain events that do not directly trigger catalog rebuilds. Instead, they register intent to rebuild
 * a specific service release in this database-backed queue. Each queue entry represents the latest
 * intent to rebuild a single release, ensuring that multiple events collapse into a single execution.
 * <p>
 * Debouncing is achieved by delaying execution using a configurable time window. When multiple events
 * for the same release arrive in quick succession, they update the same queue entry and push its
 * execution slightly into the future. This reduces redundant rebuilds while still ensuring eventual
 * consistency.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
@Repository
class ServiceCatalogQueueListener {

	static final String QUEUE_NAME = "namespace.service-catalog-builder";

	private final DSLContext context;
	private final Duration debouncePeriod;

	/**
	 * Creates a new {@link ServiceCatalogQueueListener} instance using the given {@link DSLContext}, task
	 * debouncing period and the number of parallel executions.
	 *
	 * @param context the Jooq DSL context to use for database operations, can't be {@literal null}
	 * @param debouncePeriod the debouncing period to use for task execution, can't be {@literal null}
	 */
	ServiceCatalogQueueListener(DSLContext context, Duration debouncePeriod) {
		Assert.isTrue(debouncePeriod.isPositive(), "Debounce period must be positive, got: " + debouncePeriod);
		this.context = context;
		this.debouncePeriod = debouncePeriod;
	}

	/**
	 * Enqueues and schedules a catalog rebuild in response to an artifact metadata publication event.
	 * <p>
	 * This method handles events indicating that metadata for an {@link com.konfigyr.artifactory.Artifact}
	 * version has become available and is ready to be used by the service.
	 * <p>
	 * Rebuilds would be triggered only for those service releases whose manifests reference the artifact.
	 * It would enqueue the tasks for those releases using a set-based SQL operation where we rely on the
	 * queue's deduplication to avoid redundant rebuilds.
	 * <p>
	 * This SQL-based approach ensures that no unnecessary rebuilds when metadata was already available
	 * and improves our scalability when artifacts are used by a large number of services.
	 *
	 * @param event the artifact release completion event containing artifact coordinates
	 */
	@Async
	@Transactional(
			propagation = Propagation.REQUIRES_NEW,
			isolation = Isolation.SERIALIZABLE,
			label = "service-catalog-queue.scheduler-for-artifact"
	)
	@TransactionalEventListener(id = "namespace.catalog.build.artifact-released")
	void enqueue(ArtifactoryEvent.PublicationCompleted event) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to schedule service catalog builds for released artifact: [id={}, coordinates={}]",
					event.id(), event.coordinates());
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
										SERVICE_ARTIFACTS.RELEASE_ID,
										DSL.val(QueuedTaskState.PENDING.name()),
										DSL.val(timestamp.plus(debouncePeriod)),
										DSL.val(timestamp)
								)
								.from(SERVICE_ARTIFACTS)
								.innerJoin(ARTIFACTS)
								.on(DSL.and(
										ARTIFACTS.GROUP_ID.eq(SERVICE_ARTIFACTS.GROUP_ID),
										ARTIFACTS.ARTIFACT_ID.eq(SERVICE_ARTIFACTS.ARTIFACT_ID)
								))
								.innerJoin(ARTIFACT_VERSIONS)
								.on(DSL.and(
										ARTIFACT_VERSIONS.ARTIFACT_ID.eq(ARTIFACTS.ID),
										ARTIFACT_VERSIONS.VERSION.eq(SERVICE_ARTIFACTS.VERSION)
								))
								.where(ARTIFACT_VERSIONS.ID.eq(event.id().get()))
				)
				.onDuplicateKeyUpdate()
				.set(WORKER_QUEUE.STATUS, DSL.when(WORKER_QUEUE.STATUS.eq(QueuedTaskState.RUNNING.name()), QueuedTaskState.RUNNING.name())
						.otherwise(QueuedTaskState.PENDING.name()))
				.set(WORKER_QUEUE.SCHEDULED_AT, timestamp.plus(debouncePeriod))
				.set(WORKER_QUEUE.NEEDS_RESCHEDULE, true)
				.execute();

		log.info("Scheduled {} service catalog build(s) for released artifact: [id={}, coordinates={}]",
				rows, event.id(), event.coordinates());
	}

	/**
	 * Enqueues and schedules a catalog rebuild in response to a {@link Manifest} publication event.
	 * <p>
	 * This event represents the creation of a new service release with a defined set of artifact
	 * dependencies. Since the dependency graph has changed, the catalog must always be rebuilt
	 * for the associated service release or {@link Manifest}.
	 * <p>
	 * The release is inserted into the queue with a debounced execution time. If multiple events
	 * occur in quick succession, they are merged into the same queue entry, ensuring that only
	 * one rebuild is eventually performed.
	 * <p>
	 * This guarantees that the catalog remains a deterministic projection of the latest manifest
	 * without producing intermediate or inconsistent states.
	 *
	 * @param event the manifest publication event containing the release identifier
	 */
	@Async
	@Transactional(
			propagation = Propagation.REQUIRES_NEW,
			isolation = Isolation.SERIALIZABLE,
			label = "service-catalog-queue.scheduler-for-manifest"
	)
	@TransactionalEventListener(id = "namespace.catalog.build.manifest-published")
	void enqueue(ServiceEvent.Published event) {
		final Manifest manifest = event.manifest();

		if (log.isDebugEnabled()) {
			log.debug("Attempting to schedule service catalog builds for published manifest: [id={}, service={}]",
					manifest.id(), event.id());
		}

		final EntityId id = EntityId.from(manifest.id());

		// check if the release exists in the database, and if not, simply ignore the event
		if (!context.fetchExists(SERVICE_RELEASES, SERVICE_RELEASES.ID.eq(id.get()))) {
			return;
		}

		final OffsetDateTime timestamp = OffsetDateTime.now();

		long rows = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, QUEUE_NAME)
				.set(WORKER_QUEUE.ENTITY_ID, id.get())
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.PENDING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, timestamp.plus(debouncePeriod))
				.set(WORKER_QUEUE.CREATED_AT, timestamp)
				.onDuplicateKeyUpdate()
				.set(WORKER_QUEUE.STATUS, DSL.when(WORKER_QUEUE.STATUS.eq(QueuedTaskState.RUNNING.name()), QueuedTaskState.RUNNING.name())
						.otherwise(QueuedTaskState.PENDING.name()))
				.set(WORKER_QUEUE.SCHEDULED_AT, timestamp.plus(debouncePeriod))
				.set(WORKER_QUEUE.NEEDS_RESCHEDULE, true)
				.execute();

		log.info("Scheduled {} service catalog build(s) for published manifest: [id={}, service={}]",
				rows, manifest.id(), event.id());
	}

}
