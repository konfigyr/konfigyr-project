package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.*;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.security.oauth.RequiresScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

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
		return assembler.assemble(lookupInvitation(lookupNamespace(slug), key));
	}

	@PostMapping
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<Invitation> create(@PathVariable @NonNull String slug, @Validated @RequestBody InvitationAttributes attributes) {
		final Namespace namespace = lookupNamespace(slug);

		return assembler.assemble(invitations.create(namespace, attributes.create()));
	}

	@PostMapping("{key}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void accept(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		final Namespace namespace = lookupNamespace(slug);
		final Invitation invitation = lookupInvitation(namespace, key);

		invitations.accept(namespace, invitation, authenticatedAccountEntityId(principal ->
				"Invitation can not be accepted by a non-user account principal: " + principal));
	}

	@DeleteMapping("{key}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cancel(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		final Namespace namespace = lookupNamespace(slug);
		final Invitation invitation = lookupInvitation(namespace, key);

		invitations.cancel(namespace, invitation);
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	Invitation lookupInvitation(@NonNull Namespace namespace, @NonNull String key) {
		return invitations.get(namespace, key).orElseThrow(() -> new InvitationException(
				InvitationException.ErrorCode.INVITATION_NOT_FOUND, "Invitation with key \"%s\" not found".formatted(key)
		));
	}

	static EntityId authenticatedAccountEntityId(Function<AuthenticatedPrincipal, String> errorMessageFactory) {
		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();

		if (principal.getType() != PrincipalType.USER_ACCOUNT) {
			throw new AccessDeniedException(errorMessageFactory.apply(principal));
		}

		return EntityId.from(principal.get());
	}

	record InvitationAttributes(@NotBlank @Email String email, @NotNull NamespaceRole role) {
		Invite create() {
			final EntityId sender = authenticatedAccountEntityId(principal ->
					"Invitation attempt was made by a non-user account principal: " + principal);

			return new Invite(sender, email(), role());
		}
	}

}
