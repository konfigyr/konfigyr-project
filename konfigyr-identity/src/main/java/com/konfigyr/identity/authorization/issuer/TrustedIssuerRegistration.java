package com.konfigyr.identity.authorization.issuer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable value object describing an external OIDC identity provider that this
 * authorization server trusts as a source of subject tokens during token exchange.
 * <p>
 * Use {@link #withId(String)} to construct a new registration or {@link #from(TrustedIssuerRegistration)}
 * to produce a modified copy of an existing one.
 *
 * @param id unique identifier for this issuer registration
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
public record TrustedIssuerRegistration(
		String id,
		String issuerUri,
		String name,
		@Nullable String jwksUri,
		Set<String> allowedAudiences
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 102723649959045957L;

	public TrustedIssuerRegistration {
		Assert.hasText(id, "Trusted issuer registration identifier must not be blank");
		Assert.hasText(issuerUri, "Trusted issuer URI must not be blank");
		Assert.hasText(name, "Trusted issuer name must not be blank");
		allowedAudiences = Set.copyOf(allowedAudiences);
	}

	/**
	 * Creates a new {@link Builder} initialized with the given registration identifier.
	 *
	 * @param id the unique identifier for the registration, e.g. {@code "github-actions"}
	 * @return a new {@link Builder}
	 */
	public static Builder withId(String id) {
		return new Builder(id);
	}

	/**
	 * Creates a new {@link Builder} pre-populated from an existing registration. Useful
	 * for producing a modified copy without altering the original.
	 *
	 * @param registration the registration to copy from, must not be {@code null}
	 * @return a new {@link Builder} pre-populated with all fields from {@code registration}
	 */
	public static Builder from(TrustedIssuerRegistration registration) {
		Assert.notNull(registration, "TrustedIssuerRegistration must not be null");
		return new Builder(registration.id())
				.issuerUri(registration.issuerUri())
				.name(registration.name())
				.jwksUri(registration.jwksUri())
				.allowedAudiences(registration.allowedAudiences());
	}

	/**
	 * Builder for {@link TrustedIssuerRegistration}.
	 * <p>
	 * Create an instance via {@link TrustedIssuerRegistration#withId(String)} for a new
	 * registration or via {@link TrustedIssuerRegistration#from(TrustedIssuerRegistration)}
	 * to produce a modified copy of an existing one.
	 */
	public static final class Builder {

		private final String id;
		@Nullable private String issuerUri;
		@Nullable private String name;
		@Nullable private String jwksUri;
		private final Set<String> allowedAudiences = new HashSet<>();

		private Builder(String id) {
			Assert.hasText(id, "Trusted issuer registration identifier must not be blank");
			this.id = id;
		}

		/**
		 * Sets the OIDC issuer URI. Must match the {@code iss} claim of tokens issued
		 * by this provider.
		 *
		 * @param issuerUri the issuer URI, must not be blank
		 * @return this builder
		 */
		public Builder issuerUri(String issuerUri) {
			this.issuerUri = issuerUri;
			return this;
		}

		/**
		 * Sets the human-readable display name for this registration.
		 *
		 * @param name the display name, must not be blank
		 * @return this builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the explicit JWKS endpoint URI. When not set or set to {@code null}, the
		 * endpoint is resolved via OIDC discovery at runtime.
		 *
		 * @param jwksUri the JWKS endpoint URI, or {@code null} to rely on discovery
		 * @return this builder
		 */
		public Builder jwksUri(@Nullable String jwksUri) {
			this.jwksUri = jwksUri;
			return this;
		}

		/**
		 * Adds a single accepted {@code aud} claim value. May be called multiple times
		 * to accumulate more than one audience.
		 *
		 * @param audience the audience value to be accepted
		 * @return this builder
		 */
		public Builder allowedAudience(@Nullable String audience) {
			if (StringUtils.hasText(audience)) {
				this.allowedAudiences.add(audience);
			}
			return this;
		}

		/**
		 * Adds all the given {@code aud} claim values to the accepted set.
		 *
		 * @param audiences the audience values to accept
		 * @return this builder
		 */
		public Builder allowedAudiences(String @Nullable... audiences) {
			return audiences == null ? this : allowedAudiences(Arrays.asList(audiences));
		}

		/**
		 * Adds all the given {@code aud} claim values to the accepted set.
		 *
		 * @param audiences the audience values to accept
		 * @return this builder
		 */
		public Builder allowedAudiences(@Nullable Collection<String> audiences) {
			if (audiences != null) {
				audiences.forEach(this::allowedAudience);
			}
			return this;
		}

		/**
		 * Builds and returns the {@link TrustedIssuerRegistration}.
		 *
		 * @return the registration
		 * @throws IllegalArgumentException if {@code issuerUri} or {@code name} are not set or blank
		 */
		public TrustedIssuerRegistration build() {
			return new TrustedIssuerRegistration(id, issuerUri, name, jwksUri, allowedAudiences);
		}

	}

}
