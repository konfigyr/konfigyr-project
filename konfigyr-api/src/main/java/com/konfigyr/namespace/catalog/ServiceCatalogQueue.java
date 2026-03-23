package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jmolecules.ddd.annotation.Repository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceReleaseBuildQueue.SERVICE_RELEASE_BUILD_QUEUE;

/**
 * Component that provides basic behavior of a service catalog build queue backed by a single
 * database table. Tasks are added to the queue from incoming domain events that do not directly
 * trigger catalog rebuilds. Instead, they register intent to rebuild a specific service
 * release in this database-backed queue. Each queue entry represents the latest intent to rebuild a
 * single release, ensuring that multiple events collapse into a single execution.
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
class ServiceCatalogQueue {

	static String PENDING = "PENDING";
	static String RUNNING = "RUNNING";
	static String FAILED = "FAILED";

	private final DSLContext context;
	private final Duration debouncePeriod;
	private final int parallelism;

	/**
	 * Creates a new {@link ServiceCatalogQueue} instance using the given {@link DSLContext}, task
	 * debouncing period and the number of parallel executions.
	 *
	 * @param context the Jooq DSL context to use for database operations, can't be {@literal null}
	 * @param debouncePeriod the debouncing period to use for task execution, can't be {@literal null}
	 * @param parallelism the number of parallel executions to be scheduled; must be greater than zero
	 */
	ServiceCatalogQueue(DSLContext context, Duration debouncePeriod, int parallelism) {
		Assert.isTrue(debouncePeriod.isPositive(), "Debounce period must be positive, got: " + debouncePeriod);
		Assert.isTrue(parallelism > 0, "Parallelism must be greater than zero, got: " + parallelism);

		this.context = context;
		this.debouncePeriod = debouncePeriod;
		this.parallelism = parallelism;
	}

	/**
	 * Consumes the enqueued, or pending, tasks from the queue to execute the catalog rebuild operation.
	 * <p>
	 * This method is typically invoked by a fixed schedule and acts as a supplier of queued releases
	 * to the service catalog build worker. It selects a batch of releases whose scheduled execution
	 * time has been reached, locks them using {@code FOR UPDATE SKIP LOCKED} and updates their state
	 * to {@code RUNNING}. This ensures that each release is processed by at most one worker, even
	 * when multiple instances are running.
	 *
	 * @return the list of entity identifier of releases to be built, can't be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "service-catalog-queue.consume")
	List<EntityId> consume() {
		final OffsetDateTime timestamp = OffsetDateTime.now();

		if (log.isDebugEnabled()) {
			log.debug("Consuming pending service catalog build tasks from the queue with: [parallelism={}, timestamp={}]",
					parallelism, timestamp);
		}

		return context.update(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, RUNNING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STARTED_AT, OffsetDateTime.now())
				.where(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID.in(
						DSL.select(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID)
								.from(SERVICE_RELEASE_BUILD_QUEUE)
								.where(DSL.and(
										SERVICE_RELEASE_BUILD_QUEUE.STATUS.in(PENDING, FAILED),
										SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT.lessOrEqual(timestamp)
								))
								.orderBy(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT)
								.limit(parallelism)
								.forUpdate()
								.skipLocked()
				))
				.returning(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID)
				.fetch(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, EntityId.class);
	}

	/**
	 * Marks the scheduled service catalog build job as complete by either removing it from the queue
	 * or re-scheduling it again as there was an enqueue operation during the build operation.
	 *
	 * @param id the identifier of the service release to complete, can't be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "service-catalog-queue.complete")
	void complete(EntityId id) {
		final Boolean needsRebuild = context.select(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD)
				.from(SERVICE_RELEASE_BUILD_QUEUE)
				.where(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID.eq(id.get()))
				.fetchOne(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD);

		if (Boolean.TRUE.equals(needsRebuild)) {
			log.info("Rescheduling service catalog rebuild for release with identifier: {}", id);

			context.update(SERVICE_RELEASE_BUILD_QUEUE)
					.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, PENDING)
					.set(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD, false)
					.set(SERVICE_RELEASE_BUILD_QUEUE.RETRY_COUNT, SERVICE_RELEASE_BUILD_QUEUE.RETRY_COUNT.plus(1))
					.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now().plus(debouncePeriod))
					.where(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID.eq(id.get()))
					.execute();
		} else {
			log.info("Service catalog has been successfully built for release with identifier: {}", id);

			context.deleteFrom(SERVICE_RELEASE_BUILD_QUEUE)
					.where(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID.eq(id.get()))
					.execute();
		}
	}

	/**
	 * Marks the scheduled service catalog build job as failed by updating its status to {@code FAILED}.
	 * <p>
	 * This method would also increase the retry count by one and re-schedule the build operation using
	 * the specified back-off period.
	 * <p>
	 * If the supplied {@code cause} is not {@literal null}, the affected queue entry would be updated
	 * with the exception stack trace.
	 *
	 * @param id the identifier of the service release to fail, can't be {@literal null}
	 * @param cause the exception that caused the failure, can be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "service-catalog-queue.fail")
	void fail(EntityId id, @Nullable Throwable cause) {
		log.warn("Failed to rebuild service catalog for release with identifier: {}", id, cause);

		context.update(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, FAILED)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RETRY_COUNT, SERVICE_RELEASE_BUILD_QUEUE.RETRY_COUNT.plus(1))
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now().plus(debouncePeriod))
				.set(SERVICE_RELEASE_BUILD_QUEUE.LAST_ERROR, ExceptionUtils.getStackTrace(cause))
				.where(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID.eq(id.get()))
				.execute();
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
	void enqueue(ArtifactoryEvent.ReleaseCompleted event) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to schedule service catalog builds for released artifact: [id={}, coordinates={}]",
					event.id(), event.coordinates());
		}

		final OffsetDateTime timestamp = OffsetDateTime.now();

		final long rows = context.insertInto(SERVICE_RELEASE_BUILD_QUEUE,
						SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID,
						SERVICE_RELEASE_BUILD_QUEUE.STATUS,
						SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT,
						SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT
				)
				.select(
						DSL.selectDistinct(SERVICE_ARTIFACTS.RELEASE_ID, DSL.val("PENDING"), DSL.val(timestamp.plus(debouncePeriod)), DSL.val(timestamp))
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
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, DSL.when(SERVICE_RELEASE_BUILD_QUEUE.STATUS.eq(RUNNING), RUNNING)
						.otherwise(PENDING))
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, timestamp.plus(debouncePeriod))
				.set(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD, true)
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

		final OffsetDateTime timestamp = OffsetDateTime.now();

		long rows = context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, EntityId.from(manifest.id()).get())
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, PENDING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, timestamp.plus(debouncePeriod))
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, timestamp)
				.onDuplicateKeyUpdate()
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, DSL.when(SERVICE_RELEASE_BUILD_QUEUE.STATUS.eq(RUNNING), RUNNING)
						.otherwise(PENDING))
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, timestamp.plus(debouncePeriod))
				.set(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD, true)
				.execute();

		log.info("Scheduled {} service catalog build(s) for published manifest: [id={}, service={}]",
				rows, manifest.id(), event.id());
	}

}
