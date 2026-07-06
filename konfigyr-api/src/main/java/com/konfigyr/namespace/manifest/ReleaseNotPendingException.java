package com.konfigyr.namespace.manifest;

import com.konfigyr.artifactory.ReleaseState;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when artifact metadata is uploaded via {@link ServiceManifests#upload} for a
 * release that is not currently {@link ReleaseState#PENDING}. A new build must be started through
 * {@link ServiceManifests#open} before any further metadata can be uploaded.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class ReleaseNotPendingException extends ServiceManifestException {

	@Serial
	private static final long serialVersionUID = 8014237154920064582L;

	private final EntityId releaseId;
	private final ReleaseState state;

	/**
	 * Create new instance of the {@link ReleaseNotPendingException} for the given release and its
	 * current, non-{@link ReleaseState#PENDING} state.
	 *
	 * @param releaseId the entity identifier of the release, can't be {@literal null}
	 * @param state the current state of the release, can't be {@literal null}
	 */
	public ReleaseNotPendingException(@NonNull EntityId releaseId, @NonNull ReleaseState state) {
		super(HttpStatus.CONFLICT, "Release %s is not pending, current state is: %s".formatted(releaseId.serialize(), state));
		this.releaseId = releaseId;
		this.state = state;
	}

	@Override
	public Object @Nullable [] getDetailMessageArguments() {
		return new Object[] { releaseId.serialize(), state };
	}

	@NonNull
	public EntityId getReleaseId() {
		return releaseId;
	}

	@NonNull
	public ReleaseState getState() {
		return state;
	}

}
