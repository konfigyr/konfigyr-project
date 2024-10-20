package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.integration.Integration;
import com.konfigyr.integration.IntegrationManager;
import com.konfigyr.registry.Artifactory;
import com.konfigyr.registry.Repository;
import com.konfigyr.security.AccountPrincipal;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;
import java.util.function.*;

/**
 * Controller that handles the {@link Namespace} related request mappings.
 *
 * @author Vladimir Spasic
 **/
@Slf4j
@Controller
@RequiredArgsConstructor
public class NamespaceController implements MessageSourceAware {

	private static final UriComponents NAMESPACE_URI = UriComponentsBuilder
			.fromPath("/namespace/{namespace}")
			.build();

	private static final UriComponents NAMESPACE_MEMBERS_URI = UriComponentsBuilder
			.fromPath("/namespace/{namespace}/members")
			.build();

	private static final UriComponents NAMESPACE_SETTINGS_URI = UriComponentsBuilder
			.fromPath("/namespace/{namespace}/settings")
			.build();

	private final Artifactory artifactory;
	private final Invitations invitations;
	private final IntegrationManager integrations;
	private final NamespaceManager manager;
	private final NamespaceSettingsService settings;

	private MessageSourceAccessor accessor;

	@Override
	public void setMessageSource(@NonNull MessageSource messageSource) {
		this.accessor = new MessageSourceAccessor(messageSource);
	}

	/**
	 * Request mapping that would render the {@link Namespace} details page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}")
	ModelAndView namespace(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		final Namespace namespace = lookupNamespace(slug);
		final Page<Repository> repositories = artifactory.searchRepositories(
				SearchQuery.builder()
						.criteria(SearchQuery.NAMESPACE, namespace.slug())
						.pageable(Pageable.ofSize(20))
						.build()
		);

		model.addAttribute("namespace", namespace)
				.addAttribute("repositories", repositories);

		return new ModelAndView("namespaces/details", model.asMap());
	}

	/**
	 * Request mapping that would render the {@link Namespace} {@link Member members} page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/members</code> template
	 */
	@GetMapping("/namespace/{namespace}/members")
	ModelAndView members(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		final Namespace namespace = lookupNamespace(slug);
		final Page<Member> members = manager.findMembers(namespace, Pageable.unpaged());

		model.addAttribute("namespace", namespace)
				.addAttribute("members", members)
				.addAttribute("invitationForm", InvitationForm.empty());

		return new ModelAndView("namespaces/members", model.asMap());
	}

