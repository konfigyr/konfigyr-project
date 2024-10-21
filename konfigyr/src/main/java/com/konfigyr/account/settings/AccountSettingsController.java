package com.konfigyr.account.settings;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountException;
import com.konfigyr.account.AccountManager;
import com.konfigyr.account.AccountNotFoundException;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceType;
import com.konfigyr.security.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Spring controller that is used to render the {@link Account} profil settings page.
 * <p>
 * The profile page should contain a form that submits a {@link AccountSettingsForm} to update
 * user account attributes and a form to delete the {@link Account}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@Controller
@RequiredArgsConstructor
class AccountSettingsController {

	private final AccountManager manager;

	@ModelAttribute("id")
	EntityId extractAccountIdentifier(@AuthenticationPrincipal AccountPrincipal principal) {
		return principal.getId();
	}

	@GetMapping("/account")
	String profile(@ModelAttribute("id") EntityId id, @NonNull Model model) {
		setup(model, lookupAccount(id));

		return "accounts/profile";
	}

	@PostMapping("/account")
	Object update(
			@ModelAttribute("id") EntityId id,
			@ModelAttribute("form") @Validated AccountSettingsForm form,
			BindingResult errors,
			Model model
	) {
		final Account account = lookupAccount(id);

		if (errors.hasErrors()) {
			setup(model, account, form);

			return new ModelAndView("accounts/profile", model.asMap(), HttpStatus.BAD_REQUEST);
		}

		manager.update(form.apply(account));

		return "redirect:/account";
	}

	@PostMapping("/account/delete")
	Object delete(@ModelAttribute("id") EntityId id, BindingResult errors, Model model) {
		try {
			manager.delete(id);
		} catch (AccountNotFoundException e) {
			throw e;
		} catch (AccountException e) {
			log.warn("Failed to delete Account({}) due to an account exception", id, e);

			errors.addError(new ObjectError("delete", e.getMessage()));
		}

		if (errors.hasErrors()) {
			setup(model, lookupAccount(id));

			return new ModelAndView("accounts/profile", model.asMap(), HttpStatus.BAD_REQUEST);
		}

		return "redirect:/";
	}

	Account lookupAccount(@NonNull EntityId accountId) {
		return manager.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
	}

	static void setup(@NonNull Model model, @NonNull Account account) {
		setup(model, account, AccountSettingsForm.from(account));
	}

	static void setup(@NonNull Model model, @NonNull Account account, @NonNull AccountSettingsForm form) {
		model.addAttribute("account", account)
				.addAttribute("form", form)
				.addAttribute("memberships", account.memberships())
				.addAttribute("namespaceNames", account.memberships()
						.filter(membership -> NamespaceType.PERSONAL != membership.type()).join());
	}

}
