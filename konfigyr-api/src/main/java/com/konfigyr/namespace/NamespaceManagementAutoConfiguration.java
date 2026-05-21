package com.konfigyr.namespace;

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
	NamespaceManager defaultNamespaceManager(ObjectProvider<@NonNull PasswordEncoder> passwordEncoder) {
		final PasswordEncoder encoder = passwordEncoder.getIfAvailable(PasswordEncoders::get);
		return new DefaultNamespaceManager(context, encoder, applicationEventPublisher);
	}

	@Bean
	@ConditionalOnMissingBean(Services.class)
	Services defaultNamespaceServices(NamespaceManager namespaces, ServiceCatalogSource catalogSource) {
		return new DefaultServices(context, namespaces, catalogSource, applicationEventPublisher);
	}

}
