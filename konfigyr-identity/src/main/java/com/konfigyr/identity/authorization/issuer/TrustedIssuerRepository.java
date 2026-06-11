package com.konfigyr.identity.authorization.issuer;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for resolving a {@link TrustedIssuerRegistration} given a namespace and an
 * issuer URI.
 * <p>
 * Implementations may back the registry with static well-known issuers, a database
 * table scoped to a namespace, a remote configuration service, or any combination
 * thereof. The {@link CompositeTrustedIssuerRepository} delegates to an ordered list
 * of repositories and returns the first non-null match, which allows global and
 * namespace-scoped implementations to coexist without coupling.
 * <p>
 * Returning {@code null} from {@link #lookup} signals that this repository has no
 * record of the issuer for the given namespace; it does not imply that the issuer is
 * explicitly untrusted. The caller is responsible for treating an unresolved issuer as
 * an authentication failure.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see CompositeTrustedIssuerRepository
 * @see WellKnownTrustedIssuers
 */
@NullMarked
public interface TrustedIssuerRepository {

	/**
	 * Looks up the {@link TrustedIssuerRegistration} for the given namespace and issuer URI.
	 *
	 * @param namespace the namespace on whose behalf the lookup is performed
	 * @param issuerUri the OIDC issuer URI to resolve, taken from the workload
	 *                  application's client settings
	 * @return the matching {@link TrustedIssuerRegistration}, or {@code null} if this repository
	 *         has no entry for the given combination
	 */
	@Nullable
	TrustedIssuerRegistration lookup(EntityId namespace, String issuerUri);

}
