package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.provenance.ProvenanceConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
class ArtifactoryBatchJobConfiguration {

	private final JobRepository repository;

	@Bean(name = ArtifactoryJobNames.RELEASE_JOB)
	Job releaseJob(
			@Qualifier(ProvenanceConfiguration.PROVENANCE_STEP) Step provenance
	) {
		return new JobBuilder(ArtifactoryJobNames.RELEASE_JOB, repository)
				.validator(createReleaseParametersValidator())
				.start(provenance)
				.build();
	}

	@Bean
	ArtifactoryJobListener artifactoryJobListener(JobLocator locator, JobLauncher launcher) {
		return new ArtifactoryJobListener(locator, launcher);
	}

	static JobParametersValidator createReleaseParametersValidator() {
		return new DefaultJobParametersValidator(new String[] { "artifact" }, new String[0]);
	}

}
