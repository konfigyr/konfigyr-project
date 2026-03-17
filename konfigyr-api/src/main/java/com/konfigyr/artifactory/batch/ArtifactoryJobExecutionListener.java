package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.*;
import com.konfigyr.data.tables.ArtifactVersions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class ArtifactoryJobExecutionListener implements JobExecutionListener {

	private final ApplicationEventPublisher eventPublisher;
	private final Artifactory artifactory;
	private final DSLContext context;

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
	public void afterJob(JobExecution jobExecution) {
		final ReleaseState state;

		final ArtifactCoordinates coordinates = ArtifactCoordinates.parse(
				jobExecution.getJobParameters().getString("artifact")
		);

		log.info("Job execution finished: [artifact={}, status={}, details={}]",
				coordinates.format(), jobExecution.getStatus(), jobExecution.getExitStatus());

		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			state = ReleaseState.RELEASED;
		} else {
			state = ReleaseState.FAILED;
		}

		final VersionedArtifact artifact = artifactory.get(coordinates).orElseThrow(() -> new IllegalStateException(
				String.format("Failed to find artifact with coordinates: %s", coordinates)
		));

		context.update(ArtifactVersions.ARTIFACT_VERSIONS)
				.set(ArtifactVersions.ARTIFACT_VERSIONS.STATE, state.name())
				.where(ArtifactVersions.ARTIFACT_VERSIONS.ID.eq(artifact.id().get()))
				.execute();

		if (state == ReleaseState.RELEASED) {
			eventPublisher.publishEvent(new ArtifactoryEvent.ReleaseCompleted(artifact));
		}

		if (state == ReleaseState.FAILED) {
			eventPublisher.publishEvent(new ArtifactoryEvent.ReleaseFailed(artifact));
		}
	}
}
