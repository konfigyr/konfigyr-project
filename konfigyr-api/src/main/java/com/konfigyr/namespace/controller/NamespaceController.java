package com.konfigyr.namespace.controller;

import com.google.crypto.tink.subtle.Base64;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

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
		final Page<@NonNull Namespace> result;
		final Optional<EntityId> account = retrieveAccountIdentifier(authentication);
		final Optional<EntityId> namespace = retrieveNamespaceIdentifier(authentication);

		// TODO: Improve how we retrieve namespaces based on the current authentication
		if (account.isPresent()) {
			final SearchQuery query = SearchQuery.builder()
					.criteria(SearchQuery.ACCOUNT, account.get())
					.pageable(pageable)
					.term(term)
					.build();

			result = namespaces.search(query);
		} else if (namespace.isPresent()) {
			result = namespaces.findById(namespace.get())
							.map(it -> (Page<@NonNull Namespace>) new PageImpl<>(List.of(it), pageable, 1))
							.orElseGet(() -> Page.empty(pageable));
		} else {
			throw new AuthenticationCredentialsNotFoundException(
					"Failed to retrieve account identifier from authentication principal"
			);
		}

		return assembler.assemble(result);
	}

	@PreAuthorize("isMember(#slug)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	@RequestMapping(path = "/{slug}", method = RequestMethod.HEAD)
	ResponseEntity<@NonNull Void> check(@PathVariable String slug) {
		final HttpStatus status = namespaces.exists(slug) ? HttpStatus.OK : HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status).build();
	}

	@GetMapping("/{slug}")
	@PreAuthorize("isMember(#slug)")
	@RequiresScope(OAuthScope.READ_NAMESPACES)
	EntityModel<Namespace> get(@PathVariable String slug) {
		final SearchQuery query = SearchQuery.builder()
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

	static Optional<EntityId> retrieveAccountIdentifier(@NonNull Authentication authentication) {
		try {
			return Optional.of(EntityId.from(authentication.getName()));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	static Optional<EntityId> retrieveNamespaceIdentifier(@NonNull Authentication authentication) {
		if (!authentication.getName().startsWith("kfg-")) {
			return Optional.empty();
		}

		try {
			final byte[] decoded = Base64.urlSafeDecode(authentication.getName().replace("kfg-", ""));
			final ByteBuffer buffer = ByteBuffer.wrap(decoded);

			return Optional.of(EntityId.from(buffer.getLong()));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	record NamespaceAttributes(
			@NotBlank @Length(min = 5, max = 30) String slug,
			@NotBlank @Length(min = 3, max = 30) String name,
			@Length(max = 255) String description
	) {
		NamespaceDefinition definition(@NonNull Authentication authentication) {
			final EntityId owner = retrieveAccountIdentifier(authentication).orElseThrow(
					() -> new AuthenticationCredentialsNotFoundException(
							"Failed to retrieve account identifier from authentication principal"
					)
			);

			return NamespaceDefinition.builder()
					.owner(1L)
					.slug(slug)
					.name(name)
					.description(description)
					.owner(owner)
					.build();
		}
	}

}
