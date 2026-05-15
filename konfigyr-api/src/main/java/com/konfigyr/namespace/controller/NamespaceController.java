package com.konfigyr.namespace.controller;

import com.google.crypto.tink.subtle.Base64;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.hateoas.RepresentationModelAssembler;
import com.konfigyr.namespace.*;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.PrincipalType;
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
	PagedModel<?> search(@Nullable @RequestParam(required = false) String term, @NonNull Pageable pageable) {
		final Page<@NonNull Namespace> result;
		final Optional<EntityId> account = retrieveAccountIdentifier();
		final Optional<EntityId> namespace = retrieveNamespaceIdentifier();

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
			@RequestBody @Validated NamespaceAttributes attributes
	) {
		return assembler.assemble(namespaces.create(attributes.definition()));
	}

	@PutMapping("/{slug}")
	@PreAuthorize("isAdmin(#slug)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<Namespace> update(
			@PathVariable String slug,
			@RequestBody @Validated NamespaceAttributes attributes
	) {
		return assembler.assemble(namespaces.update(slug, attributes.definition()));
	}

	@DeleteMapping("/{slug}")
	@PreAuthorize("isAdmin(#slug)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.DELETE_NAMESPACES)
	void delete(@PathVariable String slug) {
		namespaces.delete(slug);
	}

	static Optional<EntityId> retrieveAccountIdentifier() {
		return AuthenticatedPrincipal.fromSecurityContext()
				.filter(principal -> PrincipalType.USER_ACCOUNT.equals(principal.getType()))
				.map(AuthenticatedPrincipal::get)
				.map(subject -> {
					try {
						return EntityId.from(subject);
					} catch (IllegalArgumentException ignore) {
						return null;
					}
				});
	}

	static Optional<EntityId> retrieveNamespaceIdentifier() {
		return AuthenticatedPrincipal.fromSecurityContext()
				.filter(principal -> PrincipalType.OAUTH_CLIENT.equals(principal.getType()))
				.map(AuthenticatedPrincipal::get)
				.map(subject -> {
					try {
						final byte[] decoded = Base64.urlSafeDecode(subject.replace("kfg-", ""));
						final ByteBuffer buffer = ByteBuffer.wrap(decoded);
						return EntityId.from(buffer.getLong());
					} catch (Exception ignore) {
						return null;
					}
				});
	}

	record NamespaceAttributes(
			@NotBlank @Length(min = 5, max = 30) String slug,
			@NotBlank @Length(min = 3, max = 30) String name,
			@Length(max = 255) String description
	) {
		NamespaceDefinition definition() {
			final EntityId owner = retrieveAccountIdentifier().orElseThrow(
					() -> new AuthenticationCredentialsNotFoundException(
							"Failed to retrieve account identifier from authenticated principal"
					)
			);

			return NamespaceDefinition.builder()
					.slug(slug)
					.name(name)
					.description(description)
					.owner(owner)
					.build();
		}
	}

}
