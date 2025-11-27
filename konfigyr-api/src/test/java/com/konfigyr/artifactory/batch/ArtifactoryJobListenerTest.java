package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.entity.EntityId;
import org.apache.commons.lang3.function.Consumers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactoryJobListenerTest {

	@Mock
	JobRegistry registry;

	@Mock
	JobOperator operator;

	@Mock
	Job job;

	ArtifactoryJobListener listener;

	@BeforeEach
	void setup() {
		listener = new ArtifactoryJobListener(registry, operator);
	}

	@Test
	@DisplayName("should launch artifact release batch Job on 'artifactory.artifact-version.release' event")
	void launchReleaseJob() throws Exception {
		final var event = new ArtifactoryEvent.Release(
				EntityId.from(1L),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-licences:1.0.0")
		);

		doReturn(job).when(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);

		assertThatNoException().isThrownBy(() -> listener.released(event));

		verify(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(operator).start(eq(job), assertArg(parameters -> assertThatObject(parameters)
				.returns(event.coordinates().format(), it -> it.getString("artifact"))
		));
	}

	@Test
	@DisplayName("should fail to launch unknown batch Job")
	void launchUnknownJob() {
		doReturn(null).when(registry).getJob("unknown");

		assertThatExceptionOfType(NoSuchJobException.class)
				.isThrownBy(() -> listener.launch("unknown", Consumers.nop()));

		verify(registry).getJob("unknown");
		verifyNoInteractions(operator);
	}

	@Test
	@DisplayName("should fail to launch batch Job that is already running")
	void launchRunningJob() throws Exception {
		doReturn(job).when(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		doThrow(JobExecutionAlreadyRunningException.class).when(operator).start(eq(job), any());

		assertThatExceptionOfType(JobExecutionAlreadyRunningException.class)
				.isThrownBy(() -> listener.launch(ArtifactoryJobNames.RELEASE_JOB, Consumers.nop()));

		verify(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(operator).start(eq(job), any());
	}

	@Test
	@DisplayName("should fail to launch batch Job that is already executed")
	void launchCompletedJob() throws Exception {
		doReturn(job).when(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		doThrow(JobInstanceAlreadyCompleteException.class).when(operator).start(eq(job), any());

		assertThatExceptionOfType(JobInstanceAlreadyCompleteException.class)
				.isThrownBy(() -> listener.launch(ArtifactoryJobNames.RELEASE_JOB, Consumers.nop()));

		verify(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(operator).start(eq(job), any());
	}

	@Test
	@DisplayName("should fail to launch batch Job that contains invalid Job parameters")
	void launchJobWithInvalidParameters() throws Exception {
		doReturn(job).when(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		doThrow(InvalidJobParametersException.class).when(operator).start(eq(job), any());

		assertThatExceptionOfType(InvalidJobParametersException.class)
				.isThrownBy(() -> listener.launch(ArtifactoryJobNames.RELEASE_JOB, Consumers.nop()));

		verify(registry).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(operator).start(eq(job), any());
	}

}
