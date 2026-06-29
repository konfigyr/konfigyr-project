package com.konfigyr.namespace;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.artifactory.OwnerNotFoundException;
import com.konfigyr.artifactory.OwnerResolver;
import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@RequiredArgsConstructor
final class NamespaceOwnerResolver implements OwnerResolver {

	private final NamespaceManager manager;

	@Override
	public Owner resolve(EntityId id) {
		final Namespace namespace = manager.findById(id)
				.orElseThrow(() -> new OwnerNotFoundException(id));
		return new Owner(namespace.id(), namespace.slug());
	}

	@Override
	public Owner resolve(String slug) {
		final Namespace namespace = manager.findBySlug(slug)
				.orElseThrow(() -> new OwnerNotFoundException(slug));
		return new Owner(namespace.id(), namespace.slug());
	}

}
