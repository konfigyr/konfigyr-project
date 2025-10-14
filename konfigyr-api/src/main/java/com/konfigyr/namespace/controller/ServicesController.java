package com.konfigyr.namespace.controller;

import com.konfigyr.hateoas.EntityModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.*;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.oauth.RequiresScope;
import com.konfigyr.support.SearchQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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
	ResponseEntity<Void> check(@PathVariable @NonNull String namespace, @PathVariable @NonNull String slug) {
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
