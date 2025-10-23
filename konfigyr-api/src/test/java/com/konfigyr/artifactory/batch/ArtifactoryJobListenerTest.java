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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactoryJobListenerTest {

	@Mock
	JobLocator locator;

	@Mock
	JobLauncher launcher;

	@Mock
	Job job;

	ArtifactoryJobListener listener;

	@BeforeEach
	void setup() {
		listener = new ArtifactoryJobListener(locator, launcher);
	}

	@Test
	@DisplayName("should launch artifact release batch Job on 'artifactory.artifact-version.release' event")
	void launchReleaseJob() throws Exception {
		final var event = new ArtifactoryEvent.Release(
				EntityId.from(1L),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-licences:1.0.0")
		);

		doReturn(job).when(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);

		assertThatNoException().isThrownBy(() -> listener.released(event));

		verify(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(launcher).run(eq(job), assertArg(parameters -> assertThat(parameters)
				.returns(event.coordinates().format(), it -> it.getString("artifact"))
		));
	}

	@Test
	@DisplayName("should fail to launch unknown batch Job")
	void launchUnknownJob() throws Exception {
		doThrow(NoSuchJobException.class).when(locator).getJob("unknown");

		assertThatExceptionOfType(NoSuchJobException.class)
				.isThrownBy(() -> listener.launch("unknown", Consumers.nop()));

		verify(locator).getJob("unknown");
		verifyNoInteractions(launcher);
	}

	@Test
	@DisplayName("should fail to launch batch Job that is already running")
	void launchRunningJob() throws Exception {
		doReturn(job).when(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		doThrow(JobExecutionAlreadyRunningException.class).when(launcher).run(eq(job), any());

		assertThatExceptionOfType(JobExecutionAlreadyRunningException.class)
				.isThrownBy(() -> listener.launch(ArtifactoryJobNames.RELEASE_JOB, Consumers.nop()));

		verify(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(launcher).run(eq(job), any());
	}

	@Test
	@DisplayName("should fail to launch batch Job that is already executed")
	void launchCompletedJob() throws Exception {
		doReturn(job).when(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		doThrow(JobInstanceAlreadyCompleteException.class).when(launcher).run(eq(job), any());

		assertThatExceptionOfType(JobInstanceAlreadyCompleteException.class)
				.isThrownBy(() -> listener.launch(ArtifactoryJobNames.RELEASE_JOB, Consumers.nop()));

		verify(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(launcher).run(eq(job), any());
	}

	@Test
	@DisplayName("should fail to launch batch Job that contains invalid Job parameters")
	void launchJobWithInvalidParameters() throws Exception {
		doReturn(job).when(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		doThrow(JobParametersInvalidException.class).when(launcher).run(eq(job), any());

		assertThatExceptionOfType(JobParametersInvalidException.class)
				.isThrownBy(() -> listener.launch(ArtifactoryJobNames.RELEASE_JOB, Consumers.nop()));

		verify(locator).getJob(ArtifactoryJobNames.RELEASE_JOB);
		verify(launcher).run(eq(job), any());
	}

}
