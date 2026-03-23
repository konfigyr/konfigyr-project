package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static com.konfigyr.data.tables.ServiceReleaseBuildQueue.SERVICE_RELEASE_BUILD_QUEUE;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ServiceCatalogQueueTest extends AbstractIntegrationTest {

	@Autowired
	DSLContext context;

	@Autowired
	ServiceCatalogQueue queue;

	@AfterEach
	void cleanup() {
		context.truncate(SERVICE_RELEASE_BUILD_QUEUE).execute();
	}

	@Test
	@DisplayName("should enqueue when 'com.konfigyr:konfigyr-crypto-api:1.0.1' artifact is successfully released")
	void scheduleForArtifactRelease() {
		final var event = createReleaseCompletedEvent(3);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(2L, EnqueuedItem::release)
				.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
				.satisfies(item -> assertThat(item.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(1, ChronoUnit.SECONDS))
				)
		);
	}

	@Test
	@DisplayName("should enqueue when 'org.springframework.boot:spring-boot:4.0.4' artifact is successfully released")
	void scheduleMultipleReleasesForArtifactRelease() {
		final var event = createReleaseCompletedEvent(7);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(2)
				.allSatisfy(item -> assertThat(item.status())
						.isEqualTo(ServiceCatalogQueue.PENDING)
				)
				.allSatisfy(item -> assertThat(item.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(1, ChronoUnit.SECONDS))
				)
				.extracting(EnqueuedItem::release)
				.containsExactlyInAnyOrder(1L, 2L)
		);
	}

	@Test
	@DisplayName("should schedule service catalog build task when service manifest was published")
	void scheduleForPublishedManifest() {
		final var event = createManifestPublishedEvent(2);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(2L, EnqueuedItem::release)
				.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
				.satisfies(item -> assertThat(item.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(1, ChronoUnit.SECONDS))
				)
		);
	}

	@Test
	@DisplayName("should update the scheduled build time when build is already enqueued for the same release")
	void enqueueBuildForSameRelease() throws InterruptedException {
		final var timeout = Duration.ofSeconds(2);

		assertThatNoException().isThrownBy(() -> queue.enqueue(createManifestPublishedEvent(2)));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(2L, EnqueuedItem::release)
				.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
				.returns(false, EnqueuedItem::needsRebuild)
				.satisfies(it -> assertThat(it.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
				)
		);

		Thread.sleep(timeout);

		assertThatNoException().isThrownBy(() -> queue.enqueue(createReleaseCompletedEvent(7)));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						it -> assertThat(it)
								.returns(1L, EnqueuedItem::release)
								.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
								.returns(false, EnqueuedItem::needsRebuild)
								.satisfies(item -> assertThat(item.scheduledAt())
										.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
								),
						it -> assertThat(it)
								.returns(2L, EnqueuedItem::release)
								.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
								.returns(true, EnqueuedItem::needsRebuild)
								.satisfies(item -> assertThat(item.scheduledAt())
										.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
								)
				)
		);

		Thread.sleep(timeout.plus(timeout));

		assertThatNoException().isThrownBy(() -> queue.enqueue(createManifestPublishedEvent(2)));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						it -> assertThat(it)
								.returns(1L, EnqueuedItem::release)
								.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
								.returns(false, EnqueuedItem::needsRebuild)
								.satisfies(item -> assertThat(item.scheduledAt())
										.isCloseTo(OffsetDateTime.now().plusSeconds(6), within(timeout))
								),
						it -> assertThat(it)
								.returns(2L, EnqueuedItem::release)
								.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
								.returns(true, EnqueuedItem::needsRebuild)
								.satisfies(item -> assertThat(item.scheduledAt())
										.isCloseTo(OffsetDateTime.now().plusSeconds(8), within(timeout))
								)
				)
		);
	}

	@Test
	@DisplayName("should enqueue the next release build when it is already running")
	void enqueueBuildForRunningTask() {
		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 2L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.RUNNING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThatNoException().isThrownBy(() -> queue.enqueue(createManifestPublishedEvent(2)));

		await().untilAsserted(() -> assertQueueForRelease(2)
				.returns(ServiceCatalogQueue.RUNNING, EnqueuedItem::status)
				.returns(true, EnqueuedItem::needsRebuild)
				.satisfies(item -> assertThat(item.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(2, ChronoUnit.SECONDS))
				)
		);
	}

	@Test
	@DisplayName("should consume the pending item from the queue and mark the scheduled task in progress")
	void consumePendingQueue() {
		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 1L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.PENDING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now().plusDays(1))
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 2L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.PENDING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThat(queue.consume())
				.as("Should contain the only the identifier for: (release_id=2)")
				.containsExactly(EntityId.from(2));

		assertThat(queue.consume())
				.as("Should not consume anything from the queue as it is already consumed")
				.isEmpty();
	}

	@Test
	@DisplayName("should consume the failed item from the queue and mark the scheduled task in progress")
	void consumeFailedQueue() {
		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 1L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.FAILED)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThat(queue.consume())
				.as("Should contain the only the identifier for: (release_id=1)")
				.containsExactly(EntityId.from(1));

		assertThat(queue.consume())
				.as("Should not consume anything from the queue as it is already consumed")
				.isEmpty();
	}

	@Test
	@DisplayName("should fail to schedule service catalog build task for unknown service release identifier")
	void scheduleForUnknownManifest() {
		final var event = createManifestPublishedEvent(9999);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));
		await().untilAsserted(() -> assertQueue().isEmpty());
	}

	@Test
	@DisplayName("should fail to schedule service catalog build task for an unknown released artifact")
	void scheduleForUnknownArtifact() {
		final var event = createReleaseCompletedEvent(9999);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));
		await().untilAsserted(() -> assertQueue().isEmpty());
	}

	@Test
	@Transactional
	@DisplayName("should reschedule the task when completed but requires a rebuild")
	void rescheduleForRebuild() {
		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 1L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.RUNNING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD, true)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThatNoException().isThrownBy(() -> queue.complete(EntityId.from(1)));

		assertQueueForRelease(1)
				.returns(ServiceCatalogQueue.PENDING, EnqueuedItem::status)
				.returns(false, EnqueuedItem::needsRebuild)
				.satisfies(item -> assertThat(item.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(2, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should mark the task complete by removing it from the database table")
	void markCompleted() {
		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 1L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.RUNNING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD, false)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThatNoException().isThrownBy(() -> queue.complete(EntityId.from(1)));

		assertQueue().isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should mark the task as failed and reschedule it for a retry")
	void markFailed() {
		context.insertInto(SERVICE_RELEASE_BUILD_QUEUE)
				.set(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID, 1L)
				.set(SERVICE_RELEASE_BUILD_QUEUE.STATUS, ServiceCatalogQueue.RUNNING)
				.set(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThatNoException().isThrownBy(() -> queue.fail(EntityId.from(1), null));

		assertQueueForRelease(1)
				.returns(ServiceCatalogQueue.FAILED, EnqueuedItem::status)
				.returns(false, EnqueuedItem::needsRebuild)
				.returns(1, EnqueuedItem::retryCount)
				.satisfies(item -> assertThat(item.scheduledAt())
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(2, ChronoUnit.SECONDS))
				);
	}

	ListAssert<EnqueuedItem> assertQueue() {
		return assertQueue(DSL.trueCondition());
	}

	ObjectAssert<EnqueuedItem> assertQueueForRelease(long release) {
		return assertQueue(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID.eq(release))
				.first();
	}

	ListAssert<EnqueuedItem> assertQueue(Condition condition) {
		return assertThat(
				context.select(SERVICE_RELEASE_BUILD_QUEUE.fields())
						.from(SERVICE_RELEASE_BUILD_QUEUE)
						.where(condition)
						.fetch(EnqueuedItem::new)
		);
	}

	static ArtifactoryEvent.ReleaseCompleted createReleaseCompletedEvent(long id) {
		return new ArtifactoryEvent.ReleaseCompleted(EntityId.from(id), mock(ArtifactCoordinates.class));
	}

	static ServiceEvent.Published createManifestPublishedEvent(long id) {
		final var service = mock(Service.class);
		doReturn(EntityId.from(id)).when(service).id();

		final var manifest = mock(Manifest.class);
		doReturn(EntityId.from(id).serialize()).when(manifest).id();

		return new ServiceEvent.Published(service, manifest);
	}

	record EnqueuedItem(long release, String status, int retryCount, boolean needsRebuild, OffsetDateTime scheduledAt, OffsetDateTime createdAt) {

		EnqueuedItem(Record record) {
			this(
					record.get(SERVICE_RELEASE_BUILD_QUEUE.RELEASE_ID),
					record.get(SERVICE_RELEASE_BUILD_QUEUE.STATUS),
					record.get(SERVICE_RELEASE_BUILD_QUEUE.RETRY_COUNT),
					record.get(SERVICE_RELEASE_BUILD_QUEUE.NEEDS_REBUILD),
					record.get(SERVICE_RELEASE_BUILD_QUEUE.SCHEDULED_AT),
					record.get(SERVICE_RELEASE_BUILD_QUEUE.CREATED_AT)
			);
		}
	}

}
