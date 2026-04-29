package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactoryConverters;
import com.konfigyr.queue.QueueProcessorRegistration;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	ServiceCatalogQueueListener serviceCatalogQueue() {
		return new ServiceCatalogQueueListener(context, properties.getBuildDebouncePeriod());
	}

	@Bean
	QueueProcessorRegistration serviceCatalogBuilderProcessorRegistration(ServiceCatalogWorker worker) {
		return QueueProcessorRegistration.of(ServiceCatalogQueueListener.QUEUE_NAME, worker::build)
				.backoff(properties.getBuildDebouncePeriod())
				.timeout(properties.getBuildTimeout());
	}

}
