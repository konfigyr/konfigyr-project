package com.konfigyr.vault.changes;

import com.konfigyr.entity.EntityId;
import com.konfigyr.queue.QueuedTaskState;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.vault.ApplyResult;
import com.konfigyr.vault.ChangeRequestEvent;
import com.konfigyr.vault.VaultEvent;
import org.assertj.core.api.ListAssert;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ChangeRequestEvaluationQueueListenerTest extends AbstractIntegrationTest {

	@Autowired
	DSLContext context;

	@Autowired
	ChangeRequestEvaluationQueueListener queue;

	@AfterEach
	void cleanup() {
		context.truncate(WORKER_QUEUE).execute();
	}

	@ValueSource(classes = {
			ChangeRequestEvent.Opened.class,
			ChangeRequestEvent.Merged.class,
			ChangeRequestEvent.Approved.class,
			ChangeRequestEvent.ChangesRequested.class,
			ChangeRequestEvent.Discarded.class
	})
	@ParameterizedTest(name = "should enqueue for event: {0}")
	@DisplayName("should enqueue change request when change request domain event was published")
	void scheduleForChangeRequestOpenedEvent(Class<? extends ChangeRequestEvent> eventType) {
		final var event = mock(eventType);
		doReturn(EntityId.from(2)).when(event).id();

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(1)
				.first()
				.returns(event.id().get(), it -> it.get(WORKER_QUEUE.ENTITY_ID))
				.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(
								OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
								within(1, ChronoUnit.SECONDS)
						)
				)
		);
	}

	@Test
	@DisplayName("should enqueue the next change request evaluation task when it is already running")
	void enqueueForRunningTask() {
		final var id = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, ChangeRequestEvaluationQueueListener.QUEUE_NAME)
				.set(WORKER_QUEUE.ENTITY_ID, 2L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.returning(WORKER_QUEUE.ID)
				.fetchOne(WORKER_QUEUE.ID);

		assertThatNoException()
				.as("should successfully enqueue the change request evaluation task")
				.isThrownBy(() -> queue.enqueue(new ChangeRequestEvent.Opened(EntityId.from(2))));

		await().untilAsserted(() -> assertQueue(WORKER_QUEUE.ID.eq(id))
				.first()
				.returns(QueuedTaskState.RUNNING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(
								OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
								within(1, ChronoUnit.SECONDS)
						)
				)
		);
	}

	@Test
	@DisplayName("should enqueue open change requests that were affected by the applied changes on the profile")
	void scheduleForChangesAppliedEvent() {
		final var event = new VaultEvent.ChangesApplied(EntityId.from(4), mock(ApplyResult.class));

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));

		await().untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						record -> assertThat(record)
								.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								),
						record -> assertThat(record)
								.returns(3L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								)
				)
		);
	}

	@Test
	@DisplayName("should update the scheduled build time when build is already enqueued for the same release")
	void scheduleTasksWith() throws InterruptedException {
		final var timeout = Duration.ofSeconds(2);

		assertThatNoException().isThrownBy(() -> queue.enqueue(new VaultEvent.ChangesApplied(EntityId.from(4), mock(ApplyResult.class))));
		await().untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						record -> assertThat(record)
								.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								),
						record -> assertThat(record)
								.returns(3L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								)
				)
		);

		Thread.sleep(timeout);

		assertThatNoException().isThrownBy(() -> queue.enqueue(new ChangeRequestEvent.Opened(EntityId.from(3))));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						record -> assertThat(record)
								.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(false, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now()
														.plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD)
														.minus(timeout),
												within(1, ChronoUnit.SECONDS)
										)
								),
						record -> assertThat(record)
								.returns(3L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								)
				)
		);

		Thread.sleep(timeout.plus(timeout));

		assertThatNoException().isThrownBy(() -> queue.enqueue(new VaultEvent.ChangesApplied(EntityId.from(4), mock(ApplyResult.class))));
		await().atMost(timeout).untilAsserted(() -> assertQueue()
				.hasSize(2)
				.satisfiesExactlyInAnyOrder(
						record -> assertThat(record)
								.returns(2L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								),
						record -> assertThat(record)
								.returns(3L, it -> it.get(WORKER_QUEUE.ENTITY_ID))
								.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
								.returns(true, it -> it.get(WORKER_QUEUE.NEEDS_RESCHEDULE))
								.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
										.isCloseTo(
												OffsetDateTime.now().plus(ChangeRequestEvaluationQueueListener.BACKOFF_PERIOD),
												within(1, ChronoUnit.SECONDS)
										)
								)
				)
		);
	}

	@Test
	@DisplayName("should fail to schedule change request evaluation tasks for an unknown change request")
	void scheduleForUnknownChangeRequest() {
		final var event = new ChangeRequestEvent.Opened(EntityId.from(9999));

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));
		await().untilAsserted(() -> assertQueue().isEmpty());
	}

	@Test
	@DisplayName("should fail to schedule change request evaluation tasks for an unknown profile")
	void scheduleForUnknownProfile() {
		final var event = new VaultEvent.ChangesApplied(EntityId.from(9999), mock(ApplyResult.class));

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));
		await().untilAsserted(() -> assertQueue().isEmpty());
	}

	@Test
	@DisplayName("should not schedule change request evaluation tasks for a profile when none are open")
	void scheduleForProfileWithoutOpenRequests() {
		final var event = new VaultEvent.ChangesApplied(EntityId.from(5), mock(ApplyResult.class));

		assertThatNoException().isThrownBy(() -> queue.enqueue(event));
		await().untilAsserted(() -> assertQueue().isEmpty());
	}

	ListAssert<org.jooq.Record> assertQueue() {
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

	static ConditionFactory await() {
		return Awaitility.await()
				// should be able to process everything in two seconds
				.atMost(2, TimeUnit.SECONDS)
				// use the initial delay of 200ms, this should be enough to execute DB operations
				.pollDelay(200, TimeUnit.MILLISECONDS)
				// poll for every 100ms
				.pollInterval(100, TimeUnit.MILLISECONDS);
	}

}
