package com.konfigyr.artifactory.provenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.PropertyMetadata;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.artifactory.store.MetadataStoreReader;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class ProvenanceConfiguration {

	/**
	 * The name of the Provenance {@link Step Spring Batch Step} that is used to read the uploaded Spring Boot
	 * configuration metadata and import it in the Konfigyr Artifactory.
	 */
	public static final String PROVENANCE_STEP = "com.konfigyr.batch.step.provenance";

	static final String PROVENANCE_STEP_READER = PROVENANCE_STEP + ".reader";
	static final String PROVENANCE_STEP_PROCESSOR = PROVENANCE_STEP + ".processor";
	static final String PROVENANCE_STEP_WRITER = PROVENANCE_STEP + ".writer";

	private final DSLContext context;

	@Bean
	@ConditionalOnMissingBean(ProvenanceEvaluator.class)
	ProvenanceEvaluator defaultProvenanceEvaluator() {
		return new DefaultProvenanceEvaluator(context);
	}

	@Bean(name = PROVENANCE_STEP)
	Step provenanceStep(
			JobRepository repository,
			PlatformTransactionManager transactionManager,
			@Qualifier(PROVENANCE_STEP_READER) MetadataStoreReader reader,
			@Qualifier(PROVENANCE_STEP_PROCESSOR) ProvenanceProcessor processor,
			@Qualifier(PROVENANCE_STEP_WRITER) ProvenanceEvaluationWriter writer
	) {
		return new StepBuilder(PROVENANCE_STEP, repository)
				.<PropertyMetadata, EvaluationResult>chunk(20, transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
	}

	@StepScope
	@Bean(name = PROVENANCE_STEP_READER)
	MetadataStoreReader provenanceStepReader(
			@Value("#{jobParameters['artifact']}") String coordinates,
			MetadataStore store
	) {
		return new MetadataStoreReader(coordinates, store);
	}

	@StepScope
	@Bean(name = PROVENANCE_STEP_PROCESSOR)
	ProvenanceProcessor provenanceStepProcessor(
			@Value("#{jobParameters['artifact']}") String coordinates,
			Artifactory artifactory,
			ProvenanceEvaluator evaluator
	) {
		return new ProvenanceProcessor(coordinates, artifactory, evaluator);
	}

	@StepScope
	@Bean(name = PROVENANCE_STEP_WRITER)
	ProvenanceEvaluationWriter provenanceStepWriter(ObjectMapper mapper) {
		return new ProvenanceEvaluationWriter(mapper, context);
	}

}
