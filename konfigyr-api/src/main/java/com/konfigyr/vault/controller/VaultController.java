package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.vault.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
	Map<String, String> properties(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			return vault.unseal();
		}
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

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("profiles/{profileName}/history")
	PagedModel<EntityModel<ChangeHistory>> history(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@PageableDefault @NonNull Pageable pageable
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		Page<ChangeHistory> result;
		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			result = vault.history(pageable);
		}

		return assembler.changeHistory(profile).assemble(result);
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
