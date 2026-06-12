package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.READ_NAMESPACES)
@RequestMapping("/namespaces/{slug}/trusted-issuers")
class TrustedIssuersController {

	private final NamespaceManager namespaces;

	@GetMapping
	@PreAuthorize("isAdmin(#slug)")
	PagedModel<EntityModel<NamespaceTrustedIssuer>> find(
			@PathVariable @NonNull String slug,
			@RequestParam(required = false) @Nullable String term,
			@RequestParam(required = false) @Nullable Boolean active,
			@NonNull Pageable pageable
	) {
		final Namespace namespace = lookupNamespace(slug);
		final SearchQuery query = SearchQuery.builder()
				.term(term)
				.criteria(NamespaceTrustedIssuer.ACTIVE_CRITERIA, active)
				.pageable(pageable)
				.build();

		return Assemblers.trustedIssuer(namespace).assemble(
				namespaces.findTrustedIssuers(namespace, query)
		);
	}

	@GetMapping("{id}")
	@PreAuthorize("isAdmin(#slug)")
	EntityModel<NamespaceTrustedIssuer> get(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId id) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.trustedIssuer(namespace).assemble(
				namespaces.getTrustedIssuer(namespace, id)
						.orElseThrow(() -> new NamespaceTrustedIssuerNotFoundException(id))
		);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<NamespaceTrustedIssuer> create(
			@PathVariable @NonNull String slug,
			@Validated @RequestBody TrustedIssuerAttributes attributes
	) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.trustedIssuer(namespace).assemble(
				namespaces.createTrustedIssuer(namespace, attributes.toDefinition())
		);
	}

	@PutMapping("{id}")
	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<NamespaceTrustedIssuer> update(
			@PathVariable @NonNull String slug,
			@PathVariable @NonNull EntityId id,
			@Validated @RequestBody TrustedIssuerAttributes attributes
	) {
		final Namespace namespace = lookupNamespace(slug);

		return Assemblers.trustedIssuer(namespace).assemble(
				namespaces.updateTrustedIssuer(namespace, id, attributes.toDefinition())
		);
	}

	@DeleteMapping("{id}")
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	void delete(@PathVariable @NonNull String slug, @PathVariable @NonNull EntityId id) {
		final Namespace namespace = lookupNamespace(slug);
		namespaces.removeTrustedIssuer(namespace, id);
	}

	@NonNull
	private Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	record TrustedIssuerAttributes(
			@NotBlank @Length(min = 3, max = 30) String name,
			@Nullable @Length(max = 255) String description,
			@NotBlank @URL String issuerUri,
			@Nullable @URL String jwksUri,
			@Nullable @Size(max = 10) List<@NotBlank String> allowedAudiences,
			@Nullable @Size(max = 20) Map<@NotBlank String, @NotBlank String> customClaims
	) {

		NamespaceTrustedIssuerDefinition toDefinition() {
			return NamespaceTrustedIssuerDefinition.builder()
					.name(name())
					.description(description())
					.issuerUri(issuerUri())
					.jwksUri(jwksUri())
					.allowedAudiences(allowedAudiences())
					.customClaims(customClaims())
					.build();
		}

	}

}
