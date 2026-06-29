package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a namespace owner cannot be resolved from the supplied slug or identifier.
 *
 * @author Vitalii Kushnir
 * @since 1.0.0
 **/
public class OwnerNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	public OwnerNotFoundException(@NonNull String slug) {
		super(HttpStatus.NOT_FOUND, "Could not find an owner with the following name: " + slug);
	}

	public OwnerNotFoundException(@NonNull HttpStatus status, @NonNull EntityId id) {
		super(status, "Could not find an owner with the following identifier: " + id);
	}

}
