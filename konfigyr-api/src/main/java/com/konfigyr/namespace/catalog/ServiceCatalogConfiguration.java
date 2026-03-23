package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactoryConverters;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ServiceCatalogConfiguration {

	@Bean
	ConfigurationCatalogService configurationCatalogService(DSLContext context, ArtifactoryConverters converters) {
		return new ConfigurationCatalogService(context, converters);
	}

}