	/**
	 * Request mapping that would handle the POST request that would update the {@link Member members}
	 * {@link NamespaceRole} in the {@link Namespace}.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param member identity identifier of the member to be updated, can't be {@literal null}
	 * @param role new namespace role to be assigned to the member, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success message, can't be {@literal null}
	 * @return <code>namespaces/members</code> template
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/members/update")
	RedirectView updateMember(
			@PathVariable("namespace") @NonNull String slug,
			@RequestParam("member") @NonNull EntityId member,
			@RequestParam("role") @NonNull NamespaceRole role,
			@NonNull RedirectAttributes redirectAttributes) {
		manager.updateMember(member, role);

		redirectAttributes.addFlashAttribute("notification", messageFor(
				"namespace.members.notifications.updated", "Member was successfully updated."
		));

		// redirect to the main members page when member has been removed
		return new RedirectView(NAMESPACE_MEMBERS_URI.expand(slug).toUriString());
	}

	/**
	 * Request mapping that would handle the POST request that would remove the {@link Member}
	 * from the {@link Namespace}.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param member identity identifier of the member to be removed, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success message, can't be {@literal null}
	 * @return <code>namespaces/members</code> template
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/members/remove")
	RedirectView removeMember(
			@PathVariable("namespace") @NonNull String slug,
			@RequestParam("member") @NonNull EntityId member,
			@NonNull RedirectAttributes redirectAttributes) {
		manager.removeMember(member);

		redirectAttributes.addFlashAttribute("notification", messageFor(
				"namespace.members.notifications.removed", "Member was successfully removed."
		));

		// redirect to the main members page when member has been removed
		return new RedirectView(NAMESPACE_MEMBERS_URI.expand(slug).toUriString());
	}

	/**
	 * Request mapping that would create a new {@link Invitation} for the {@link Namespace} based
	 * on the data entered in the {@link InvitationForm}.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success message, can't be {@literal null}
	 * @param sender currently logged-in user account, can't be {@literal null}
	 * @param invitationForm the invitation form data, can't be {@literal null}
	 * @param errors Spring MVC errors container, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/members</code> template
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/members")
	Object invite(
			@PathVariable("namespace") @NonNull String slug,
			@NonNull RedirectAttributes redirectAttributes,
			@AuthenticationPrincipal AccountPrincipal sender,
			@ModelAttribute @Validated InvitationForm invitationForm,
			@NonNull BindingResult errors,
			@NonNull Model model) {
		final Namespace namespace = lookupNamespace(slug);

		if (errors.hasErrors()) {
			model.addAttribute("namespace", namespace);
			model.addAttribute("members", manager.findMembers(namespace));

			return new ModelAndView("namespaces/members", model.asMap());
		}

		final Invite invite = new Invite(namespace.id(), sender.getId(), invitationForm.email(), invitationForm.role);

		try {
			invitations.create(invite);
		} catch (InvitationException ex) {
			errors.addError(new ObjectError(errors.getObjectName(), messageFor(ex)));

			model.addAttribute("namespace", namespace);
			model.addAttribute("members", manager.findMembers(namespace));

			return new ModelAndView("namespaces/members", model.asMap());
		}

		// store the success message to redirect attributes in order to render it to the user
		redirectAttributes.addFlashAttribute("success", accessor.getMessage(
				"invitation.form.success", new Object[] { invitationForm.email() }
		));

		// redirect to the main members page when invitation has been sent
		return new RedirectView(NAMESPACE_MEMBERS_URI.expand(slug).toUriString());
	}

	/**
	 * Request mapping that would render the {@link Namespace} pending {@link Invitation invitations} page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/members</code> template
	 */
	@PreAuthorize("isAdmin(#slug)")
	@GetMapping("/namespace/{namespace}/members/invitations")
	ModelAndView invitations(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		final Namespace namespace = lookupNamespace(slug);
		final Page<Invitation> invitations = this.invitations.find(namespace, Pageable.unpaged());

		model.addAttribute("namespace", namespace);
		model.addAttribute("invitations", invitations);

		return new ModelAndView("namespaces/invitations", model.asMap());
	}

	/**
	 * Request mapping that would render the {@link Namespace} invitation page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param key  invitation key, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/members</code> template
	 */
	@PreAuthorize("isAuthenticated()")
	@RequestMapping("/namespace/{namespace}/members/invitation/{key}")
	ModelAndView invitation(@PathVariable("namespace") String slug, @PathVariable String key, Model model) {
		final Namespace namespace = lookupNamespace(slug);

		invitations.get(namespace, key)
				.filter(Predicate.not(Invitation::isExpired))
				.ifPresent(invitation -> model.addAttribute("invitation", invitation));

		model.addAttribute("namespace", namespace);

		return new ModelAndView("namespaces/invitation", model.asMap());
	}

	/**
	 * Request mapping that handles the {@link Invitation} accept POST request that would create a new
	 * {@link Namespace} member.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param key  invitation key, can't be {@literal null}
	 * @param principal currently logged-in user account, can't be {@literal null}
	 * @return redirect view to the namespace dashboard page
	 */
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/namespace/{namespace}/members/invitation/{key}")
	Object accept(
			@PathVariable("namespace") String slug,
			@PathVariable String key,
			@AuthenticationPrincipal AccountPrincipal principal
	) {
		final Namespace namespace = lookupNamespace(slug);
		final Invitation invitation = invitations.get(namespace, key)
				.orElseThrow(() -> new IllegalStateException("Failed to load invitation with key: " + key));

		invitations.accept(invitation, principal.getId());

		return new RedirectView(NAMESPACE_URI.expand(namespace.slug()).toUriString());
	}

