package com.konfigyr.vault.environment;

import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.*;
import com.konfigyr.vault.Properties;
import com.konfigyr.vault.state.RepositoryStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class ConfigurationEnvironmentLocator {

	private final VaultAccessor vaultAccessor;
	private final ProfileManager profileManager;
	private final ConfigurationCache configurationCache;

	public ConfigurationEnvironment locate(AuthenticatedPrincipal principal, Service service, String profile) {
		return locate(principal, service, Collections.singleton(profile));
	}

	public ConfigurationEnvironment locate(AuthenticatedPrincipal principal, Service service, String... profiles) {
		return locate(principal, service, List.of(profiles));
	}

	public ConfigurationEnvironment locate(AuthenticatedPrincipal principal, Service service, Collection<String> profileNames) {
		if (CollectionUtils.isEmpty(profileNames)) {
			return new ConfigurationEnvironment(service.slug(), List.of(), List.of());
		}

		final List<PropertySource> properties = new ArrayList<>(profileNames.size());

		for (String profileName : profileNames) {
			profileManager.get(service, profileName).map(profile -> createPropertySource(
					principal, service, profile)
			).ifPresentOrElse(properties::add, () -> log.warn(
					"Profile '{}' not found for service '{}'", profileName, service.slug()
			));
		}

		if (log.isDebugEnabled()) {
			log.debug("Successfully located configuration environment for '{}' service and {} profiles: {}",
					service, StringUtils.collectionToCommaDelimitedString(profileNames), properties);
		}

		return new ConfigurationEnvironment(service.slug(), List.copyOf(profileNames), properties);
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
