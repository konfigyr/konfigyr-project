package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Exception thrown when a {@link Namespace} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class NamespaceNotFoundException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	/**
	 * Create new instance of the {@link NamespaceNotFoundException} when there are no
	 * {@link Namespace Namespaces} with the matching {@link com.konfigyr.support.Slug slug}.
	 *
	 * @param slug namespace name slug, can't be {@code null}
	 */
	public NamespaceNotFoundException(@NonNull String slug) {
		super("Could not find a namespace with the following name: " + slug);
	}

	/**
	 * Create new instance of the {@link NamespaceNotFoundException} when there are no
	 * {@link Namespace Namespaces} with the matching {@link EntityId entity identifier}.
	 *
	 * @param id namespace entity identifier, can't be {@code null}
	 */
	public NamespaceNotFoundException(@NonNull EntityId id) {
		super("Could not find a namespace with the following identifier: " + id.serialize());
	}

}
