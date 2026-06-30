package com.konfigyr.namespace;

import com.konfigyr.artifactory.OwnerResolver;
import com.konfigyr.feature.FeatureDefinition;
import com.konfigyr.feature.FeatureDefinitionConfigurer;
import com.konfigyr.namespace.catalog.ServiceCatalogSource;
import com.konfigyr.security.PasswordEncoders;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;

/**
 * Spring autoconfiguration class for the {@code konfigyr-namespace} module.
 *
 * @author Vladimir Spasic
 **/
@AutoConfiguration
@RequiredArgsConstructor
@AutoConfigureAfter(JooqAutoConfiguration.class)
public class NamespaceManagementAutoConfiguration implements FeatureDefinitionConfigurer {

	private final DSLContext context;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void configure(@NonNull Collection<FeatureDefinition<?>> definitions) {
		definitions.add(NamespaceFeatures.MEMBERS_COUNT);
		definitions.add(NamespaceFeatures.SERVICES_COUNT);
	}

	@Bean
	@ConditionalOnMissingBean(NamespaceManager.class)
	NamespaceManager defaultNamespaceManager(ObjectProvider<@NonNull PasswordEncoder> passwordEncoder, JsonMapper jsonMapper) {
		final PasswordEncoder encoder = passwordEncoder.getIfAvailable(PasswordEncoders::get);
		final NamespaceConverters converters = new NamespaceConverters(jsonMapper);
		return new DefaultNamespaceManager(context, encoder, applicationEventPublisher, converters);
	}

	@Bean
	@ConditionalOnMissingBean(Services.class)
	Services defaultNamespaceServices(NamespaceManager namespaces, ServiceCatalogSource catalogSource) {
		return new DefaultServices(context, namespaces, catalogSource, applicationEventPublisher);
	}

	@Bean
	OwnerResolver namespaceOwnerResolver(NamespaceManager namespaces) {
		return new NamespaceOwnerResolver(namespaces);
	}

}
