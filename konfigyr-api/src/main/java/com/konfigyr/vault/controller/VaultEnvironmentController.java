package com.konfigyr.vault.controller;

import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.basic.BasicAuthenticatedPrincipal;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.Vault;
import com.konfigyr.vault.VaultAccessor;
import com.konfigyr.vault.environment.ConfigEnvironment;
import com.konfigyr.vault.environment.PropertySource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/configs")
public class VaultEnvironmentController extends AbstractVaultController {

	private final VaultAccessor accessor;

	VaultEnvironmentController(NamespaceManager namespaces, Services services, ProfileManager profiles, VaultAccessor accessor) {
		super(namespaces, profiles, services);
		this.accessor = accessor;
	}

	@GetMapping("{service}/{profiles}")
	ConfigEnvironment configs(@PathVariable String service, @PathVariable String profiles) {
		return configs(service, profiles, null);
	}

	@GetMapping("{service}/{profiles}/{label}")
	ConfigEnvironment configs(@PathVariable String service, @PathVariable String profiles, @PathVariable(required = false) String label) {
		final BasicAuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve(BasicAuthenticatedPrincipal.class);
		final VaultAssembler assembler = createAssembler(principal.getNamespace(), service);
		final String[] profilesArray = StringUtils.commaDelimitedListToStringArray(profiles);
		final List<PropertySource> properties = new ArrayList<>();

		for (String profileName: profilesArray) {
			this.profiles.get(assembler.service(), profileName)
					.ifPresent(profile -> {
						try (Vault vault = accessor.open(principal, assembler.service(), profile)) {
							properties.add(
									new PropertySource(service + "-" + profileName, new LinkedHashMap<>(vault.unseal()))
							);
						} catch (Exception e) {
							throw new RuntimeException("Failed to fetch properties for service=" + assembler.service() +
									", profile= " + profileName, e);
						}
					});

		}

		return new ConfigEnvironment(service, profilesArray, properties);
	}

}
