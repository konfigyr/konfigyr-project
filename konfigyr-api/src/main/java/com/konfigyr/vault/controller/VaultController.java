package com.konfigyr.vault.controller;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.CursorModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.vault.*;
import com.konfigyr.vault.history.RevisionNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/namespaces/{namespace}/services/{service}")
public class VaultController extends AbstractVaultController {

	private final VaultAccessor accessor;
	private final VaultChronicle chronicle;

	VaultController(NamespaceManager namespaces, Services services, ProfileManager profiles,
					VaultAccessor accessor, VaultChronicle chronicle) {
		super(namespaces, profiles, services);
		this.accessor = accessor;
		this.chronicle = chronicle;
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
	EntityModel<RevisionInformation> apply(
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

		return assembler.<RevisionInformation>of().assemble(new RevisionInformation(result));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@PostMapping("profiles/{profileName}/submit")
	EntityModel<ChangeRequest> submit(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@RequestBody @Validated ChangesetRequest request
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);
		final ChangeRequest result;

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			result = vault.submit(request.changes(profile));
		}

		return assembler.changeRequest().assemble(result);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("profiles/{profileName}/history")
	CursorModel<EntityModel<ChangeHistory>> history(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@NonNull CursorPageable pageable
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		return assembler.changeHistory(profile)
				.assemble(chronicle.fetchHistory(profile, pageable));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("profiles/{profileName}/history/{revision}")
	CollectionModel<EntityModel<ChangeHistoryRecord>> history(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@PathVariable String revision
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		final List<PropertyHistory> history = chronicle.examine(profile, revision)
				.map(chronicle::traceRevision)
				.orElseThrow(() -> new RevisionNotFoundException(profile.slug(), revision));

		final List<ChangeHistoryRecord> changes;

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			changes = history.stream()
					.map(it -> ChangeHistoryRecord.from(it, vault))
					.toList();
		}

		return assembler.<ChangeHistoryRecord>of().assemble(changes);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("profiles/{profileName}/property/{propertyName}/history")
	CursorModel<EntityModel<ChangeHistoryRecord>> transitions(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable String profileName,
			@PathVariable String propertyName,
			@NonNull CursorPageable pageable
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final Profile profile = lookupProfile(assembler.service(), profileName);

		final CursorPage<PropertyHistory> history = chronicle.traceProperty(profile, propertyName, pageable);
		final CursorPage<ChangeHistoryRecord> changes;

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), profile)) {
			changes = history.map(it -> ChangeHistoryRecord.from(it, vault));
		}

		return assembler.<ChangeHistoryRecord>of().assemble(changes);
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