	/**
	 * Request mapping that would render the {@link Namespace} repositories page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}/repositories")
	ModelAndView repositories(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		return namespace(slug, model);
	}

	/**
	 * Request mapping that would render the {@link Namespace} applications page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}/applications")
	ModelAndView vaults(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		return namespace(slug, model);
	}

	/**
	 * Request mapping that would render the {@link Namespace} settings page.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @param principal the authenticated principal, can't be {@literal null}
	 * @return <code>namespaces/settings/details</code> template
	 */
	@PreAuthorize("isAdmin(#slug)")
	@GetMapping("/namespace/{namespace}/settings")
	ModelAndView settings(
			@PathVariable("namespace") @NonNull String slug,
			@AuthenticationPrincipal AccountPrincipal principal,
			@NonNull Model model
	) {
		return setupNamspaceSettingsModelAndView(slug, principal, model, HttpStatus.OK, (it, namespace) -> it
				.addAttribute("nameForm", SettingsForm.of(namespace.name()))
				.addAttribute("urlForm", SettingsForm.of(namespace.slug()))
				.addAttribute("descriptionForm", SettingsForm.of(namespace.description()))
		);
	}

	/**
	 * Request mapping that would render the {@link Namespace} settings page where all available
	 * {@link Integration integrations} are displayed.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @param pageable paging instructions, can't be {@literal null}
	 * @return <code>namespaces/details</code> template
	 */
	@PreAuthorize("isAdmin(#slug)")
	@GetMapping("/namespace/{namespace}/settings/integrations")
	ModelAndView integrations(
			@PathVariable("namespace") @NonNull String slug,
			@NonNull Model model,
			@NonNull Pageable pageable
	) {
		final Namespace namespace = lookupNamespace(slug);
		final Page<Integration> integrations = this.integrations.find(namespace.id(), pageable);

		model.addAttribute("namespace", namespace)
				.addAttribute("integrations", integrations);

		return new ModelAndView("namespaces/settings/integrations", model.asMap());
	}

