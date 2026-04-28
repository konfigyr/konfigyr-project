package com.konfigyr.queue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerQueueSchedulerTest {

	@Mock
	WorkerQueue queue;

	@Mock
	QueueProcessor firstQueueProcessor;

	@Mock
	QueueProcessor secondQueueProcessor;

	ExecutorService executor;

	QueueRegistrar registrar;
	WorkerQueueScheduler scheduler;

	@BeforeEach
	void setup() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(WorkerQueueScheduler.class);
		logger.setLevel(Level.TRACE);

		executor = Executors.newFixedThreadPool(3);
		registrar = QueueRegistrar.of(
				QueueProcessorRegistration.of("first-queue", firstQueueProcessor)
						.backoff(Duration.ofMillis(200))
						.timeout(Duration.ofMillis(400))
						.taskExecutor(new TaskExecutorAdapter(executor)),
				QueueProcessorRegistration.of("second-queue", secondQueueProcessor)
						.backoff(Duration.ofMillis(250))
						.timeout(Duration.ofMillis(400))
		);
		scheduler = new WorkerQueueScheduler(logger, queue, registrar);
		scheduler.afterPropertiesSet();
	}

	@AfterEach
	void cleanup() throws Exception {
		scheduler.destroy();
	}

	@Test
	@DisplayName("should not schedule any builds to the worker when queue is empty")
	void scheduleEmptyQueue() {
		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();
		verifyNoInteractions(firstQueueProcessor);
		verifyNoInteractions(secondQueueProcessor);
	}

	@Test
	@DisplayName("should fail to schedule any tasks for queues that are not registered")
	void scheduleUndeclaredQueue() {
		final var task = new QueuedTask(UUID.randomUUID(), "first-queue", EntityId.from(1));
		final var unknown = new QueuedTask(UUID.randomUUID(), "unknown-queue", EntityId.from(1));

		doReturn(List.of(unknown, task)).when(queue).consume();

		assertThatIllegalStateException()
				.isThrownBy(scheduler::schedule)
				.withMessageContaining("No queue configuration registered for queue %s", unknown.queueName())
				.withNoCause();

		verify(queue).consume();
		verifyNoInteractions(firstQueueProcessor);
		verifyNoInteractions(secondQueueProcessor);
	}

	@Test
	@DisplayName("should schedule single task to the executor and mark it as complete")
	void completeSingleTask() {
		final var task = new QueuedTask(UUID.randomUUID(), "first-queue", EntityId.from(1));

		doReturn(List.of(task)).when(queue).consume();

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(firstQueueProcessor).process(EntityId.from(1));
			verify(queue).complete(task);
		});

		verifyNoMoreInteractions(queue, firstQueueProcessor);
		verifyNoInteractions(secondQueueProcessor);
	}

	@Test
	@DisplayName("should schedule single tasks to the worker and mark it as failed")
	void failSingleTask() {
		final var task = new QueuedTask(UUID.randomUUID(), "second-queue", EntityId.from(1));
		final var cause = new RuntimeException("Failed to execute task");

		doReturn(List.of(task)).when(queue).consume();
		doThrow(cause).when(secondQueueProcessor).process(EntityId.from(1));

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(secondQueueProcessor).process(EntityId.from(1));
			verify(queue).fail(task, cause);
		});

		verifyNoMoreInteractions(queue, secondQueueProcessor);
		verifyNoInteractions(firstQueueProcessor);
	}

	@Test
	@DisplayName("should prevent long processing worker tasks and mark them as failed")
	void timeoutTask() {
		final var task = new QueuedTask(UUID.randomUUID(), "first-queue", EntityId.from(1));

		doReturn(List.of(task)).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(1200, Answers.RETURNS_SMART_NULLS))
				.when(firstQueueProcessor).process(EntityId.from(1));

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(firstQueueProcessor).process(EntityId.from(1));
			verify(queue).fail(eq(task), any(TimeoutException.class));
		});

		verifyNoMoreInteractions(queue, firstQueueProcessor);
		verifyNoInteractions(secondQueueProcessor);
	}

	@Test
	@DisplayName("should schedule multiple tasks to the worker and execute them in parallel")
	void executeTasksInParallel() {
		final var tasks = Stream.concat(
				generateTasks("first-queue", 9),
				generateTasks("second-queue", 7)
		).collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
			Collections.shuffle(list);
			return Collections.unmodifiableList(list);
		}));

		doReturn(tasks).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(100, args -> {
			if (args.getArgument(0, EntityId.class).get() % 2 == 0) {
				throw new RuntimeException("Failed to build service catalog");
			}
			return null;
		})).when(firstQueueProcessor).process(any());

		doAnswer(AdditionalAnswers.answersWithDelay(200, Answers.RETURNS_SMART_NULLS))
				.when(secondQueueProcessor).process(any());

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(firstQueueProcessor, times(9)).process(any());
			verify(secondQueueProcessor, times(7)).process(any());
			verify(queue, times(12)).complete(any());
			verify(queue, times(4)).fail(any(), any(RuntimeException.class));
		});

		verifyNoMoreInteractions(queue, firstQueueProcessor, secondQueueProcessor);
	}

	@Test
	@DisplayName("should rescheduled the task when task executor is abruptly stopped")
	void expectExecutorShutdown() {
		final var task = new QueuedTask(UUID.randomUUID(), "first-queue", EntityId.from(1));

		doReturn(List.of(task)).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(600, Answers.RETURNS_SMART_NULLS))
				.when(firstQueueProcessor).process(any());

		assertThatNoException().isThrownBy(scheduler::schedule);
		assertThatNoException().isThrownBy(executor::shutdownNow);

		await().untilAsserted(() -> {
			verify(firstQueueProcessor).process(task.entityId());
			verify(queue).fail(eq(task), any(InterruptedException.class));
		});

		verifyNoMoreInteractions(queue, firstQueueProcessor, secondQueueProcessor);
	}

	@Test
	@DisplayName("should invoke lifecycle methods for registered queue task executors")
	void invokeLifecycleMethods() throws Exception {
		final var firstTaskExecutor = mock(TaskExecutor.class, withSettings()
				.extraInterfaces(InitializingBean.class, DisposableBean.class));
		final var secondTaskExecutor = mock(TaskExecutor.class, withSettings()
				.extraInterfaces(AutoCloseable.class));
		final var thirdTaskExecutor = mock(TaskExecutor.class);

		scheduler = new WorkerQueueScheduler(queue, QueueRegistrar.of(
				QueueProcessorRegistration.of("first-queue", firstQueueProcessor)
						.taskExecutor(firstTaskExecutor),
				QueueProcessorRegistration.of("second-queue", secondQueueProcessor)
						.taskExecutor(secondTaskExecutor),
				QueueProcessorRegistration.of("third-queue", secondQueueProcessor)
						.taskExecutor(thirdTaskExecutor)
		));

		assertThatNoException()
				.as("Should not throw an exception when invoking lifecycle methods")
				.isThrownBy(scheduler::afterPropertiesSet);

		assertThatNoException()
				.as("Should not throw an exception when invoking lifecycle methods")
				.isThrownBy(scheduler::destroy);

		verify((InitializingBean) firstTaskExecutor).afterPropertiesSet();
		verify((DisposableBean) firstTaskExecutor).destroy();
		verify((AutoCloseable) secondTaskExecutor).close();
		verifyNoInteractions(thirdTaskExecutor);
	}

	static Stream<QueuedTask> generateTasks(String queueName, int count) {
		return IntStream.range(1, count + 1)
				.mapToObj(id -> new QueuedTask(UUID.randomUUID(), queueName, EntityId.from(id)));
	}

}
