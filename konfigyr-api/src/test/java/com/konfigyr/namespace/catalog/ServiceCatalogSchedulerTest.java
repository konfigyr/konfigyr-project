package com.konfigyr.namespace.catalog;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogSchedulerTest {

	@Mock
	ServiceCatalogQueue queue;

	@Mock
	ServiceCatalogWorker worker;

	ExecutorService executor;
	ServiceCatalogScheduler scheduler;

	@BeforeEach
	void setup() {
		executor = spy(Executors.newFixedThreadPool(3));
		scheduler = new ServiceCatalogScheduler(queue, worker, new TaskExecutorAdapter(executor), Duration.ofMillis(600));
	}

	@AfterEach
	void cleanup() {
		executor.shutdown();
	}

	@Test
	@DisplayName("should not schedule any builds to the worker when queue is empty")
	void scheduleEmptyQueue() {
		assertThatNoException().isThrownBy(scheduler::schedule);

		// shut down the executor to make sure that all tasks are processed
		assertThatNoException().isThrownBy(executor::shutdown);

		verify(queue).consume();
		verifyNoInteractions(worker);
	}

	@Test
	@DisplayName("should schedule single build to the worker and mark it as complete")
	void completeSingleBuild() {
		final var release = EntityId.from(1);

		doReturn(List.of(release)).when(queue).consume();

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(worker).build(release);
			verify(queue).complete(release);
		});

		verifyNoMoreInteractions(queue, worker);
	}

	@Test
	@DisplayName("should schedule single build to the worker and mark it as failed")
	void failSingleBuild() {
		final var release = EntityId.from(1);
		final var cause = new RuntimeException("Failed to build service catalog");

		doReturn(List.of(release)).when(queue).consume();
		doThrow(cause).when(worker).build(release);

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(worker).build(release);
			verify(queue).fail(release, cause);
		});

		verifyNoMoreInteractions(queue, worker);
	}

	@Test
	@DisplayName("should prevent long processing worker tasks and mark them as failed")
	void timeoutBuild() {
		final var release = EntityId.from(1);

		doReturn(List.of(release)).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(1200, Answers.RETURNS_SMART_NULLS)).when(worker).build(any());

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(worker).build(release);
			verify(queue).fail(eq(release), any(TimeoutException.class));
		});

		verifyNoMoreInteractions(queue, worker);
	}

	@Test
	@DisplayName("should schedule multiple builds to the worker and execute them in parallel")
	void executeWorkersInParallel() {
		final var releases = IntStream.range(1, 10).mapToObj(EntityId::from).toList();

		doReturn(releases).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(100, args -> {
			if (args.getArgument(0, EntityId.class).get() % 2 == 0) {
				throw new RuntimeException("Failed to build service catalog");
			}
			return null;
		})).when(worker).build(any());

		assertThatNoException().isThrownBy(scheduler::schedule);

		verify(queue).consume();

		await().untilAsserted(() -> {
			verify(worker, times(9)).build(any());
			verify(queue, times(5)).complete(any());
			verify(queue, times(4)).fail(any(), any(RuntimeException.class));
		});

		verifyNoMoreInteractions(queue, worker);
	}

	@Test
	@DisplayName("should fail to execute the worker task using the supplied executor")
	void expectExecutorFailure() {
		final var release = EntityId.from(1);
		final var cause = new RuntimeException("Failed to submit task to executor");

		doReturn(List.of(release)).when(queue).consume();
		doThrow(cause).when(executor).execute(any());

		assertThatException()
				.isThrownBy(scheduler::schedule)
				.isEqualTo(cause);

		verify(queue).consume();
		verifyNoMoreInteractions(queue, worker);
	}

	@Test
	@DisplayName("should wait for the task be completed when task executor is gracefully shutdown")
	void expectExecutorGracefulShutdown() {
		final var release = EntityId.from(1);

		doReturn(List.of(release)).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(400, Answers.RETURNS_SMART_NULLS)).when(worker).build(any());

		assertThatNoException().isThrownBy(scheduler::schedule);
		assertThatNoException().isThrownBy(executor::shutdown);

		await().untilAsserted(() -> {
			verify(worker).build(release);
			verify(queue).complete(eq(release));
		});

		verifyNoMoreInteractions(queue, worker);
	}

	@Test
	@DisplayName("should rescheduled the build task when task executor is abruptly stopped")
	void expectExecutorShutdown() {
		final var release = EntityId.from(1);

		doReturn(List.of(release)).when(queue).consume();

		doAnswer(AdditionalAnswers.answersWithDelay(600, Answers.RETURNS_SMART_NULLS)).when(worker).build(any());

		assertThatNoException().isThrownBy(scheduler::schedule);
		assertThatNoException().isThrownBy(executor::shutdownNow);

		await().untilAsserted(() -> {
			verify(worker).build(release);
			verify(queue).fail(eq(release), any(InterruptedException.class));
		});

		verifyNoMoreInteractions(queue, worker);
	}

}
