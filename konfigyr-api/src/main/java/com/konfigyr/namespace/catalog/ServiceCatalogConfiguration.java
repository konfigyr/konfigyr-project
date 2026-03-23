package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactoryConverters;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceCatalogProperties.class)
public class ServiceCatalogConfiguration {

	private final DSLContext context;
	private final ServiceCatalogProperties properties;

	@Bean
	ConfigurationCatalogService configurationCatalogService(ArtifactoryConverters converters) {
		return new ConfigurationCatalogService(context, converters);
	}

	@Bean
	ServiceCatalogWorker serviceCatalogBuilder() {
		return new ServiceCatalogWorker(context);
	}

	@Bean
	ServiceCatalogQueue serviceCatalogQueue() {
		return new ServiceCatalogQueue(context, properties.getBuildDebouncePeriod(), properties.getParallelBuilds());
	}

	@Bean
	ServiceCatalogScheduler serviceCatalogScheduler(
			ServiceCatalogWorker worker,
			ServiceCatalogQueue queue,
			@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
			TaskExecutor taskExecutor
	) {
		return new ServiceCatalogScheduler(queue, worker, taskExecutor, properties.getBuildTimeout());
	}

}
