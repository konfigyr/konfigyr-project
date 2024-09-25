package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.registry.Artifactory;
import com.konfigyr.registry.Repository;
import com.konfigyr.security.AccountPrincipal;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
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

import java.util.function.Predicate;

/**
 * Controller that handles the {@link Namespace} related request mappings.
 *
 * @author Vladimir Spasic
 **/
@Controller
@RequiredArgsConstructor
public class NamespaceController implements MessageSourceAware {

	private static final UriComponents NAMESPACE_URI = UriComponentsBuilder
			.fromPath("/namespace/{namespace}")
			.build();

	private static final UriComponents NAMESPACE_MEMBERS_URI = UriComponentsBuilder
			.fromPath("/namespace/{namespace}/members")
			.build();

	private final Artifactory artifactory;
	private final Invitations invitations;
	private final NamespaceManager manager;

	private MessageSourceAccessor accessor;

	@Override
	public void setMessageSource(@NonNull MessageSource messageSource) {
		this.accessor = new MessageSourceAccessor(messageSource);
	}

	/**
	 * Request mapping that would render the {@link Namespace} details page.
	 *
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
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

		model.addAttribute("namespace", namespace);
		model.addAttribute("repositories", repositories);

		return new ModelAndView("namespaces/details", model.asMap());
	}

	/**
	 * Request mapping that would render the {@link Namespace} {@link Member members} page.
	 *
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
	 * @return <code>namespaces/members</code> template
	 */
	@GetMapping("/namespace/{namespace}/members")
	ModelAndView members(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		final Namespace namespace = lookupNamespace(slug);
		final Page<Member> members = manager.findMembers(namespace, SearchQuery.of(Pageable.unpaged()));

		model.addAttribute("namespace", namespace);
		model.addAttribute("members", members);
		model.addAttribute("invitationForm", InvitationForm.empty());

		return new ModelAndView("namespaces/members", model.asMap());
	}

	/**
	 * Request mapping that would handle the POST request that would update the {@link Member members}
	 * {@link NamespaceRole} in the {@link Namespace}.
	 *
	 * @param slug namespace name slug, can't be {@link null}
	 * @param member identity identifier of the member to be updated, can't be {@link null}
	 * @param role new namespace role to be assigned to the member, can't be {@link null}
	 * @param redirectAttributes redirect attributes that should contain the success message, can't be {@link null}
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
	 * @param slug namespace name slug, can't be {@link null}
	 * @param member identity identifier of the member to be removed, can't be {@link null}
	 * @param redirectAttributes redirect attributes that should contain the success message, can't be {@link null}
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
	 * @param slug namespace name slug, can't be {@link null}
	 * @param redirectAttributes redirect attributes that should contain the success message, can't be {@link null}
	 * @param sender currently logged-in user account, can't be {@link null}
	 * @param invitationForm the invitation form data, can't be {@link null}
	 * @param errors Spring MVC errors container, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
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
			model.addAttribute("members", manager.findMembers(namespace, SearchQuery.of(Pageable.unpaged())));

			return new ModelAndView("namespaces/members", model.asMap());
		}

		final Invite invite = new Invite(namespace.id(), sender.getId(), invitationForm.email(), invitationForm.role);

		try {
			invitations.create(invite);
		} catch (InvitationException ex) {
			errors.addError(new ObjectError(errors.getObjectName(), messageFor(ex)));

			model.addAttribute("namespace", namespace);
			model.addAttribute("members", manager.findMembers(namespace, SearchQuery.of(Pageable.unpaged())));

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
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
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
	 * @param slug namespace name slug, can't be {@link null}
	 * @param key  invitation key, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
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
	 * @param slug namespace name slug, can't be {@link null}
	 * @param key  invitation key, can't be {@link null}
	 * @param principal currently logged-in user account, can't be {@link null}
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
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}/repositories")
	ModelAndView repositories(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		return namespace(slug, model);
	}

	/**
	 * Request mapping that would render the {@link Namespace} applications page.
	 *
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}/applications")
	ModelAndView vaults(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		return namespace(slug, model);
	}

	/**
	 * Request mapping that would render the {@link Namespace} settings page.
	 *
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}/settings")
	ModelAndView settings(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		return namespace(slug, model);
	}

	/**
	 * Request mapping that would perform a check if there are any {@link Namespace Namespaces}
	 * with the given name, or slug, present in the system.
	 *
	 * @param value namespace name value, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
	 * @return <code>namespaces/check-name</code> template
	 */
	@PostMapping("/namespaces/check-name")
	ModelAndView provision(@RequestParam("value") @NonNull String value, @NonNull Model model) {
		String slug;

		try {
			slug = Slug.slugify(value).get();
		} catch (IllegalArgumentException e) {
			slug = value;
		}

		final boolean unavailable = manager.exists(slug);
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

}
