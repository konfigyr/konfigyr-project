package com.konfigyr.namespace.controller;

import com.konfigyr.artifactory.Manifest;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.artifactory.ServiceRelease;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.*;
import com.konfigyr.namespace.manifest.ReleaseNotFoundException;
import com.konfigyr.namespace.manifest.ServiceManifests;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequiresScope(OAuthScope.READ_NAMESPACES)
@RequestMapping("/namespaces/{namespace}/services")
class ServicesController {

	private final Services services;
	private final NamespaceManager namespaces;
	private final ServiceManifests manifests;

	@GetMapping
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<Service>> find(@PathVariable @NonNull String namespace, Pageable pageable) {
		final Namespace ns = lookupNamespace(namespace);

		final SearchQuery query = SearchQuery.builder()
				.pageable(pageable)
				.build();

		return Assemblers.service(ns).assemble(services.find(ns, query));
	}

	@GetMapping("{slug}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<Service> get(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
		final Namespace ns = lookupNamespace(namespace);

		return Assemblers.service(ns).assemble(services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		));
	}

	@PreAuthorize("isMember(#namespace)")
	@RequestMapping(path = "{slug}", method = RequestMethod.HEAD)
	ResponseEntity<@NonNull Void> check(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
		final HttpStatus status = namespaces.findBySlug(namespace)
				.map(it -> services.exists(it, slug))
				.filter(Boolean.TRUE::equals)
				.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND;

		return ResponseEntity.status(status).build();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("isMember(#namespace)")
	EntityModel<Service> create(
			@PathVariable @NonNull String namespace,
			@RequestBody @Validated @NonNull ServiceAttributes attributes
	) {
		final Namespace ns = lookupNamespace(namespace);
		final ServiceDefinition definition = attributes.definition(ns);

		return Assemblers.service(ns).assemble(services.create(definition));
	}

	@PutMapping("{slug}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<Service> update(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull String slug,
			@RequestBody @Validated @NonNull ServiceAttributes attributes
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		return Assemblers.service(ns).assemble(services.update(service.id(), attributes.definition(ns)));
	}

	@DeleteMapping("{slug}")
	@PreAuthorize("isAdmin(#namespace)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
		services.delete(lookupNamespace(namespace), slug);
	}

	@GetMapping("{slug}/manifest")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<Manifest> manifest(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		return Assemblers.manifest(ns, service).assemble(manifests.get(service));
	}

	@GetMapping("{slug}/releases/{id}")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<ServiceRelease> release(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull String slug,
			@PathVariable EntityId id
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		final ServiceRelease release = manifests.get(service, id).orElseThrow(
				() -> new ReleaseNotFoundException(id)
		);

		return Assemblers.release(ns, service).assemble(release);
	}

	@GetMapping("{slug}/catalog")
	@PreAuthorize("isMember(#namespace)")
	EntityModel<ServiceCatalog> catalog(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
		final Namespace ns = lookupNamespace(namespace);

		return Assemblers.catalog(ns).assemble(services.catalog(ns, slug));
	}

	@GetMapping("{slug}/catalog/search")
	@PreAuthorize("isMember(#namespace)")
	PagedModel<EntityModel<PropertyDescriptor>> search(
			@PathVariable @NonNull String namespace,
			@PathVariable @NonNull String slug,
			@RequestParam(required = false) String term,
			Pageable pageable
	) {
		final Namespace ns = lookupNamespace(namespace);
		final Service service = services.get(ns, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace, slug)
		);

		final SearchQuery.Builder builder = SearchQuery.builder().pageable(pageable);
		if (term != null) {
			builder.term(term);
		}

		return Assemblers.property(ns, service).assemble(services.search(service, builder.build()));
	}

	@NonNull
	Namespace lookupNamespace(@NonNull String slug) {
		return namespaces.findBySlug(slug).orElseThrow(() -> new NamespaceNotFoundException(slug));
	}

	record ServiceAttributes(
			@NotBlank @Length(min = 5, max = 30) String slug,
			@NotBlank @Length(min = 3, max = 30) String name,
			@Length(max = 255) String description
	) {
		ServiceDefinition definition(Namespace namespace) {
			return ServiceDefinition.builder()
					.namespace(namespace.id())
					.slug(slug)
					.name(name)
					.description(description)
					.build();
		}
	}
}
