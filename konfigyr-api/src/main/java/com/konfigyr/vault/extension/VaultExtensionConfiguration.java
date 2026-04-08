package com.konfigyr.vault.extension;

import com.konfigyr.vault.VaultExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
public class VaultExtensionConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	VaultExtension lockingVaultExtension() {
		return new LockingVaultExtension();
	}

	@Bean
	@Order
	VaultExtension publishingVaultExtension(ApplicationEventPublisher eventPublisher) {
		return new PublishingVaultExtension(eventPublisher);
	}

}
