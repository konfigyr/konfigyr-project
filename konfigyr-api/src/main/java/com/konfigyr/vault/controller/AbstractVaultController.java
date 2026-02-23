package com.konfigyr.vault.controller;

import com.konfigyr.namespace.*;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.ProfileNotFoundException;
import org.jspecify.annotations.NonNull;

abstract class AbstractVaultController {

	protected final NamespaceManager namespaces;
	protected final ProfileManager profiles;
	protected final Services services;

	protected AbstractVaultController(NamespaceManager namespaces, ProfileManager profiles, Services services) {
		this.namespaces = namespaces;
		this.profiles = profiles;
		this.services = services;
	}

	@NonNull
	Profile lookupProfile(Service service, String profileName) {
		return profiles.get(service, profileName).orElseThrow(() -> new ProfileNotFoundException(
				service.slug(), profileName
		));
	}

	@NonNull
	VaultAssembler createAssembler(@NonNull String namespaceSlug, @NonNull String serviceSlug) {
		final Namespace namespace = namespaces.findBySlug(namespaceSlug)
				.orElseThrow(() -> new NamespaceNotFoundException(namespaceSlug));

		final Service service = services.get(namespace, serviceSlug)
				.orElseThrow(() -> new ServiceNotFoundException(namespace.slug(), serviceSlug));

		return new VaultAssembler(namespace, service);
	}

}
