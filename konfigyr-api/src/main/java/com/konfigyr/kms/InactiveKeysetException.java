package com.konfigyr.kms;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when there was an attempt to perform an operation upon the {@link KeysetMetadata}
 * that is not in an {@link KeysetMetadataState#ACTIVE active state}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public class InactiveKeysetException extends KeysetManagementException {

	@Serial
	private static final long serialVersionUID = 3024070458455989992L;

	/**
     * Creates a new {@link InactiveKeysetException} for the given {@link EntityId entity identifier}.
	 *
	 * @param id the identifier of the keyset that was inactive, cannot be {@literal null}.
	 */
	public InactiveKeysetException(EntityId id) {
		super(HttpStatus.CONFLICT, "Could not perform an operation on an inactive keyset with following identifier: " + id);
	}

}
