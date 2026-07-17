package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.ArtifactoryException;
import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when an {@link ArtifactOwnershipTransfer} cannot be found for the requested
 * namespace and transfer identifier.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ArtifactOwnershipTransferNotFoundException extends ArtifactoryException {

	@Serial
	private static final long serialVersionUID = -3854923014028443771L;

	/**
	 * The namespace that attempted to look up a transfer it is not a party to, or that does not exist.
	 */
	private final Owner owner;

	/**
	 * Create a new instance when no {@link ArtifactOwnershipTransfer} with the given {@code id} is visible
	 * to the supplied {@code owner}, either because it does not exist or because {@code owner} is neither
	 * the {@code from} nor the {@code to} party to it.
	 *
	 * @param owner the namespace that attempted the lookup, can't be {@literal null}
	 * @param id the transfer identifier that could not be resolved, can't be {@literal null}
	 */
	public ArtifactOwnershipTransferNotFoundException(@NonNull Owner owner, @NonNull EntityId id) {
		super(HttpStatus.NOT_FOUND, "Could not find an artifact ownership transfer '%s' visible to '%s' namespace"
				.formatted(id, owner.slug()));
		this.owner = owner;
	}

	/**
	 * Returns the namespace that attempted to look up a transfer it is not a party to, or that does not exist.
	 *
	 * @return the namespace that attempted the lookup, never {@literal null}
	 */
	@NonNull
	public Owner getOwner() {
		return owner;
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { owner.slug() };
	}
}
