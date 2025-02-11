package com.konfigyr.namespace.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/namespaces")
class NamespaceController {

	private final NamespaceManager namespaces;
	private final RepresentationModelAssembler<Namespace, EntityModel<Namespace>> assembler = Assemblers.namespace();

	@GetMapping
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	PagedModel<?> search(
			@Nullable @RequestParam(required = false) String term,
			@NonNull Authentication authentication,
			@NonNull Pageable pageable
	) {
		final SearchQuery query = SearchQuery.builder()
				.criteria(SearchQuery.ACCOUNT, retrieveAccountIdentifier(authentication))
				.pageable(pageable)
				.term(term)
				.build();

		return assembler.assemble(namespaces.search(query));
	}

	@GetMapping("/{slug}")
	@PreAuthorize("isMember(#slug)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	EntityModel<Namespace> get(@PathVariable String slug, @NonNull Authentication authentication) {
		final SearchQuery query = SearchQuery.builder()
				.criteria(SearchQuery.ACCOUNT, retrieveAccountIdentifier(authentication))
				.criteria(SearchQuery.NAMESPACE, slug)
				.pageable(Pageable.ofSize(1))
				.build();

		return namespaces.search(query).stream()
				.findFirst()
				.map(assembler::assemble)
				.orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<Namespace> create(
			@RequestBody @Validated NamespaceAttributes attributes,
			@NonNull Authentication authentication
	) {
		return assembler.assemble(namespaces.create(attributes.definition(authentication)));
	}

	@PutMapping("/{slug}")
	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<Namespace> update(
			@PathVariable String slug,
			@RequestBody @Validated NamespaceAttributes attributes,
			@NonNull Authentication authentication
	) {
		return assembler.assemble(namespaces.update(slug, attributes.definition(authentication)));
	}

	@DeleteMapping("/{slug}")
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.DELETE_NAMESPACES)
	void delete(@PathVariable String slug) {
		namespaces.delete(slug);
	}

	static EntityId retrieveAccountIdentifier(@NonNull Authentication authentication) {
		try {
			return EntityId.from(authentication.getName());
		} catch (IllegalArgumentException ex) {
			throw new AuthenticationCredentialsNotFoundException(
					"Failed to retrieve account identifier from authentication principal", ex
			);
		}
	}

	record NamespaceAttributes(
			@NotNull NamespaceType type,
			@NotBlank @Length(min = 5, max = 30) String slug,
			@NotBlank @Length(min = 3, max = 30) String name,
			@Length(max = 255) String description
	) {
		NamespaceDefinition definition(@NonNull Authentication authentication) {
			return NamespaceDefinition.builder()
					.owner(1L)
					.slug(slug)
					.name(name)
					.type(type)
					.description(description)
					.owner(retrieveAccountIdentifier(authentication))
					.build();
		}
	}

}
