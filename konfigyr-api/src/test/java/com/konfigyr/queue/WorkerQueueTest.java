package com.konfigyr.queue;

import com.konfigyr.entity.EntityId;
import com.konfigyr.test.AbstractIntegrationTest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.ListAssert;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static com.konfigyr.data.tables.WorkerQueue.WORKER_QUEUE;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WorkerQueueTest extends AbstractIntegrationTest {

	@Mock
	QueueProcessor processor;

	@Autowired
	DSLContext context;

	WorkerQueue queue;

	@BeforeEach
	void setup() {
		queue = new WorkerQueue(context, QueueRegistrar.of(
				QueueProcessorRegistration.of("integration-test-queue", processor)
						.backoff(Duration.ofMinutes(20))
						.timeout(Duration.ofMinutes(1))
		));
	}

	@AfterEach
	void cleanup() {
		context.truncate(WORKER_QUEUE).execute();
	}

	@Test
	@DisplayName("should consume the pending task from the queue and mark the scheduled task in progress")
	void consumePendingQueue() {
		context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 1L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.PENDING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now().plusDays(1))
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 2L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.PENDING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 3L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThat(queue.consume())
				.as("Should contain the only the identifier for: (entity_id=2)")
				.extracting(QueuedTask::queueName, QueuedTask::entityId)
				.containsExactly(tuple("integration-test-queue", EntityId.from(2)));

		assertThat(queue.consume())
				.as("Should not consume anything from the queue as it is already consumed")
				.isEmpty();
	}

	@Test
	@DisplayName("should consume the failed item from the queue and mark the scheduled task in progress")
	void consumeFailedQueue() {
		context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 1L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.FAILED.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.execute();

		assertThat(queue.consume())
				.as("Should contain the only the identifier for: (entity_id=1)")
				.extracting(QueuedTask::queueName, QueuedTask::entityId)
				.containsExactly(tuple("integration-test-queue", EntityId.from(1)));

		assertThat(queue.consume())
				.as("Should not consume anything from the queue as it is already consumed")
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should mark the task complete by removing it from the database table")
	void markCompleted() {
		final var id = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 1L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.NEEDS_RESCHEDULE, false)
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.returning(WORKER_QUEUE.ID)
				.fetchOne(WORKER_QUEUE.ID);

		assertThatNoException().isThrownBy(() -> queue.complete(
				new QueuedTask(id, "integration-test-queue", EntityId.from(1))
		));

		assertQueue()
				.as("Should remove the task from the queue")
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should reschedule the completed task by applying the backoff policy and moving it to pending state")
	void rescheduleCompleted() {
		final var id = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 1L)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.NEEDS_RESCHEDULE, true)
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.returning(WORKER_QUEUE.ID)
				.fetchOne(WORKER_QUEUE.ID);

		assertThatNoException().isThrownBy(() -> queue.complete(
				new QueuedTask(id, "integration-test-queue", EntityId.from(1))
		));

		assertQueue()
				.as("Should reschedule the task with the backoff policy")
				.hasSize(1)
				.first()
				.returns(id, it -> it.get(WORKER_QUEUE.ID))
				.returns(QueuedTaskState.PENDING.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.returns(1, it -> it.get(WORKER_QUEUE.RETRY_COUNT))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusMinutes(20), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should mark the task as failed and reschedule it for a retry")
	void markFailed() {
		final var cause = new RuntimeException("Failed to execute task");
		final var id = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 1L)
				.set(WORKER_QUEUE.RETRY_COUNT, 3)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.returning(WORKER_QUEUE.ID)
				.fetchOne(WORKER_QUEUE.ID);

		assertThatNoException().isThrownBy(() -> queue.fail(
				new QueuedTask(id, "integration-test-queue", EntityId.from(1)),
				cause
		));

		assertQueue()
				.as("Should reschedule the task with the backoff policy")
				.hasSize(1)
				.first()
				.returns(id, it -> it.get(WORKER_QUEUE.ID))
				.returns(ExceptionUtils.getStackTrace(cause), it -> it.get(WORKER_QUEUE.LAST_ERROR))
				.returns(QueuedTaskState.FAILED.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.returns(4, it -> it.get(WORKER_QUEUE.RETRY_COUNT))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusMinutes(20), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should mark the task as failed without known cause and reschedule it for a retry")
	void markFailedWithoutCause() {
		final var id = context.insertInto(WORKER_QUEUE)
				.set(WORKER_QUEUE.QUEUE_NAME, "integration-test-queue")
				.set(WORKER_QUEUE.ENTITY_ID, 1L)
				.set(WORKER_QUEUE.RETRY_COUNT, 3)
				.set(WORKER_QUEUE.STATUS, QueuedTaskState.RUNNING.name())
				.set(WORKER_QUEUE.SCHEDULED_AT, OffsetDateTime.now())
				.set(WORKER_QUEUE.CREATED_AT, OffsetDateTime.now())
				.returning(WORKER_QUEUE.ID)
				.fetchOne(WORKER_QUEUE.ID);

		assertThatNoException().isThrownBy(() -> queue.fail(
				new QueuedTask(id, "integration-test-queue", EntityId.from(1)),
				null
		));

		assertQueue()
				.as("Should reschedule the task with the backoff policy")
				.hasSize(1)
				.first()
				.returns(id, it -> it.get(WORKER_QUEUE.ID))
				.returns("", it -> it.get(WORKER_QUEUE.LAST_ERROR))
				.returns(QueuedTaskState.FAILED.name(), it -> it.get(WORKER_QUEUE.STATUS))
				.returns(4, it -> it.get(WORKER_QUEUE.RETRY_COUNT))
				.satisfies(it -> assertThat(it.get(WORKER_QUEUE.SCHEDULED_AT))
						.isCloseTo(OffsetDateTime.now().plusMinutes(20), within(1, ChronoUnit.SECONDS))
				);
	}

	ListAssert<Record> assertQueue() {
		return assertThat(
				context.select(WORKER_QUEUE.fields())
						.from(WORKER_QUEUE)
						.where(WORKER_QUEUE.QUEUE_NAME.eq("integration-test-queue"))
						.fetch()
		);
	}
}
