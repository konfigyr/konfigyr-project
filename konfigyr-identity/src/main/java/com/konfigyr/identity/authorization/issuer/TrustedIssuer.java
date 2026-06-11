package com.konfigyr.identity.authorization.issuer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Immutable value object describing an external OIDC identity provider that this
 * authorization server trusts as a source of subject tokens during token exchange.
 * <p>
 * A trusted issuer records everything the authorization server needs to validate an
 * inbound JWT without going through the full OIDC login flow: where to fetch the
 * signing keys, and which audience values are acceptable. It is intentionally
 * decoupled from any particular use-case (Pipeline Integration, Service Accounts,
 * future workload-identity flows) so that the same registry can serve multiple grant
 * types as the platform evolves.
 *
 * @param issuerUri the OIDC issuer URI; must match the {@code iss} claim of subject
 *                  tokens issued by this provider
 * @param name human-readable display name, used in audit logs and the management UI
 * @param jwksUri explicit JWKS endpoint URI; when {@code null} the authorization server
 *                resolves the endpoint via OIDC discovery ({@code /.well-known/openid-configuration})
 * @param allowedAudiences set of accepted {@code aud} claim values; an empty set
 *                          disables audience validation for this issuer
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public record TrustedIssuer(
		String issuerUri,
		String name,
		@Nullable String jwksUri,
		Set<String> allowedAudiences
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 102723649959045957L;

	public TrustedIssuer {
		Assert.hasText(issuerUri, "Trusted issuer URI must not be blank");
		Assert.hasText(name, "Trusted issuer name must not be blank");
		allowedAudiences = Set.copyOf(allowedAudiences);
	}

}
