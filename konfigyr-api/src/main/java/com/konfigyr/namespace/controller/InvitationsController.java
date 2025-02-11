package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.INVITE_MEMBERS)
@RequestMapping("/namespaces/{slug}/invitations")
class InvitationsController {

	private final Invitations invitations;
	private final NamespaceManager namespaces;
	private final RepresentationModelAssembler<Invitation, EntityModel<Invitation>> assembler = Assemblers.invitation();

	@GetMapping
	@PreAuthorize("isAdmin(#slug)")
	PagedModel<EntityModel<Invitation>> find(@PathVariable @NonNull String slug, Pageable pageable) {
		return assembler.assemble(invitations.find(lookupNamespace(slug), pageable));
	}

	@GetMapping("{key}")
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<Invitation> get(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		return assembler.assemble(lookupInvitation(slug, key));
	}

	@PostMapping
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<Invitation> create(@PathVariable @NonNull String slug, @Validated @RequestBody InvitationAttributes attributes) {
		final Invite invite = attributes.create(lookupNamespace(slug));

		return assembler.assemble(invitations.create(invite));
	}

	@PostMapping("{key}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void accept(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		invitations.accept(lookupInvitation(slug, key), EntityId.from(1));
	}

	@DeleteMapping("{key}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cancel(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		invitations.cancel(lookupInvitation(slug, key));
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	Invitation lookupInvitation(@NonNull String slug, @NonNull String key) {
		return invitations.get(lookupNamespace(slug), key).orElseThrow(() -> new InvitationException(
				InvitationException.ErrorCode.INVITATION_NOT_FOUND, "Invitation with key \"%s\" not found".formatted(key)
		));
	}

	record InvitationAttributes(@NotBlank @Email String email, @NotNull NamespaceRole role) {
		Invite create(Namespace namespace) {
			return new Invite(namespace.id(), EntityId.from(1), email(), role());
		}
	}

}
