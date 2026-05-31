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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashSet;
import java.util.Set;

@NullMarked
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
			@PathVariable String namespace,
			@Nullable @RequestParam(required = false) String term,
			@Nullable @RequestParam(required = false) KeysetMetadataAlgorithm algorithm,
			@Nullable @RequestParam(required = false) KeysetMetadataState state,
			Pageable pageable
	) {
		final Namespace ns = lookupNamespace(namespace);

		final SearchQuery query = SearchQuery.builder()
				.criteria(KeysetMetadata.STATE_CRITERIA, state)
				.criteria(KeysetMetadata.ALGORITHM_CRITERIA, algorithm)
				.pageable(pageable)
				.term(term)
				.build();

		return assemble(ns).assemble(manager.find(ns, query));
	}

	@GetMapping("{id}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<KeysetMetadata> get(@PathVariable String namespace, @PathVariable EntityId id) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);

		return assemble(ns).assemble(keyset);
	}

	@PostMapping
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> create(@PathVariable String namespace, @RequestBody @Validated KeysetAttributes attributes) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = manager.create(ns, attributes.toDefinition());

		return assemble(ns).assemble(keyset);
	}

	@PatchMapping("{id}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> update(
			@PathVariable String namespace,
			@PathVariable EntityId id,
			@RequestBody ObjectNode body
	) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);
		final KeysetPatch patch = KeysetPatch.from(keyset, body);

		return assemble(ns).assemble(manager.update(keyset, patch.description(), patch.tags()));
	}

	@PutMapping("{id}/rotate")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> rotate(@PathVariable String namespace, @PathVariable EntityId id, @RequestBody KeysetRotate rotate) {
		final Namespace ns = lookupNamespace(namespace);

		return assemble(ns).assemble(manager.rotate(ns, id, rotate.algorithm()));
	}

	@DeleteMapping("{id}")
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	void destroy(@PathVariable String namespace, @PathVariable EntityId id) {
		manager.delete(lookupNamespace(namespace), id);
	}

	@GetMapping("{id}/keys")
	@PreAuthorize("isAdmin(#namespace)")
	CollectionModel<EntityModel<KeyMetadata>> keys(@PathVariable String namespace, @PathVariable EntityId id) {
		final Namespace ns = lookupNamespace(namespace);
		final KeysetMetadata keyset = lookupKeysetMetadata(ns, id);

		return assemble(ns, keyset).assemble(manager.keys(keyset));
	}

	@PutMapping("{id}/keys/{key}/reactivate")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> reactivate(@PathVariable String namespace, @PathVariable EntityId id, @PathVariable String key) {
		final Namespace ns = lookupNamespace(namespace);

		return assemble(ns).assemble(manager.transition(ns, KeyOperation.reactivate(id, key)));
	}

	@PutMapping("{id}/keys/{key}/deactivate")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> deactivate(@PathVariable String namespace, @PathVariable EntityId id, @PathVariable String key) {
		final Namespace ns = lookupNamespace(namespace);

		return assemble(ns).assemble(manager.transition(ns, KeyOperation.deactivate(id, key)));
	}

	@PutMapping("{id}/keys/{key}/compromised")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> compromised(@PathVariable String namespace, @PathVariable EntityId id, @PathVariable String key) {
		final Namespace ns = lookupNamespace(namespace);

		return assemble(ns).assemble(manager.transition(ns, KeyOperation.compromise(id, key)));
	}

	@PutMapping("{id}/keys/{key}/restore")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> restore(@PathVariable String namespace, @PathVariable EntityId id, @PathVariable String key) {
		final Namespace ns = lookupNamespace(namespace);

		return assemble(ns).assemble(manager.transition(ns, KeyOperation.restore(id, key)));
	}

	@DeleteMapping("{id}/keys/{key}")
	@PreAuthorize("isAdmin(#namespace)")
	@RequiresScope(OAuthScope.WRITE_NAMESPACES)
	EntityModel<KeysetMetadata> delete(@PathVariable String namespace, @PathVariable EntityId id, @PathVariable String key) {
		final Namespace ns = lookupNamespace(namespace);

		return assemble(ns).assemble(manager.transition(ns, KeyOperation.destroy(id, key)));
	}

	Namespace lookupNamespace(String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	KeysetMetadata lookupKeysetMetadata(Namespace namespace, EntityId id) {
		return manager.get(namespace, id).orElseThrow(() -> new KeysetNotFoundException(id));
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

	static RepresentationModelAssembler<KeyMetadata, EntityModel<KeyMetadata>> assemble(Namespace namespace, KeysetMetadata keyset) {
		return key -> EntityModel.of(key, linkBuilder(namespace, keyset, key).selfRel())
				.add(linkBuilder(namespace, keyset, key).method(HttpMethod.PATCH).rel("update"))
				.add(linkBuilder(namespace, keyset, key).method(HttpMethod.DELETE).rel("destroy"))
				.add(linkBuilder(namespace, keyset, key).path("deactivate").method(HttpMethod.PUT).rel("deactivate"));
	}

	static LinkBuilder linkBuilder(Namespace namespace, KeysetMetadata keyset) {
		return Link.builder()
				.path("namespaces")
				.path(namespace.slug())
				.path("kms")
				.path(keyset.id().serialize());
	}

	static LinkBuilder linkBuilder(Namespace namespace, KeysetMetadata keyset, KeyMetadata key) {
		return Link.builder()
				.path("namespaces")
				.path(namespace.slug())
				.path("kms")
				.path(keyset.id().serialize())
				.path("keys")
				.path(key.id());
	}

	record KeysetAttributes(
			@Nullable @NotNull KeysetMetadataAlgorithm algorithm,
			@Nullable @NotBlank String name,
			@Nullable String description,
			@Nullable Set<String> tags
	) {

		KeysetMetadataDefinition toDefinition() {
			return KeysetMetadataDefinition.builder()
					.algorithm(algorithm)
					.name(name)
					.description(description)
					.tags(tags)
					.build();
		}

	}

	record KeysetRotate(@Nullable KeysetMetadataAlgorithm algorithm) {

	}

	record KeysetPatch(@Nullable String description, @Nullable Set<String> tags) {

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
