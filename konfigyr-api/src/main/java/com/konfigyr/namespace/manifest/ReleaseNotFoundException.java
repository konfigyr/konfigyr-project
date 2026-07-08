package com.konfigyr.namespace.manifest;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link ServiceManifests} operation is attempted for a release that does
 * not exist for the given service, either because the identifier is unknown or because it identifies
 * a release belonging to a different service.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ReleaseNotFoundException extends ServiceManifestException {

	@Serial
	private static final long serialVersionUID = -3187004821552018403L;

	private final EntityId releaseId;

	/**
	 * Create new instance of the {@link ReleaseNotFoundException} for the given release identifier.
	 *
	 * @param releaseId the entity identifier of the release that could not be found, can't be {@literal null}
	 */
	public ReleaseNotFoundException(@NonNull EntityId releaseId) {
		super(HttpStatus.NOT_FOUND, "Could not find a release with the following identifier: " + releaseId.serialize());
		this.releaseId = releaseId;
	}

	@Override
	public Object @Nullable [] getDetailMessageArguments() {
		return new Object[] { releaseId.serialize() };
	}

	@NonNull
	public EntityId getReleaseId() {
		return releaseId;
	}

}
