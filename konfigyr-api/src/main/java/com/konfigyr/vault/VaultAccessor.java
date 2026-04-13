package com.konfigyr.vault;

import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import org.jmolecules.ddd.annotation.Factory;
import org.jspecify.annotations.NullMarked;

/**
 * Provides access to a {@link Vault} for a given service and profile, enforcing authorization rules
 * based on the calling {@link AuthenticatedPrincipal}.
 * <p>
 * {@code VaultAccessor} acts as a security boundary between the application layer and the Vault
 * domain. It is responsible for:
 * <ul>
 *     <li>Validating that the principal has access to the requested service</li>
 *     <li>Validating that the principal has access to the requested profile</li>
 *     <li>Resolving and returning the corresponding {@link Vault} if it exists</li>
 * </ul>
 * <p>
 * Implementations must ensure that no {@link Vault} instance is returned unless the principal is
 * authorized to access the specified resource. If access is denied, an appropriate authorization exception
 * should be thrown. The accessor must not perform any state mutations or transactional coordination, it
 * should only perform authorization and resolution of the {@link Vault} instance.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Factory
@NullMarked
@FunctionalInterface
public interface VaultAccessor {

	/**
	 * Resolves a {@link Vault} for the given {@link Service} and {@link Profile}, ensuring that the
	 * provided principal is authorized.
	 * <p>
	 * If the principal does not have access to the specified service or profile, an authorization
	 * exception must be thrown.
	 *
	 * @param principal the authenticated actor requesting access, must not be {@literal null}
	 * @param service the target service, must not be {@literal null}
	 * @param profile the target profile, must not be {@literal null}
	 * @return a {@link Vault} instance authorized for the given principal
	 * @throws org.springframework.security.access.AccessDeniedException
	 * 		if the principal is not authorized to access the Vault
	 */
	Vault open(AuthenticatedPrincipal principal, Service service, Profile profile);

}
