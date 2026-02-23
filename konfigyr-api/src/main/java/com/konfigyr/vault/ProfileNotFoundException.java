package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link Profile} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ProfileNotFoundException extends VaultException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	/**
	 * Create new instance of the {@link ProfileNotFoundException} when there are no {@link Profile profiles}
	 * within a given {@link com.konfigyr.namespace.Service service} with the matching name.
	 *
	 * @param service service name slug, can't be {@code null}
	 * @param name profile name, can't be {@code null}
	 */
	public ProfileNotFoundException(@NonNull String service, @NonNull String name) {
		super(HttpStatus.NOT_FOUND, "Could not find a profile with the following name: %s within a %s Service"
				.formatted(name, service));
	}

	/**
	 * Create new instance of the {@link ProfileNotFoundException} when there is no {@link Profile profile}
	 * with the matching {@link EntityId entity identifier}.
	 *
	 * @param id profile entity identifier, can't be {@code null}
	 */
	public ProfileNotFoundException(@NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find a profile with the following identifier: " + id.serialize());
	}

}
