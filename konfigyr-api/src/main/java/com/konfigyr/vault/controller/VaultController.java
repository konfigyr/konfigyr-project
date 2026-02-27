package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.vault.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/namespaces/{namespace}/services/{service}")
public class VaultController extends AbstractVaultController {

	private final VaultAccessor accessor;

	VaultController(NamespaceManager namespaces, Services services, ProfileManager profiles, VaultAccessor accessor) {
		super(namespaces, profiles, services);
		this.accessor = accessor;
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("profiles/{profileName}/properties")
	CollectionModel<EntityModel<ConfigurationProperty>> properties(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);
		final List<ConfigurationProperty> properties = new ArrayList<>();

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			vault.unseal().forEach((name, value) -> properties.add(
					new ConfigurationProperty(name, value)
			));
		}

		return assembler.<ConfigurationProperty>properties().assemble(properties);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@PostMapping("profiles/{profileName}/apply")
	EntityModel<ApplyResult> apply(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@RequestBody @Validated ChangesetRequest request
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);
		final ApplyResult result;

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			result = vault.apply(request.changes(profile));
		}

		return assembler.<ApplyResult>properties().assemble(result);
	}

	/*
	 * Temporary record that maps to the UI structure, it should be replaced once the Kofigyr registry is
	 * built and connected to the Vault to retrieve the configuration property metadata.
	 */
	record ConfigurationProperty(String name, String value, String state, Map<String, String> schema) {
		ConfigurationProperty(String name, String value) {
			this(name, value, "unchanged", Map.of("type", "string"));
		}
	}

	record ChangesetRequest(@NotBlank String name, String description, @NotEmpty Set<PropertyChange> changes) {

		PropertyChanges changes(Profile profile) {
			return PropertyChanges.builder()
					.profile(profile)
					.subject(name)
					.description(description)
					.add(changes)
					.build();
		}

	}

}
