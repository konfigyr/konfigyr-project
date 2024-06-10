package com.konfigyr.security.provision;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountExistsException;
import com.konfigyr.namespace.NamespaceExistsException;
import com.konfigyr.security.provisioning.ProvisioningRequiredException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;

/**
 * Controller used to render the account provisioning page when user account needs to be registered.
 * <p>
 * It would also handle the <code>POST</code> request containing the {@link ProvisioningForm} data
 * and attempt to create a new {@link Account} and {@link com.konfigyr.namespace.Namespace}.
 *
 * @author Vladimir Spasic
 **/
@Controller
@RequiredArgsConstructor
@RequestMapping("${konfigyr.security.provisioning.page:/provision}")
class ProvisioningController {

	static String PROVISIONING_VIEW = "accounts/provision";

	private final Provisioner provisioner;

	@GetMapping
	Object provision(@NonNull Model model, @NonNull HttpSession session) {
		final Object value = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

		if (value instanceof ProvisioningRequiredException ex) {
			model.addAttribute("form", ProvisioningForm.from(ex.getHints()));
			return PROVISIONING_VIEW;
		}

		return new RedirectView("/login");
	}

	@PostMapping
	Object submit(@ModelAttribute("form") @Validated ProvisioningForm form, BindingResult errors) {
		if (errors.hasErrors()) {
			return new ModelAndView(PROVISIONING_VIEW, HttpStatus.BAD_REQUEST);
		}

		UriComponents forwardUri = null;

		try {
			forwardUri = provisioner.provision(form);
		} catch (AccountExistsException e) {
			errors.rejectValue("email", "errors.account.exists", e.getMessage());
		} catch (NamespaceExistsException e) {
			errors.rejectValue("namespace", "errors.namespace.exists", e.getMessage());
		} catch (Exception e) {
			errors.reject("errors.provisioning", e.getMessage());
		}

		if (errors.hasErrors()) {
			return new ModelAndView(PROVISIONING_VIEW, HttpStatus.BAD_REQUEST);
		}

		Assert.state(forwardUri != null, "Provisioning Forward URI is null");
		return "forward:" + forwardUri.toUriString();
	}

}
