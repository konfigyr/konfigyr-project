package com.konfigyr.membership.controller;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.membership.*;
import com.konfigyr.namespace.*;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.security.oauth.RequiresScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequiredArgsConstructor
class InvitationsController {

	private final AccountManager accounts;
	private final Invitations invitations;
	private final NamespaceManager namespaces;

	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.INVITE_MEMBERS)
	@GetMapping("/namespaces/{slug}/invitations")
	PagedModel<EntityModel<Invitation>> find(@PathVariable @NonNull String slug, Pageable pageable) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.invitationForNamespace(namespace)
				.assemble(invitations.find(namespace, pageable));
	}

	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.INVITE_MEMBERS)
	@GetMapping("/namespaces/{slug}/invitations/{key}")
	EntityModel<Invitation> get(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.invitationForNamespace(namespace)
				.assemble(lookupInvitation(namespace, key));
	}

	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.INVITE_MEMBERS)
	@PostMapping("/namespaces/{slug}/invitations")
	EntityModel<Invitation> create(@PathVariable @NonNull String slug, @Validated @RequestBody InvitationAttributes attributes) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.invitationForNamespace(namespace)
				.assemble(invitations.create(namespace, attributes.create()));
	}

	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.INVITE_MEMBERS)
	@DeleteMapping("/namespaces/{slug}/invitations/{key}")
	void cancel(@PathVariable @NonNull String slug, @PathVariable @NonNull String key) {
		final Namespace namespace = lookupNamespace(slug);
		final Invitation invitation = lookupInvitation(namespace, key);

		invitations.cancel(namespace, invitation);
	}

	@GetMapping("/account/invitations")
	PagedModel<EntityModel<Invitation>> find(Pageable pageable) {
		return Assemblers.invitationForAccount()
				.assemble(invitations.find(lookupAccount(), pageable));
	}

	@GetMapping("/account/invitations/{key}")
	EntityModel<Invitation> get(@PathVariable @NonNull String key) {
		return Assemblers.invitationForAccount()
				.assemble(lookupInvitation(lookupAccount(), key));
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/account/invitations/{key}")
	void accept(@PathVariable @NonNull String key) {
		final Account account = lookupAccount();
		final Invitation invitation = lookupInvitation(account, key);

		invitations.accept(account, invitation);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/account/invitations/{key}")
	void decline(@PathVariable @NonNull String key) {
		final Account account = lookupAccount();
		final Invitation invitation = lookupInvitation(account, key);

		invitations.decline(account, invitation);
	}

	@NonNull
	Account lookupAccount() {
		final AuthenticatedPrincipal principal = AuthenticatedPrincipal.resolve();

		if (principal.getType() != PrincipalType.USER_ACCOUNT) {
			throw new AccessDeniedException("Account invitations can only be accessed by user account principals");
		}

		final EntityId id = EntityId.from(principal.get());

		return accounts.findById(id).orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
				"Could not find a user account that matches the authenticated principal subject: " + principal.get()
		));
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	Invitation lookupInvitation(@NonNull Account account, @NonNull String key) {
		return invitations.get(account, key).orElseThrow(() -> new InvitationException(
				InvitationException.ErrorCode.INVITATION_NOT_FOUND, "Invitation with key \"%s\" not found".formatted(key)
		));
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
