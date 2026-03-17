package com.konfigyr.artifactory.batch;

import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.provenance.ProvenanceConfiguration;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
class ArtifactoryBatchJobConfiguration {

	private final JobRepository repository;

	@Bean(name = ArtifactoryJobNames.RELEASE_JOB)
	Job releaseJob(
			@Qualifier(ProvenanceConfiguration.PROVENANCE_STEP) Step provenance,
			JobExecutionListener artifactoryJobExecutionListener
	) {
		return new JobBuilder(ArtifactoryJobNames.RELEASE_JOB, repository)
				.validator(createReleaseParametersValidator())
				.start(provenance)
				.listener(artifactoryJobExecutionListener)
				.build();
	}

	@Bean
	ArtifactoryJobExecutionListener artifactoryJobExecutionListener(
			ApplicationEventPublisher eventPublisher,
			Artifactory artifactory,
			DSLContext context
	) {
		return new ArtifactoryJobExecutionListener(eventPublisher, artifactory, context);
	}

	@Bean
	ArtifactoryJobListener artifactoryJobListener(JobRegistry registry, JobOperator operator) {
		return new ArtifactoryJobListener(registry, operator);
	}

	static JobParametersValidator createReleaseParametersValidator() {
		return new DefaultJobParametersValidator(new String[] { "artifact" }, new String[0]);
	}

}
