package com.konfigyr.vault;

import com.konfigyr.crypto.KeysetOperationsFactory;
import com.konfigyr.namespace.Services;
import com.konfigyr.vault.extension.LockingVaultExtension;
import com.konfigyr.vault.state.RepositoryVaultManager;
import com.konfigyr.vault.state.StateRepositoryEventListener;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(VaultProperties.class)
public class VaultAutoConfiguration {

	private final DSLContext context;
	private final VaultProperties properties;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Bean
	@ConditionalOnMissingBean
	ProfileManager defaultProfileManager() {
		return new DefaultProfileManager(context, applicationEventPublisher);
	}

	@Bean
	VaultAccessor repositoryVaultManager(KeysetOperationsFactory keysetOperationsFactory) {
		return new RepositoryVaultManager(new LockingVaultExtension(), properties.getRepositoryDirectory(),
				keysetOperationsFactory);
	}

	@Bean
	StateRepositoryEventListener stateRepositoryEventListener(Services services) {
		return new StateRepositoryEventListener(services, properties.getRepositoryDirectory());
	}

}
