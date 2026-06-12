package com.konfigyr.namespace;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Value object used to create or update a {@link NamespaceTrustedIssuer} via {@link NamespaceManager}.
 *
 * @param name human-readable display name
 * @param description optional description
 * @param issuerUri OIDC issuer URI; must match the {@code iss} claim of subject tokens
 * @param jwksUri explicit JWKS endpoint; when {@code null}, resolved via OIDC discovery
 * @param allowedAudiences set of accepted {@code aud} claim values; an empty list disables audience validation
 * @param customClaims extra claim name → expected string value assertions applied during JWT validation
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@ValueObject
public record NamespaceTrustedIssuerDefinition(
		@NonNull String name,
		@Nullable String description,
		@NonNull String issuerUri,
		@Nullable String jwksUri,
		@NonNull List<String> allowedAudiences,
		@NonNull Map<String, String> customClaims
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6623051866107394523L;

	public NamespaceTrustedIssuerDefinition {
		Assert.hasText(name, "NamespaceTrustedIssuerDefinition name must not be blank");
		Assert.hasText(issuerUri, "NamespaceTrustedIssuerDefinition issuer URI must not be blank");
		allowedAudiences = List.copyOf(allowedAudiences);
		customClaims = Map.copyOf(customClaims);
	}

	/**
	 * Creates a new {@link Builder} instance.
	 *
	 * @return builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder for {@link NamespaceTrustedIssuerDefinition}.
	 */
	public static final class Builder {

		private String name;
		private String description;
		private String issuerUri;
		private String jwksUri;
		private final List<String> allowedAudiences = new ArrayList<>();
		private final Map<String, String> customClaims = new HashMap<>();

		private Builder() {
		}

		/**
		 * Specify the human-readable display name.
		 *
		 * @param name display name
		 * @return this builder
		 */
		@NonNull
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify the optional description.
		 *
		 * @param description description
		 * @return this builder
		 */
		@NonNull
		public Builder description(@Nullable String description) {
			this.description = description;
			return this;
		}

		/**
		 * Specify the OIDC issuer URI.
		 *
		 * @param issuerUri issuer URI
		 * @return this builder
		 */
		@NonNull
		public Builder issuerUri(String issuerUri) {
			this.issuerUri = issuerUri;
			return this;
		}

		/**
		 * Specify the explicit JWKS endpoint URI. When {@code null}, it is resolved via OIDC discovery.
		 *
		 * @param jwksUri JWKS endpoint URI, or {@code null}
		 * @return this builder
		 */
		@NonNull
		public Builder jwksUri(@Nullable String jwksUri) {
			this.jwksUri = jwksUri;
			return this;
		}

		/**
		 * Adds an accepted {@code aud} claim value.
		 *
		 * @param audience audience value to accept
		 * @return this builder
		 */
		@NonNull
		public Builder allowedAudience(@Nullable String audience) {
			if (audience != null && !audience.isBlank()) {
				this.allowedAudiences.add(audience);
			}
			return this;
		}

		/**
		 * Replaces all accepted {@code aud} claim values with the given collection.
		 * An empty list disables audience validation.
		 *
		 * @param allowedAudiences accepted audience values, or {@code null} to clear
		 * @return this builder
		 */
		@NonNull
		public Builder allowedAudiences(@Nullable Collection<String> allowedAudiences) {
			this.allowedAudiences.clear();
			if (allowedAudiences != null) {
				allowedAudiences.forEach(this::allowedAudience);
			}
			return this;
		}

		/**
		 * Adds a custom claim assertion.
		 *
		 * @param claim claim name
		 * @param value expected claim value
		 * @return this builder
		 */
		@NonNull
		public Builder customClaim(String claim, String value) {
			this.customClaims.put(claim, value);
			return this;
		}

		/**
		 * Replaces all custom claim assertions with the given map.
		 *
		 * @param customClaims claim name → expected value map, or {@code null} to clear
		 * @return this builder
		 */
		@NonNull
		public Builder customClaims(@Nullable Map<String, String> customClaims) {
			this.customClaims.clear();
			if (customClaims != null) {
				this.customClaims.putAll(customClaims);
			}
			return this;
		}

		/**
		 * Builds a {@link NamespaceTrustedIssuerDefinition}.
		 *
		 * @return definition, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public NamespaceTrustedIssuerDefinition build() {
			return new NamespaceTrustedIssuerDefinition(name, description, issuerUri, jwksUri,
					allowedAudiences, customClaims);
		}

	}

}