	/**
	 * Request mapping that would attempt to update a {@link Namespace} name with the given URL slug.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param form the submitted settings form, can't be {@literal null}
	 * @param principal currently logged in account, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success or error message,
	 *                           can't be {@literal null}
	 * @param errors the form validation errors, can't be {@literal null}
	 * @param model the current controller model, can't be {@literal null}
	 * @return a redirect view to either start or namespace settings page
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/settings/name")
	Object updateNamespaceName(
			@PathVariable("namespace") @NonNull String slug,
			@ModelAttribute("nameForm") @Validated(SettingsForm.NameValidation.class) SettingsForm form,
			@NonNull BindingResult errors,
			@NonNull @AuthenticationPrincipal AccountPrincipal principal,
			@NonNull RedirectAttributes redirectAttributes,
			@NonNull Model model
	) {
		if (errors.hasErrors()) {
			return setupNamspaceSettingsModelAndView(slug, principal, model, HttpStatus.BAD_REQUEST, (it, namespace) -> it
					.addAttribute("urlForm", SettingsForm.of(namespace.slug()))
					.addAttribute("descriptionForm", SettingsForm.of(namespace.description()))
			);
		}

		return performNamespaceUpdate(slug, redirectAttributes, service -> {
			service.name(slug, form.value());
			return slug;
		});
	}

	/**
	 * Request mapping that would attempt to update a {@link Namespace} URL slug.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param form the submitted settings form, can't be {@literal null}
	 * @param principal currently logged in account, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success or error message,
	 *                           can't be {@literal null}
	 * @param errors the form validation errors, can't be {@literal null}
	 * @param model the current controller model, can't be {@literal null}
	 * @return a redirect view to either start or namespace settings page
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/settings/rename")
	Object updateNamespaceSlug(
			@PathVariable("namespace") @NonNull String slug,
			@ModelAttribute("urlForm") @Validated(SettingsForm.SlugValidation.class) SettingsForm form,
			@NonNull BindingResult errors,
			@NonNull @AuthenticationPrincipal AccountPrincipal principal,
			@NonNull RedirectAttributes redirectAttributes,
			@NonNull Model model
	) {
		if (errors.hasErrors()) {
			return setupNamspaceSettingsModelAndView(slug, principal, model, HttpStatus.BAD_REQUEST, (it, namespace) -> it
					.addAttribute("nameForm", SettingsForm.of(namespace.name()))
					.addAttribute("descriptionForm", SettingsForm.of(namespace.description()))
			);
		}

		return performNamespaceUpdate(slug, redirectAttributes, service -> {
			final Slug value = Slug.slugify(form.value());

			service.slug(slug, value);
			return value.get();
		});
	}

	/**
	 * Request mapping that would attempt to update a {@link Namespace} description with the given URL slug.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param form the submitted settings form, can't be {@literal null}
	 * @param principal currently logged in account, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success or error message,
	 *                           can't be {@literal null}
	 * @param errors the form validation errors, can't be {@literal null}
	 * @param model the current controller model, can't be {@literal null}
	 * @return a redirect view to either start or namespace settings page
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/settings/description")
	Object updateNamespaceDescription(
			@PathVariable("namespace") @NonNull String slug,
			@ModelAttribute("descriptionForm") @Validated(SettingsForm.DescriptionValidation.class) SettingsForm form,
			@NonNull BindingResult errors,
			@NonNull @AuthenticationPrincipal AccountPrincipal principal,
			@NonNull RedirectAttributes redirectAttributes,
			@NonNull Model model
	) {
		if (errors.hasErrors()) {
			return setupNamspaceSettingsModelAndView(slug, principal, model, HttpStatus.BAD_REQUEST, (it, namespace) -> it
					.addAttribute("nameForm", SettingsForm.of(namespace.name()))
					.addAttribute("urlForm", SettingsForm.of(namespace.slug()))
			);
		}

		return performNamespaceUpdate(slug, redirectAttributes, service -> {
			service.description(slug, form.value());
			return slug;
		});
	}

	ModelAndView setupNamspaceSettingsModelAndView(
			@NonNull String slug,
			@NonNull AccountPrincipal principal,
			@NonNull Model model,
			@NonNull HttpStatusCode statusCode,
			@NonNull BiFunction<Model, Namespace, Model> consumer
	) {
		final Namespace namespace = lookupNamespace(slug);
		final Set<Member> administrators = manager.findMembers(namespace.id())
				.filter(member -> NamespaceRole.ADMIN.equals(member.role()))
				.filter(member -> !principal.getId().equals(member.account()))
				.toSet();

		return new ModelAndView("namespaces/settings/general", consumer.apply(model, namespace)
				.addAttribute("namespace", namespace)
				.addAttribute("administrators", administrators)
				.asMap(), statusCode);
	}

	RedirectView performNamespaceUpdate(@NonNull String namespace,
										@NonNull RedirectAttributes redirectAttributes,
										@NonNull Function<NamespaceSettingsService, String> executor) {
		final String updated;

		try {
			updated = executor.apply(settings);
		} catch (NamespaceNotFoundException ex) {
			return new RedirectView("/");
		} catch (Exception ex) {
			log.warn("Unexpected error occurred while updating '{}' namespace", namespace, ex);

			redirectAttributes.addFlashAttribute("notification", messageFor(
					"notification.settings.update.failed", "Unexpected error occurred while updating namespace."
			));

			return new RedirectView(NAMESPACE_SETTINGS_URI.expand(namespace).toUriString());
		}

		redirectAttributes.addFlashAttribute("notification", messageFor(
				"notification.settings.update.success", "Namespace was successfully updated."
		));

		return new RedirectView(NAMESPACE_SETTINGS_URI.expand(updated).toUriString());
	}

	/**
	 * Request mapping that would attempt to delete a {@link Namespace} with the given URL slug.
	 *
	 * @param slug namespace name slug, can't be {@literal null}
	 * @param redirectAttributes redirect attributes that should contain the success or error message,
	 *                           can't be {@literal null}
	 * @return a redirect view to either start or namespace settings page
	 */
	@PreAuthorize("isAdmin(#slug)")
	@PostMapping("/namespace/{namespace}/delete")
	RedirectView delete(@PathVariable("namespace") @NonNull String slug, @NonNull RedirectAttributes redirectAttributes) {
		try {
			manager.delete(slug);
		} catch (NamespaceNotFoundException ex) {
			return new RedirectView("/");
		} catch (Exception ex) {
			log.warn("Unexpected error occurred while deleting '{}' namespace", slug, ex);

			redirectAttributes.addFlashAttribute("notification", messageFor(
					"notification.settings.delete.failed", "Unexpected error occurred while deleting namespace."
			));

			return new RedirectView(NAMESPACE_SETTINGS_URI.expand(slug).toUriString());
		}

		redirectAttributes.addFlashAttribute("notification", messageFor(
				"notification.settings.delete.success", "Namespace was successfully removed."
		));

		return new RedirectView("/");
	}

