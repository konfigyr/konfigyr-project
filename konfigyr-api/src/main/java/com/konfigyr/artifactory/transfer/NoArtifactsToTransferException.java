package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryException;
import com.konfigyr.artifactory.Owner;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when an {@link ArtifactOwnershipTransfer} is requested for a namespace that owns no
 * artifacts under the requested {@code groupId}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class NoArtifactsToTransferException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = 5145318207572830461L;

	/**
	 * Maven {@code groupId} coordinate under which the {@code from} namespace owns no artifacts.
	 */
	private final String groupId;

	/**
	 * The namespace that was expected to own artifacts under the {@code groupId} but does not.
	 */
	private final Owner from;

	/**
	 * Create a new instance when the given {@code from} namespace owns no artifacts under the
	 * supplied {@code groupId}.
	 *
	 * @param groupId the artifact groupId that has no artifacts owned by {@code from}, can't be {@literal null}
	 * @param from the namespace expected to own artifacts under the groupId, can't be {@literal null}
	 */
	public NoArtifactsToTransferException(@NonNull String groupId, @NonNull Owner from) {
		super(HttpStatus.BAD_REQUEST, "Namespace '%s' does not own any artifacts under groupId '%s'".formatted(from.slug(), groupId));
		this.groupId = groupId;
		this.from = from;
	}

	/**
	 * Returns the {@code groupId} coordinate under which the {@code from} namespace owns no artifacts.
	 *
	 * @return the {@code groupId} coordinate, never {@literal null}
	 */
	@NonNull
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Returns the namespace that was expected to own artifacts under the {@code groupId} but does not.
	 *
	 * @return the namespace with no artifacts under the {@code groupId}, never {@literal null}
	 */
	@NonNull
	public Owner getFrom() {
		return from;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { groupId, from.slug() };
	}
}
