package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.Artifactory;
import com.konfigyr.artifactory.ArtifactoryConverters;
import com.konfigyr.artifactory.OwnerResolver;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class ServiceManifestConfiguration {

	private final DSLContext context;

	@Bean
	ServiceManifests serviceManifests(
			Artifactory artifactory,
			ArtifactoryConverters converters,
			ApplicationEventPublisher publisher,
			OwnerResolver ownerResolver
	) {
		return new DefaultServiceManifests(context, artifactory, converters, publisher, ownerResolver);
	}

}
