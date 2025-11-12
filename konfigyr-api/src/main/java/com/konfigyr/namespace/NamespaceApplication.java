package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.support.SearchQuery;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Represents an OAuth2 client application that belongs to a specific {@link Namespace}.
 * <p>
 * A {@link NamespaceApplication} models an integration or automation client that operates within the security
 * boundary of a Konfigyr namespace. Each application possesses its own {@code client_id} and {@code client_secret}
 * credentials, and can be authorized to access Konfigyr APIs on behalf of the namespace or its members.
 * <p>
 * Namespace applications serve as the foundation for:
 * <ul>
 *   <li>Integrations with external systems such as CI/CD pipelines, build tools, or monitoring services.</li>
 *   <li>Internal automation clients (e.g. Konfigyr CLI, synchronization agents).</li>
 *   <li>Fine-grained access control using OAuth2 scopes and token lifetimes.</li>
 * </ul>
 * <p>
 * Each application is isolated to its owning namespace and cannot access resources from other namespaces.
 * The credentials are issued, rotated, and revoked by namespace administrators through Konfigyr’s Identity
 * Provider, following OAuth2 and OpenID Connect best practices.
 * <p>
 * <b>Security characteristics:</b>
 * <ul>
 *   <li>Client credentials are generated using cryptographically secure random material and HKDF-derived secrets.</li>
 *   <li>All secrets are stored in hashed using {@code Argon2id} and displayed only once upon creation.</li>
 *   <li>Supports confidential and public client types, depending on deployment context.</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Entity
