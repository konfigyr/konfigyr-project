package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an OIDC issuer that a namespace trusts for Workload Identity token exchange.
 *
 * @param id unique entity identifier
 * @param namespace identifier of the owning namespace
 * @param name human-readable display name
 * @param description optional description
 * @param issuerUri OIDC issuer URI; must match the {@code iss} claim of subject tokens
 * @param jwksUri explicit JWKS endpoint; when {@code null}, resolved via OIDC discovery
 * @param active whether this issuer is currently trusted
 * @param allowedAudiences set of accepted {@code aud} claim values; an empty list disables audience validation
 * @param customClaims extra claim name → expected string value assertions applied during JWT validation
 * @param createdAt creation timestamp
 * @param updatedAt last-updated timestamp
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Entity
public record NamespaceTrustedIssuer(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = Namespace.class) EntityId namespace,
		@NonNull String name,
		@Nullable String description,
		@NonNull String issuerUri,
		@Nullable String jwksUri,
		boolean active,
		@NonNull List<String> allowedAudiences,
		@NonNull Map<String, String> customClaims,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 7102651946773004571L;

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to filter {@link NamespaceTrustedIssuer issuers}
	 * by their active state.
	 */
	public static final SearchQuery.Criteria<Boolean> ACTIVE_CRITERIA = SearchQuery.criteria("trusted-issuer.active", Boolean.class);

	public NamespaceTrustedIssuer {
		Assert.notNull(id, "NamespaceTrustedIssuer id must not be null");
		Assert.notNull(namespace, "NamespaceTrustedIssuer namespace id must not be null");
		Assert.hasText(name, "NamespaceTrustedIssuer name must not be blank");
		Assert.hasText(issuerUri, "NamespaceTrustedIssuer issuer URI must not be blank");
		allowedAudiences = List.copyOf(allowedAudiences);
		customClaims = Map.copyOf(customClaims);
	}

	/**
	 * Creates a new {@link Builder fluent builder} instance.
	 *
	 * @return namespace trusted issuer builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create a {@link NamespaceTrustedIssuer}.
	 */
	public static final class Builder {

		private EntityId id;
		private EntityId namespace;
		private String name;
		private String description;
		private String issuerUri;
		private String jwksUri;
		private boolean active = true;
		private final List<String> allowedAudiences = new ArrayList<>();
		private final Map<String, String> customClaims = new HashMap<>();
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} for this {@link NamespaceTrustedIssuer}.
		 *
		 * @param id internal identifier
		 * @return this builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link NamespaceTrustedIssuer}.
		 *
		 * @param id external identifier
		 * @return this builder
		 */
		@NonNull
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link NamespaceTrustedIssuer}.
		 *
		 * @param id entity identifier
		 * @return this builder
		 */
		@NonNull
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} of the owning {@link Namespace}.
		 *
		 * @param id internal namespace identifier
		 * @return this builder
		 */
		@NonNull
		public Builder namespace(Long id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} of the owning {@link Namespace}.
		 *
		 * @param id external namespace identifier
		 * @return this builder
		 */
		@NonNull
		public Builder namespace(String id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} of the owning {@link Namespace}.
		 *
		 * @param id namespace identifier
		 * @return this builder
		 */
		@NonNull
		public Builder namespace(EntityId id) {
			this.namespace = id;
			return this;
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
		 * Specify whether this issuer is active.
		 *
		 * @param active active state
		 * @return this builder
		 */
		@NonNull
		public Builder active(boolean active) {
			this.active = active;
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
		 * Adds a custom claim assertion. The JWT must contain a claim with the given name whose
		 * string value equals the given expected value.
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
		 * Specify when this {@link NamespaceTrustedIssuer} was created.
		 *
		 * @param createdAt creation timestamp
		 * @return this builder
		 */
		@NonNull
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link NamespaceTrustedIssuer} was created.
		 *
		 * @param createdAt creation timestamp
		 * @return this builder
		 */
		@NonNull
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link NamespaceTrustedIssuer} was last updated.
		 *
		 * @param updatedAt last-updated timestamp
		 * @return this builder
		 */
		@NonNull
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link NamespaceTrustedIssuer} was last updated.
		 *
		 * @param updatedAt last-updated timestamp
		 * @return this builder
		 */
		@NonNull
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new {@link NamespaceTrustedIssuer} from this builder.
		 *
		 * @return namespace trusted issuer, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public NamespaceTrustedIssuer build() {
			Assert.notNull(id, "Namespace trusted issuer identifier can not be null");
			Assert.notNull(namespace, "Namespace identifier can not be null");
			Assert.hasText(name, "Namespace trusted issuer name can not be blank");
			Assert.hasText(issuerUri, "Namespace trusted issuer issuer URI can not be blank");

			return new NamespaceTrustedIssuer(id, namespace, name, description, issuerUri,
					jwksUri, active, allowedAudiences, customClaims, createdAt, updatedAt);
		}

	}

}
