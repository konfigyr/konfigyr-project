package com.konfigyr.vault.controller;

import com.konfigyr.namespace.*;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.ProfileNotFoundException;
import org.jspecify.annotations.NullMarked;

@NullMarked
abstract class AbstractVaultController {

	protected final NamespaceManager namespaces;
	protected final ProfileManager profiles;
	protected final Services services;

	protected AbstractVaultController(NamespaceManager namespaces, ProfileManager profiles, Services services) {
		this.namespaces = namespaces;
		this.profiles = profiles;
		this.services = services;
	}

	Namespace lookupNamespace(String namespaceSlug) {
		return namespaces.findBySlug(namespaceSlug).orElseThrow(
				() -> new NamespaceNotFoundException(namespaceSlug)
		);
	}

	Service lookupService(Namespace namespace, String serviceSlug) {
		return services.get(namespace, serviceSlug).orElseThrow(
				() -> new ServiceNotFoundException(namespace.slug(), serviceSlug)
		);
	}

	Profile lookupProfile(Service service, String profileSlug) {
		return profiles.get(service, profileSlug).orElseThrow(
				() -> new ProfileNotFoundException(service.slug(), profileSlug)
		);
	}

	VaultAssembler createAssembler(String namespaceSlug, String serviceSlug) {
		final Namespace namespace = lookupNamespace(namespaceSlug);
		final Service service = lookupService(namespace, serviceSlug);
		return new VaultAssembler(namespace, service);
	}

}
