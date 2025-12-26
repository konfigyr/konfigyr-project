package com.konfigyr.kms;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to retrieve {@link KeysetMetadata} from the KMS that does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public class KeysetNotFoundException extends KeysetManagementException {

	@Serial
	private static final long serialVersionUID = 918522905298460950L;

	/**
     * Creates a new {@link KeysetNotFoundException} for the given {@link EntityId entity identifier}.
	 *
	 * @param id the identifier of the keyset that was not found, cannot be {@literal null}.
	 */
	public KeysetNotFoundException(EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find keyset metadata with the following identifier: " + id.serialize());
	}

}
