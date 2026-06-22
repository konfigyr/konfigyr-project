package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.ArtifactoryException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link Owner} does not exist.
 *
 * @author Vitalii Kushnir
 **/
public class OwnerNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -30756960861487345L;

	public OwnerNotFoundException(@NonNull String slug) {
		super(HttpStatus.NOT_FOUND, "Could not find an owner with the following name: " + slug);
	}

}
