package com.konfigyr.artifactory;

import com.github.zafarkhaja.semver.Version;
import com.konfigyr.artifactory.store.FileSystemMetadataStore;
import com.konfigyr.artifactory.store.MetadataStore;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.nio.file.Path;

@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class ArtifactoryAutoConfiguration implements WebMvcConfigurer {

	@Override
	public void addFormatters(@NonNull FormatterRegistry registry) {
		registry.addConverter(String.class, Version.class, Version::valueOf);
		registry.addConverter(Version.class, String.class, Version::toString);
		registry.addConverter(String.class, ArtifactCoordinates.class, ArtifactCoordinates::parse);
		registry.addConverter(ArtifactCoordinates.class, String.class, ArtifactCoordinates::toString);
	}

	@Bean
	MetadataStore metadataStore(@Value("${konfigyr.artifactory.metadata-store.root}") URI root) {
		return new FileSystemMetadataStore(Path.of(root));
	}

	@Bean
	@ConditionalOnMissingBean(Artifactory.class)
	Artifactory defaultArtifactory(DSLContext context, MetadataStore store, ApplicationEventPublisher eventPublisher) {
		return new DefaultArtifactory(context, store, eventPublisher);
	}
}
