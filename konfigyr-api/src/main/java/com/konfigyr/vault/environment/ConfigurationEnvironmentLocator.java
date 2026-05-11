package com.konfigyr.vault.environment;

import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.*;
import com.konfigyr.vault.Properties;
import com.konfigyr.vault.state.RepositoryStateException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class ConfigurationEnvironmentLocator {

	private final VaultAccessor vaultAccessor;
	private final ProfileManager profileManager;
	private final ConfigurationCache configurationCache;
	private final ObservationRegistry observationRegistry;

	public ConfigurationEnvironment locate(AuthenticatedPrincipal principal, Service service, String profile) {
		return locate(principal, service, Collections.singleton(profile));
	}

	public ConfigurationEnvironment locate(AuthenticatedPrincipal principal, Service service, String... profiles) {
		return locate(principal, service, List.of(profiles));
	}

	public ConfigurationEnvironment locate(AuthenticatedPrincipal principal, Service service, Collection<String> profileNames) {
		if (CollectionUtils.isEmpty(profileNames)) {
			return new ConfigurationEnvironment(service.slug(), Collections.emptyList(), Collections.emptyList());
		}

		final Observation observation = ConfigurationEnvironmentObservation.create(observationRegistry, service);

		return observation.observe(() -> {
			final List<PropertySource> properties = new ArrayList<>(profileNames.size());

			for (String profileName : profileNames) {
				profileManager.get(service, profileName).map(profile -> {
					final PropertySource source = createPropertySource(principal, service, profile);
					observation.event(ConfigurationEnvironmentObservation.located(profileName));

					if (log.isDebugEnabled()) {
						log.debug("Successfully located configuration environment for '{}' service and {} profile: {}",
								service.id(), profileName, source);
					}

					return source;
				}).ifPresentOrElse(properties::add, () -> {
					log.warn("Profile '{}' not found for service '{}'", profileName, service.id());

					observation.event(ConfigurationEnvironmentObservation.missing(profileName));
				});
			}

			return new ConfigurationEnvironment(service.slug(), List.copyOf(profileNames), properties);
		});
	}

	private PropertySource createPropertySource(AuthenticatedPrincipal principal, Service service, Profile profile) {
		try (Vault vault = vaultAccessor.open(principal, service, profile)) {
			final Properties properties = configurationCache.get(service, profile, vault::state);
			final Map<String, String> unsealed = new HashMap<>(properties.size());

			properties.forEachProperty((name, value) -> unsealed.put(
					name, new String(vault.unseal(value).get().array(), StandardCharsets.UTF_8)
			));

			return new PropertySource(
					vault.service().slug() + "-" + vault.profile().slug(),
					Collections.unmodifiableMap(unsealed)
			);
		} catch (VaultException | RepositoryStateException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new VaultException(
					"Unexpected exception occurred while retrieving configuration state for: [service=%s, profile=%s]"
							.formatted(service.slug(), profile.slug()), ex);
		}
	}

}
