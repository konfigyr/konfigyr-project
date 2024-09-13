package com.konfigyr.namespace;

import com.konfigyr.support.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller that handles the {@link Namespace} related request mappings.
 *
 * @author Vladimir Spasic
 **/
@Controller
@RequiredArgsConstructor
public class NamespaceController {

	private final NamespaceManager manager;

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

		model.addAttribute("namespace", namespace);

		return new ModelAndView("namespaces/details", model.asMap());
	}

	/**
	 * Request mapping that would render the {@link Namespace} {@link Member members} page.
	 *
	 * @param slug namespace name slug, can't be {@link null}
	 * @param model Spring MVC model, can't be {@link null}
	 * @return <code>namespaces/details</code> template
	 */
	@GetMapping("/namespace/{namespace}/members")
	ModelAndView members(@PathVariable("namespace") @NonNull String slug, @NonNull Model model) {
		return namespace(slug, model);
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

}