	/**
	 * Request mapping that would perform a check if there are any {@link Namespace Namespaces}
	 * with the given name, or slug, present in the system.
	 *
	 * @param value namespace name value, can't be {@literal null}
	 * @param current current namespace name value, can be {@literal null}
	 * @param model Spring MVC model, can't be {@literal null}
	 * @return <code>namespaces/check-name</code> template
	 */
	@PostMapping("/namespaces/check-name")
	ModelAndView provision(
			@RequestParam("value") @NonNull String value,
			@RequestParam(name = "current", required = false) @Nullable String current,
			@NonNull Model model) {
		String slug;

		try {
			slug = Slug.slugify(value).get();
		} catch (IllegalArgumentException e) {
			slug = value;
		}

		final boolean unavailable;

		if (current != null && current.equals(value)) {
			unavailable = false;
		} else {
			unavailable = manager.exists(slug);
		}

		final HttpStatus status = unavailable ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;

		model.addAttribute("slug", slug)
				.addAttribute("unavailable", unavailable)
				.addAttribute("valid", slug.equals(value));

		return new ModelAndView("namespaces/check-name", model.asMap(), status);
	}

	@NonNull
	private Namespace lookupNamespace(@NonNull String slug) {
		if (!Slug.isValid(slug)) {
			throw new NamespaceNotFoundException(slug);
		}

		return manager.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	private String messageFor(@NonNull InvitationException ex) {
		return messageFor("invitation.error-codes." + ex.getCode(), ex.getMessage());
	}

	@NonNull
	private String messageFor(@NonNull String code, @NonNull String fallback) {
		return accessor.getMessage(code, fallback);
	}

	public record InvitationForm(@NotBlank @Email String email, @NotNull NamespaceRole role) {
		static InvitationForm empty() {
			return new InvitationForm(null, null);
		}
	}

	public record SettingsForm(
			@NotBlank(groups = { NameValidation.class, SlugValidation.class })
			@Length(min = 3, max = 30, groups = NameValidation.class)
			@Length(min = 3, max = 50, groups = SlugValidation.class)
			@Length(max = 255, groups = DescriptionValidation.class)
			String value
	) {

		static SettingsForm of(@Nullable String value) {
			return new SettingsForm(value);
		}

		interface NameValidation extends Default { }

		interface SlugValidation extends Default { }

		interface DescriptionValidation extends Default { }
	}

}
