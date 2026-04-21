package com.konfigyr.vault.controller;

import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.markdown.MarkdownContents;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.vault.*;
import com.konfigyr.vault.changes.ChangeRequestManager;
import com.konfigyr.vault.changes.ChangeRequestReviewCommand;
import com.konfigyr.vault.changes.ChangeRequestUpdateCommand;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/namespaces/{namespace}/services/{service}")
class VaultChangeRequestController extends AbstractVaultController {

	ChangeRequestManager manager;
	VaultAccessor accessor;

	VaultChangeRequestController(
			NamespaceManager namespaces,
			Services services,
			ProfileManager profiles,
			ChangeRequestManager manager,
			VaultAccessor accessor
	) {
		super(namespaces, profiles, services);
		this.manager = manager;
		this.accessor = accessor;
	}

	@GetMapping("changes")
	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	PagedModel<EntityModel<ChangeRequest>> search(
			@PathVariable String namespace,
			@PathVariable String service,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String profile,
			@RequestParam(required = false) ChangeRequestState state,
			Pageable pageable
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);

		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.criteria(ChangeRequest.STATE_CRITERIA, state)
				.criteria(ChangeRequest.PROFILE_CRITERIA, profile)
				.pageable(pageable)
				.build();

		return assembler.changeRequest().assemble(manager.search(assembler.service(), query));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("changes/{number}")
	EntityModel<ChangeRequest> lookup(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);

		return assembler.changeRequest().assemble(lookupChangeRequest(assembler.service(), number));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@PutMapping("changes/{number}")
	EntityModel<ChangeRequest> update(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number,
			@RequestBody @Validated UpdateChangeRequest request
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);

		return assembler.changeRequest().assemble(manager.update(request.command(assembler.service(), number)));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("changes/{number}/history")
	CollectionModel<EntityModel<ChangeRequestHistory>> history(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final ChangeRequest request = lookupChangeRequest(assembler.service(), number);

		return assembler.<ChangeRequestHistory>of().assemble(manager.history(request));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.READ_PROFILES)
	@GetMapping("changes/{number}/changes")
	CollectionModel<EntityModel<ChangeRequestChange>> changes(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final ChangeRequest request = lookupChangeRequest(assembler.service(), number);
		final List<ChangeRequestChange> changes;

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), request.profile())) {
			changes = manager.changes(request).stream()
					.map(transition -> ChangeRequestChange.from(transition, vault))
					.toList();
		}

		return assembler.<ChangeRequestChange>of().assemble(changes);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@PostMapping("changes/{number}/merge")
	EntityModel<RevisionInformation> merge(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final ChangeRequest request = lookupChangeRequest(assembler.service(), number);
		final ApplyResult result;

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), request.profile())) {
			result = vault.merge(request);
		}

		return assembler.<RevisionInformation>of().assemble(new RevisionInformation(result));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.WRITE_PROFILES)
	@PostMapping("changes/{number}/review")
	EntityModel<ChangeRequest> review(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number,
			@RequestBody @Validated ReviewChangeRequest review
	) {
		final VaultAssembler assembler = createAssembler(namespace, service);
		final ChangeRequest request = manager.review(review.command(assembler.service(), number));
		return assembler.changeRequest().assemble(request);
	}

	@PreAuthorize("isMember(#namespace)")
	@RequiresScope(OAuthScope.DELETE_PROFILES)
	@DeleteMapping("changes/{number}")
	EntityModel<ChangeRequest> delete(
			@PathVariable String namespace,
			@PathVariable String service,
			@PathVariable Long number
	) throws Exception {
		final VaultAssembler assembler = createAssembler(namespace, service);
		ChangeRequest request = lookupChangeRequest(assembler.service(), number);

		try (Vault vault = accessor.open(AuthenticatedPrincipal.resolve(), assembler.service(), request.profile())) {
			request = vault.discard(request);
		}

		return assembler.changeRequest().assemble(request);
	}

	private ChangeRequest lookupChangeRequest(Service service, Long number) {
		return manager.get(service, number).orElseThrow(() -> new ChangeRequestNotFoundException(service, number));
	}

	private static String unseal(Vault vault, PropertyValue value) {
		if (value == null) {
			return null;
		}
		final PropertyValue unsealed = vault.unseal(value);
		return new String(unsealed.get().array(), StandardCharsets.UTF_8);
	}

	record UpdateChangeRequest(
			@Length(min = 2, max = 30) String subject,
			@Length(max = 65536) MarkdownContents description
	) {

		ChangeRequestUpdateCommand command(Service service, Long number) {
			return new ChangeRequestUpdateCommand(service, number, AuthenticatedPrincipal.resolve(), null, subject, description);
		}

	}

	record ReviewChangeRequest(
			@NotNull ChangeRequestReviewCommand.Operation state,
			@Length(max = 16384) MarkdownContents comment
	) {

		ChangeRequestReviewCommand command(Service service, Long number) {
			return new ChangeRequestReviewCommand(service, number, AuthenticatedPrincipal.resolve(), state, comment);
		}

	}

	record ChangeRequestChange(String name, PropertyTransitionType action, String from, String to) {
		static ChangeRequestChange from(PropertyTransition transition, Vault vault) {
			return new ChangeRequestChange(
					transition.name(),
					transition.type(),
					unseal(vault, transition.from()),
					unseal(vault, transition.to())
			);
		}
	}

}
