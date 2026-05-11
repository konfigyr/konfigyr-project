package com.konfigyr.vault.environment;

import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.VaultAccessor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConfigurationCacheProperties.class)
public class ConfigurationEnvironmentConfiguration {

	@Bean
	ConfigurationCache configurationCache(ConfigurationCacheProperties properties) {
		return new ConfigurationCache(properties.getSpecification());
	}

	@Bean
	ConfigurationEnvironmentLocator configurationEnvironmentLocator(
			VaultAccessor vaultAccessor,
			ProfileManager profileManager,
			ConfigurationCache configurationCache,
			ObservationRegistry observationRegistry
	) {
		return new ConfigurationEnvironmentLocator(vaultAccessor, profileManager, configurationCache, observationRegistry);
	}
}
