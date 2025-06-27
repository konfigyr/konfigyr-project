package com.konfigyr.account.controller;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountManager;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.Link;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.support.FullName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
class AccountController {

	private final RepresentationModelAssembler<Account, EntityModel<Account>> assembler =
			account -> EntityModel.of(account, Link.builder().path("account").selfRel())
					.add(Link.builder().path("account").method(HttpMethod.PATCH).rel("update"))
					.add(Link.builder().path("account").method(HttpMethod.DELETE).rel("delete"))
					.add(Link.builder().path("account/email").method(HttpMethod.POST).rel("initiate mail update"))
					.add(Link.builder().path("account/email").method(HttpMethod.PUT).rel("confirm mail update"));

	private final AccountManager accounts;
	private final AccountEmailVerificationService emailVerifier;

	@GetMapping
	EntityModel<Account> get(@NonNull Authentication authentication) {
		final Account account = retrieveAccountForAuthentication(authentication);
		return assembler.assemble(account);
	}

	@PostMapping("/email")
	@Transactional(readOnly = true)
	Object issue(@RequestBody @Validated MailHolder holder, @NonNull Authentication authentication) {
		final Account account = retrieveAccountForAuthentication(authentication);
		final String token = emailVerifier.issue(account, holder.email());
		return Map.of("token", token, "email", holder.email());
	}

	@PutMapping("/email")
	EntityModel<Account> verify(@RequestBody @Validated MailVerification verification, @NonNull Authentication authentication) {
		final Account account = retrieveAccountForAuthentication(authentication);
		final Account updated = emailVerifier.verify(account, verification.token(), verification.code());
		return assembler.assemble(updated);
	}

	@PatchMapping
	EntityModel<Account> update(@RequestBody @Validated AccountDetails details, @NonNull Authentication authentication) {
		final Account account = retrieveAccountForAuthentication(authentication);
		Account updated = details.apply(account);

		if (account != updated) {
			updated = accounts.update(updated);
		}

		return assembler.assemble(updated);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@NonNull Authentication authentication) {
		final Account account = retrieveAccountForAuthentication(authentication);
		accounts.delete(account.id());
	}

	Account retrieveAccountForAuthentication(Authentication authentication) {
		final EntityId id;

		try {
			id = EntityId.from(authentication.getName());
		} catch (IllegalArgumentException ex) {
			throw new AuthenticationCredentialsNotFoundException(
					"Failed to retrieve account identifier from authentication principal", ex
			);
		}

		return accounts.findById(id).orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
				"Failed to retrieve account for authenticated principal: " + authentication.getName()
		));
	}

	record MailHolder(@NotNull @Email String email) {

	}

	record MailVerification(@NotEmpty String token, @NotEmpty String code) {

	}

	record AccountDetails(FullName name) {

		@NonNull
		Account apply(Account account) {
			if (name == null) {
				return account;
			}

			return Account.builder(account)
					.firstName(name.firstName())
					.lastName(name.lastName())
					.build();
		}

	}
}
