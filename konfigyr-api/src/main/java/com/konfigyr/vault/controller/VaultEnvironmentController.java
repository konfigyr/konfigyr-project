package com.konfigyr.vault.controller;

import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.basic.NamespaceApplicationPrincipal;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.environment.ConfigurationEnvironment;
import com.konfigyr.vault.environment.ConfigurationEnvironmentLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/configs")
public class VaultEnvironmentController extends AbstractVaultController {

	private final ConfigurationEnvironmentLocator locator;

	VaultEnvironmentController(NamespaceManager namespaces, Services services, ProfileManager profiles, ConfigurationEnvironmentLocator locator) {
		super(namespaces, profiles, services);
		this.locator = locator;
	}

	@GetMapping("{service}/{profiles}")
	ConfigurationEnvironment configs(@PathVariable String service, @PathVariable String profiles) {
		return configs(service, profiles, null);
	}

	@GetMapping("{service}/{profiles}/{label}")
	ConfigurationEnvironment configs(
			@PathVariable(name = "service") String serviceName,
			@PathVariable(name = "profiles") String profileNames,
			@PathVariable(name = "label", required = false) String ignore
	) {
		final NamespaceApplicationPrincipal principal = AuthenticatedPrincipal.resolve();
		final Namespace namespace = lookupNamespace(principal.getNamespace());
		final Service service = lookupService(namespace, serviceName);

		return locator.locate(principal, service, StringUtils.commaDelimitedListToStringArray(profileNames));
	}

}
