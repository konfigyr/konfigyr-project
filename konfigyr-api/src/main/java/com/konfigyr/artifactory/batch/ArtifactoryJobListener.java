package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.ArtifactoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Consumer;

/**
 * Utility Spring component that listens for various {@link ArtifactoryEvent Artifactor events}
 * that are used as triggers for Spring Batch Jobs.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class ArtifactoryJobListener {

	private final JobRegistry registry;
	private final JobOperator operator;

	@Async
	@TransactionalEventListener(id = "artifactory.release-job-launcher", classes = ArtifactoryEvent.Release.class)
	void released(ArtifactoryEvent.Release event) throws Exception {
		launch(ArtifactoryJobNames.RELEASE_JOB, builder -> builder
				.addString("artifact", event.coordinates().format(), true)
		);
	}

	void launch(String name, Consumer<JobParametersBuilder> customizer) throws Exception {
		final JobParametersBuilder builder = new JobParametersBuilder();
		customizer.accept(builder);

		final JobParameters parameters = builder.toJobParameters();

		log.debug("Attempting to launch job [{}] with parameters: {}", name, parameters);

		final Job job = registry.getJob(name);

		if (job == null) {
			throw new NoSuchJobException("Failed to find job with name: " + name);
		}

		operator.start(job, parameters);
	}

}
