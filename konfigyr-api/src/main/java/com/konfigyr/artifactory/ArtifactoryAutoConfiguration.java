package com.konfigyr.artifactory;

import com.konfigyr.artifactory.converter.ArtifactoryConverters;
import com.konfigyr.artifactory.store.FileSystemMetadataStore;
import com.konfigyr.artifactory.store.MetadataStore;
import com.konfigyr.version.Version;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Path;

@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class ArtifactoryAutoConfiguration implements WebMvcConfigurer {

	@Override
	public void addFormatters(@NonNull FormatterRegistry registry) {
		registry.addConverter(String.class, Version.class, Version::of);
		registry.addConverter(Version.class, String.class, Version::toString);
		registry.addConverter(String.class, ArtifactCoordinates.class, ArtifactCoordinates::parse);
		registry.addConverter(ArtifactCoordinates.class, String.class, ArtifactCoordinates::toString);
	}

	@Bean
	ArtifactoryJacksonModule artifactoryJacksonModule() {
		return new ArtifactoryJacksonModule();
	}

	@Bean
	ArtifactoryConverters artifactoryConverters(JsonMapper jsonMapper) {
		return new ArtifactoryConverters(jsonMapper);
	}

	@Bean
	MetadataStore metadataStore(@Value("${konfigyr.artifactory.metadata-store.root}") URI root) {
		return new FileSystemMetadataStore(Path.of(root));
	}

	@Bean
	@ConditionalOnMissingBean(Artifactory.class)
	Artifactory defaultArtifactory(
			DSLContext context,
			MetadataStore store,
			ArtifactoryConverters converters,
			ApplicationEventPublisher eventPublisher
	) {
		return new DefaultArtifactory(context, store, converters, eventPublisher);
	}
}
