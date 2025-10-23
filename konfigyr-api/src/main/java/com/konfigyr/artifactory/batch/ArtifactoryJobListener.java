package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.ArtifactoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.lang.NonNull;
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
@RequiredArgsConstructor
class ArtifactoryJobListener {

	private final JobLocator locator;
	private final JobLauncher launcher;

	@Async
	@TransactionalEventListener(id = "artifactory.release-job-launcher", classes = ArtifactoryEvent.Release.class)
	void released(@NonNull ArtifactoryEvent.Release event) throws Exception {
		launch(ArtifactoryJobNames.RELEASE_JOB, builder -> builder
				.addString("artifact", event.coordinates().format(), true)
		);
	}

	void launch(String name, Consumer<JobParametersBuilder> customizer) throws Exception {
		final JobParametersBuilder builder = new JobParametersBuilder();
		customizer.accept(builder);

		final JobParameters parameters = builder.toJobParameters();

		log.debug("Attempting to launch job [{}] with parameters: {}", name, parameters);

		final Job job = locator.getJob(name);

		launcher.run(job, parameters);
	}

}
