package com.konfigyr.kms.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.*;
import com.konfigyr.kms.*;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.READ_NAMESPACES)
@RequestMapping("/namespaces/{namespace}/kms")
class KmsController {

	private final KeysetManager manager;
	private final NamespaceManager namespaces;

	@GetMapping
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<KeysetMetadata>> find(
			@PathVariable @NonNull String namespace,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) String algorithm,
			@RequestParam(required = false) KeysetMetadataState state,
			Pageable pageable
	) {
		final Namespace ns = lookupNamespace(namespace);

		final SearchQuery query = SearchQuery.builder()
				.criteria(SearchQuery.NAMESPACE, ns.slug())
				.criteria(KeysetMetadata.STATE_CRITERIA, state)
				.criteria(KeysetMetadata.ALGORITHM_CRITERIA, algorithm)
				.pageable(pageable)
				.term(term)
				.build();

		return assemble(ns).assemble(manager.find(query));
	}

	@GetMapping("{id}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<KeysetMetadata> get(@PathVariable @NonNull String namespace, @PathVariable @NonNull EntityId id) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);

		return assemble(ns).assemble(keyset);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> create(@PathVariable @NonNull String namespace, @RequestBody @Validated KeysetAttributes attributes) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = manager.create(attributes.toDefinition(ns));

		return assemble(ns).assemble(keyset);
	}

	@PatchMapping("{id}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> update(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull EntityId id,
			@RequestBody ObjectNode body
	) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);
		final KeysetPatch patch = KeysetPatch.from(keyset, body);

		return assemble(ns).assemble(manager.update(keyset.id(), patch.description(), patch.tags()));
	}

	@PutMapping("{id}/deactivate")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> deactivate(@PathVariable @NonNull String namespace, @PathVariable @NonNull EntityId id) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);

		return assemble(ns).assemble(manager.transition(keyset.id(), KeysetMetadataState.INACTIVE));
	}

	@DeleteMapping("{id}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> destroy(@PathVariable @NonNull String namespace, @PathVariable @NonNull EntityId id) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);

		return assemble(ns).assemble(manager.transition(keyset.id(), KeysetMetadataState.PENDING_DESTRUCTION));
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	@NonNull
	KeysetMetadata lookupKeysetMetadata(@NonNull Namespace namespace, @NonNull EntityId id) {
		return manager.get(namespace.id(), id).orElseThrow(() -> new KeysetNotFoundException(id));
	}

	static RepresentationModelAssembler<KeysetMetadata, EntityModel<KeysetMetadata>> assemble(Namespace namespace) {
		return keyset -> EntityModel.of(keyset, linkBuilder(namespace, keyset).selfRel())
				.add(linkBuilder(namespace, keyset).method(HttpMethod.PATCH).rel("update"))
				.add(linkBuilder(namespace, keyset).method(HttpMethod.DELETE).rel("destroy"))
				.add(linkBuilder(namespace, keyset).path("deactivate").method(HttpMethod.PUT).rel("deactivate"))
				.add(linkBuilder(namespace, keyset).path("encrypt").method(HttpMethod.PUT).rel("encrypt data"))
				.add(linkBuilder(namespace, keyset).path("decrypt").method(HttpMethod.PUT).rel("decrypt data"))
				.add(linkBuilder(namespace, keyset).path("sign").method(HttpMethod.PUT).rel("sign data"))
				.add(linkBuilder(namespace, keyset).path("verify").method(HttpMethod.PUT).rel("verify signature"));
	}

	static LinkBuilder linkBuilder(Namespace namespace, KeysetMetadata keyset) {
		return Link.builder()
				.path("namespaces")
				.path(namespace.slug())
				.path("kms")
				.path(keyset.id().serialize());
	}

	record KeysetAttributes(
			@NotNull KeysetMetadataAlgorithm algorithm,
			@NotBlank String name,
			String description,
			Set<String> tags
	) {

		KeysetMetadataDefinition toDefinition(Namespace namespace) {
			return KeysetMetadataDefinition.builder()
					.namespace(namespace.id())
					.algorithm(algorithm)
					.name(name)
					.description(description)
					.tags(tags)
					.build();
		}

	}

	record KeysetPatch(String description, Set<String> tags) {

		static KeysetPatch from(KeysetMetadata keyset, ObjectNode payload) {
			String description = keyset.description();
			Set<String> tags = keyset.tags();

			if (payload.has("description")) {
				final JsonNode node = payload.get("description");
				description = node.isString() ? node.asString() : null;
			}

			if (payload.has("tags")) {
				final JsonNode node = payload.get("tags");

				if (node.isArray()) {
					tags = new LinkedHashSet<>();

					for (JsonNode tag : node) {
						tags.add(tag.asString());
					}
				} else {
					tags = null;
				}
			}

			return new KeysetPatch(description, tags);
		}

	}

}
