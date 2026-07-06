package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.ArtifactoryConverters;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class ServiceManifestConfiguration {

	private final DSLContext context;

	@Bean
	ServiceManifests serviceManifests(Artifactory artifactory, ArtifactoryConverters converters) {
		return new DefaultServiceManifests(context, artifactory, converters);
	}

}
