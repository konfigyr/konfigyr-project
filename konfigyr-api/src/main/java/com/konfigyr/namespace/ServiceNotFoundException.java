package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * Exception thrown when a {@link Service} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ServiceNotFoundException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	/**
	 * Create new instance of the {@link ServiceNotFoundException} when there are no
	 * {@link Service services} within a given {@link Namespace Namespaces} with the
	 * matching {@link com.konfigyr.support.Slug slug}.
	 *
	 * @param namespace namespace name slug, can't be {@code null}
	 * @param slug service name slug, can't be {@code null}
	 */
	public ServiceNotFoundException(@NonNull String namespace, @NonNull String slug) {
		this("Could not find a service with the following name: %s within a %s Namespace".formatted(slug, namespace));
	}

	/**
	 * Create new instance of the {@link ServiceNotFoundException} when there are no
	 * {@link Service service} with the matching {@link EntityId entity identifier}.
	 *
	 * @param id service entity identifier, can't be {@code null}
	 */
	public ServiceNotFoundException(@NonNull EntityId id) {
		this("Could not find a service with the following identifier: " + id.serialize());
	}

	/**
	 * Create new instance of the {@link ServiceNotFoundException} with a custom error message.
	 *
	 * @param message custom error message
	 */
	public ServiceNotFoundException(@NonNull String message) {
		super(HttpStatus.NOT_FOUND, message);
	}

}