public record NamespaceApplication(
		@NonNull @Identity EntityId id,
		@NonNull @Association(aggregateType = Namespace.class) EntityId namespace,
		@NonNull String name,
		@NonNull String clientId,
		@Nullable String clientSecret,
		@NonNull OAuthScopes scopes,
		@Nullable OffsetDateTime expiresAt,
		@Nullable OffsetDateTime createdAt,
		@Nullable OffsetDateTime updatedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = -7197125169996865774L;

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to narrow down the search of
	 * {@link NamespaceApplication applications} by their {@link EntityId entity identifier}.
	 */
	public static SearchQuery.Criteria<EntityId> ID_CRITERIA = SearchQuery.criteria("oauth-application.id", EntityId.class);

	/**
	 * The {@link SearchQuery.Criteria} descriptor used to filter {@link NamespaceApplication applications}
	 * by their expiration state.
	 */
	public static SearchQuery.Criteria<Boolean> ACTIVE_CRITERIA = SearchQuery.criteria("oauth-application.active", Boolean.class);

	/**
	 * Creates a new {@link Builder fluent namespace application builder} instance used to create
	 * the {@link NamespaceApplication} record.
	 *
	 * @return namespace application builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	@NonNull
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("id", id)
				.append("namespace", namespace)
				.append("name", name)
				.append("scopes", scopes)
				.append("expiresAt", expiresAt)
				.append("createdAt", createdAt)
				.append("updatedAt", updatedAt)
				.toString();
	}

	/**
	 * Fluent builder type used to create an {@link NamespaceApplication}.
	 */
	public static final class Builder {
		private EntityId id;
		private EntityId namespace;
		private String name;
		private String clientId;
		private String clientSecret;
		private OAuthScopes scopes;
		private OffsetDateTime expiresAt;
		private OffsetDateTime createdAt;
		private OffsetDateTime updatedAt;

		/**
		 * Specify the internal {@link EntityId} for this {@link NamespaceApplication}.
		 *
		 * @param id internal namespace application identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder id(Long id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} for this {@link NamespaceApplication}.
		 *
		 * @param id external namespace application identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder id(String id) {
			return id(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} for this {@link NamespaceApplication}.
		 *
		 * @param id namespace application identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder id(EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link Namespace} to which
		 * this {@link NamespaceApplication} belongs to.
		 *
		 * @param id internal namespace identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder namespace(Long id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link Namespace} to which
		 * this {@link NamespaceApplication} belongs to.
		 *
		 * @param id external namespace identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder namespace(String id) {
			return namespace(EntityId.from(id));
		}

		/**
		 * Specify the {@link EntityId} of the {@link Namespace} to which
		 * this {@link NamespaceApplication} belongs to.
		 *
		 * @param id namespace identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder namespace(EntityId id) {
			this.namespace = id;
			return this;
		}

		/**
		 * Specify the human-friendly name for this {@link NamespaceApplication}.
		 *
		 * @param name the application name
		 * @return namespace application builder
		 */
		@NonNull
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify the OAuth {@code client_id} for this {@link NamespaceApplication}.
		 *
		 * @param clientId the OAuth client identifier
		 * @return namespace application builder
		 */
		@NonNull
		public Builder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		/**
		 * Specify the OAuth {@code client_secret} for this {@link NamespaceApplication}.
		 *
		 * @param clientSecret the OAuth client secret
		 * @return namespace application builder
		 */
		@NonNull
		public Builder clientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
			return this;
		}

		/**
		 * Specify which OAuth {@code scope} should be granted to this {@link NamespaceApplication}.
		 *
		 * @param scopes the OAuth scopes to be granted
		 * @return namespace application builder
		 */
		@NonNull
		public Builder scopes(String scopes) {
			return scopes(OAuthScopes.parse(scopes));
		}

		/**
		 * Specify which OAuth {@code scope} should be granted to this {@link NamespaceApplication}.
		 *
		 * @param scopes the OAuth scopes to be granted
		 * @return namespace application builder
		 */
		@NonNull
		public Builder scopes(OAuthScopes scopes) {
			this.scopes = scopes;
			return this;
		}

		/**
		 * Specify the date when did this {@link NamespaceApplication} should expire.
		 *
		 * @param expiresAt expiration date
		 * @return namespace application builder
		 */
		@NonNull
		public Builder expiresAt(Instant expiresAt) {
			return expiresAt(OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
		}

		/**
		 * Specify the date when did this {@link NamespaceApplication} should expire.
		 *
		 * @param expiresAt expiration date
		 * @return namespace application builder
		 */
		@NonNull
		public Builder expiresAt(OffsetDateTime expiresAt) {
			this.expiresAt = expiresAt;
			return this;
		}

		/**
		 * Specify when this {@link NamespaceApplication} was created.
		 *
		 * @param createdAt created date
		 * @return namespace application builder
		 */
		@NonNull
		public Builder createdAt(Instant createdAt) {
			return createdAt(OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link NamespaceApplication} was created.
		 *
		 * @param createdAt created date
		 * @return namespace application builder
		 */
		@NonNull
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Specify when this {@link NamespaceApplication} was last updated.
		 *
		 * @param updatedAt last updated date
		 * @return namespace application builder
		 */
		@NonNull
		public Builder updatedAt(Instant updatedAt) {
			return updatedAt(OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC));
		}

		/**
		 * Specify when this {@link NamespaceApplication} was last updated.
		 *
		 * @param updatedAt last updated date
		 * @return namespace application builder
		 */
		@NonNull
		public Builder updatedAt(OffsetDateTime updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}

		/**
		 * Creates a new instance of the {@link NamespaceApplication} using the values defined in the builder.
		 *
		 * @return namespace application instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public NamespaceApplication build() {
			Assert.notNull(id, "Namespace application identifier can not be blank");
			Assert.notNull(namespace, "Namespace identifier can not be blank");
			Assert.hasText(name, "Namespace application name can not be blank");
			Assert.hasText(clientId, "OAuth client_id can not be blank");
			Assert.notNull(scopes, "OAuth scopes can not be null");

			return new NamespaceApplication(id, namespace, name, clientId, clientSecret, scopes, expiresAt, createdAt, updatedAt);
		}

	}
}
