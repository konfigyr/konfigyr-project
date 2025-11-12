package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.io.Serial;

/**
 * Exception thrown when a {@link NamespaceApplication} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class NamespaceApplicationNotFoundException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	/**
	 * Create new instance of the {@link NamespaceApplicationNotFoundException} when there are no
	 * {@link NamespaceApplication Namespace OAuth2 applications} with the matching {@link EntityId identifier}.
	 *
	 * @param id namespace application entity identifier, can't be {@code null}
	 */
	public NamespaceApplicationNotFoundException(@NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find a namespace application with the following identifier: " + id.serialize());
	}

}
