package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.queue.QueuedTaskState;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.ListAssert;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ServiceCatalogQueueListenerTest extends AbstractIntegrationTest {

	@Autowired
	DSLContext context;

	@Autowired
	ServiceCatalogQueueListener queue;

	@AfterEach
	void cleanup() {
		context.truncate(WORKER_QUEUE).execute();
	}

	@Test
	@DisplayName("should enqueue when 'com.konfigyr:konfigyr-crypto-api:1.0.1' artifact is successfully released")
	void scheduleForArtifactRelease() {
		final var event = createReleaseCompletedEvent(3);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
				.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
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
				.allSatisfy(it -> assertThat(it.get(WORKER_QUEUE.STATUS))
						.isEqualTo(QueuedTaskState.PENDING.name())
				)
				.allSatisfy(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(1, ChronoUnit.SECONDS))
				)
				.extracting(it -> it.get(WORKER_QUEUE.ENTITY_ID))
				.containsExactlyInAnyOrder(1L, 2L)
		);
	}

	@Test
	@DisplayName("should schedule service catalog build task when service manifest was released")
	void scheduleForReleasedManifest() {
		final var event = createManifestReleasedEvent(2);

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
				.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(1, ChronoUnit.SECONDS))
				)
		);
	}

	@Test
	@DisplayName("should update the scheduled build time when build is already enqueued for the same release")
	void enqueueBuildForSameRelease() throws InterruptedException {
		final var timeout = Duration.ofSeconds(2);

		assertThatNoException().isThrownBy(() -> queue.enqueue(createManifestReleasedEvent(2)));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
				.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.returns(false, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
				)
		);

		Thread.sleep(timeout);

		assertThatNoException().isThrownBy(() -> queue.enqueue(createReleaseCompletedEvent(7)));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						record -> assertThat(record)
								.returns(1L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(false, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
								),
						record -> assertThat(record)
								.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
								)
				)
		);

		Thread.sleep(timeout.plus(timeout));

		assertThatNoException().isThrownBy(() -> queue.enqueue(createManifestReleasedEvent(2)));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						record -> assertThat(record)
								.returns(1L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(false, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(OffsetDateTime.now().plusSeconds(6), within(timeout))
								),
						record -> assertThat(record)
								.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(timeout))
								)
				)
		);
	}

	@Test
	@DisplayName("should enqueue the next release build when it is already running")
	void enqueueBuildForRunningTask() {
		final var id = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, ServiceCatalogQueueListener.QUEUE_NAME)
				.set(WORKER_QUEUE.ENTITY_ID, 2L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.returning(WORKER_QUEUE.ID)
				.fetchOne(WORKER_QUEUE.ID);

		assertThatNoException().isThrownBy(() -> queue.enqueue(createManifestReleasedEvent(2)));

		await().untilAsserted(() -> assertQueue(WORKER_QUEUE.ID.eq(id))
				.first()
				.returns(QueuedTaskState.RUNNING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusSeconds(10), within(2, ChronoUnit.SECONDS))
				)
		);
	}

	@Test
	@DisplayName("should fail to schedule service catalog build task for unknown service release identifier")
	void scheduleForUnknownManifest() {
		final var event = createManifestReleasedEvent(9999);

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

	ListAssert<Record> assertQueue() {
		return assertQueue(DSL.trueCondition());
	}

	ListAssert<Record> assertQueue(Condition condition) {
		return assertThat(
				context.select(WORKER_QUEUE.fields())
						.from(WORKER_QUEUE)
						.where(condition)
						.fetch()
		);
	}

	static ArtifactoryEvent.PublicationCompleted createReleaseCompletedEvent(long id) {
		return new ArtifactoryEvent.PublicationCompleted(EntityId.from(id), mock(Owner.class), mock(ArtifactCoordinates.class));
	}

	static ServiceEvent.Released createManifestReleasedEvent(long id) {
		final var service = mock(Service.class);
		doReturn(EntityId.from(id)).when(service).id();

		final var manifest = mock(Manifest.class);
		doReturn(EntityId.from(id).serialize()).when(manifest).id();

		return new ServiceEvent.Released(service, manifest);
	}

}
