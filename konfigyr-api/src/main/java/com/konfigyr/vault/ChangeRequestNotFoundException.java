package com.konfigyr.vault;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link ChangeRequest} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ChangeRequestNotFoundException extends VaultException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	/**
	 * Create new instance of the {@link ChangeRequestNotFoundException} when there are no
	 * {@link ChangeRequest change requests} within a given {@link Service} with the matching
	 * number.
	 *
	 * @param service the service that owns the change request, can't be {@code null}
	 * @param number change request number, can't be {@code null}
	 */
	public ChangeRequestNotFoundException(@NonNull Service service, Long number) {
		super(HttpStatus.NOT_FOUND, "Could not find a change request with the following number: %s within a %s service"
				.formatted(number, service.slug()));
	}

	/**
	 * Create new instance of the {@link ChangeRequestNotFoundException} when there is no
	 * {@link ChangeRequest change requests} with the matching {@link EntityId entity identifier}.
	 *
	 * @param id change request entity identifier, can't be {@code null}
	 */
	public ChangeRequestNotFoundException(@NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find a change request with the following identifier: " + id.serialize());
	}

}
