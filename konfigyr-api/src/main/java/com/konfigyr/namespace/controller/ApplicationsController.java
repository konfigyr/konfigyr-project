package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.READ_NAMESPACES)
@RequestMapping("/namespaces/{slug}/applications")
class ApplicationsController {

	private final NamespaceManager namespaces;

	@GetMapping
	@PreAuthorize("isAdmin(#slug)")
	PagedModel<EntityModel<NamespaceApplication>> find(
			@PathVariable @NonNull String slug,
			@RequestParam(required = false) @Nullable String term,
			@RequestParam(required = false) @Nullable Boolean active,
			@NonNull Pageable pageable
	) {
		final Namespace namespace = lookupNamespace(slug);
		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.criteria(SearchQuery.NAMESPACE, namespace.slug())
				.criteria(NamespaceApplication.ACTIVE_CRITERIA, active)
				.pageable(pageable)
				.build();

		return Assemblers.application(namespace).assemble(
				namespaces.findApplications(query)
		);
	}

	@GetMapping("{id}")
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<NamespaceApplication> get(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId id) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.application(lookupNamespace(slug)).assemble(
				lookupNamespaceApplication(namespace, id)
		);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<NamespaceApplication> create(
			@PathVariable @NonNull String slug,
			@Validated @RequestBody CreateApplicationAttributes attributes
	) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.application(namespace).assemble(
				namespaces.createApplication(attributes.create(namespace))
		);
	}

	@PutMapping("{id}")
	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<NamespaceApplication> update(
			@PathVariable @NonNull String slug,
			@PathVariable @NonNull EntityId id,
			@Validated @RequestBody CreateApplicationAttributes attributes
	) {
		final Namespace namespace = lookupNamespace(slug);
		final NamespaceApplication application = lookupNamespaceApplication(namespace, id);

		return Assemblers.application(namespace).assemble(
				namespaces.updateApplication(application.id(), attributes.create(namespace))
		);
	}

	@PutMapping("{id}/reset")
	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<NamespaceApplication> reset(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId id) {
		final Namespace namespace = lookupNamespace(slug);
		final NamespaceApplication application = lookupNamespaceApplication(namespace, id);

		return Assemblers.application(namespace).assemble(
				namespaces.resetApplication(application.id())
		);
	}

	@DeleteMapping("{id}")
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	void delete(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId id) {
		final NamespaceApplication application = lookupNamespaceApplication(lookupNamespace(slug), id);

		namespaces.removeApplication(application.id());
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	NamespaceApplication lookupNamespaceApplication(@NonNull Namespace namespace, @NonNull EntityId id) {
		final SearchQuery query = SearchQuery.builder()
				.criteria(SearchQuery.NAMESPACE, namespace.slug())
				.criteria(NamespaceApplication.ID_CRITERIA, id)
				.build();

		final Page<NamespaceApplication> applications = namespaces.findApplications(query);

		if (applications.isEmpty()) {
			throw new NamespaceApplicationNotFoundException(id);
		}

		if (applications.getTotalElements() > 1) {
			throw new IllegalStateException("Multiple OAuth namespace applications found for id: " + id);
		}

		return applications.iterator().next();
	}

	record CreateApplicationAttributes(
			@NotBlank String name,
			@NotNull OAuthScopes scopes,
			@Future OffsetDateTime expiration
	) {

		NamespaceApplicationDefinition create(Namespace namespace) {
			return NamespaceApplicationDefinition.builder()
					.namespace(namespace.id())
					.name(name())
					.scopes(scopes())
					.expiration(expiration)
					.build();
		}

	}

}
