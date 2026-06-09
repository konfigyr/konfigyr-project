package com.konfigyr.namespace;

import com.google.crypto.tink.subtle.Base64;
import com.google.crypto.tink.subtle.Hkdf;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.NamespaceApplicationSettings;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import com.konfigyr.security.OAuthScopes;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;

/**
 * Record used to define one {@link NamespaceApplication} that would be created via {@link NamespaceManager}.
 *
 * @param namespace  entity identifier of the {@link Namespace} used as the {@link NamespaceApplication} owner
 * @param type       the intended purpose of the application; determines the OAuth2 grant types,
 *                   client authentication methods, and security constraints applied at registration
 * @param name       human friendly name for the namespace application; can't be null
 * @param scopes     OAuth2 scopes that are granted to the {@link NamespaceApplication}; can't be null
 * @param expiration the expiration date for {@link NamespaceApplication}; can be null
 * @param settings   type-specific configuration for the application
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceApplication
 * @see NamespaceClientType
 * @see NamespaceApplicationSettings
 **/
@ValueObject
public record NamespaceApplicationDefinition(
		@NonNull EntityId namespace,
		@NonNull NamespaceClientType type,
		@NonNull String name,
		@NonNull OAuthScopes scopes,
		@Nullable OffsetDateTime expiration,
		@Nullable NamespaceApplicationSettings settings
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 4546458573827169841L;

	static final byte[] CLIENT_SECRET_VERSION = "konfigyr-v1".getBytes(StandardCharsets.UTF_8);

	/**
	 * Generates a random {@link NamespaceClientId} for the given {@link NamespaceApplicationDefinition}.
	 *
	 * @param definition the namespace application definition to generate the client id for, can't be {@literal null}.
	 * @return the generated {@code client_id} value for a {@link Namespace}, never {@literal null}
	 * @see NamespaceClientId
	 */
	static NamespaceClientId generateClientId(@NonNull NamespaceApplicationDefinition definition) {
		return NamespaceClientId.of(definition.namespace(), definition.type());
	}

	/**
	 * Generates a deterministic but unguessable {@code client_secret} derived using {@code HMAC} (HMAC-based
	 * Key Derivation Function).
	 * <p>
	 * The value is generated using the following parts:
	 * <ul>
	 *     <li>
	 *         Input key material: 32 bytes random that provides 256 bits of entropy.
	 *     </li>
	 *     <li>
	 *         Salt: 32 bytes decoded from the {@code client_id} that ensures per-client uniqueness.
	 *     </li>
	 *     <li>
	 *         Info: Version bytes used for future-proofing of different derivation contexts.
	 *     </li>
	 *     <li>
	 *         HMAC-SHA256: algorithm that gives us the 256-bit output.
	 *     </li>
	 * </ul>
	 *
	 * This makes sure that the generated {@code client_secret} is cryptographically strong and non-colliding
	 * in any realistic lifetime.
	 *
	 * @param clientId the {@link NamespaceClientId} for which the secret is generated, can't be {@literal null}.
	 * @return the generated {@code client_secret} that is {@code Base64 URL} encoded, never {@literal null}
	 * @throws NamespaceApplicationTypeException when the {@link NamespaceClientType} does not support client secrets.
	 */
	static String generateClientSecret(@NonNull NamespaceClientId clientId) {
		if (!clientId.type().requiresSecret()) {
			throw new NamespaceApplicationTypeException(clientId.type());
		}

		final byte[] bytes;

		try {
			bytes = Hkdf.computeHkdf(
					HmacAlgorithms.HMAC_SHA_256.getName(),
					KeyGenerators.secureRandom(32).generateKey(),
					clientId.bytes().array(),
					CLIENT_SECRET_VERSION,
					32
			);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to generate OAuth client secret for namespace application", ex);
		}

		return Base64.urlSafeEncode(bytes);
	}

	/**
	 * Creates a new {@link Builder fluent namespace definition builder} instance used to
	 * create the {@link NamespaceApplicationDefinition} record.
	 *
	 * @return namespace definition builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder type used to create an {@link NamespaceApplicationDefinition}.
	 */
	public static final class Builder {

		private Namespace namespace;
		private NamespaceClientType type;
		private String name;
		private OAuthScopes scopes;
		private OffsetDateTime expiration;
		private NamespaceApplicationSettings settings;

		private Builder() {
		}

		/**
		 * Specify the internal the {@link Namespace} for which the {@link NamespaceApplication} would be created.
		 *
		 * @param namespace the namespace that should be marked as the owner of the application
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder namespace(Namespace namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Specify the {@link NamespaceClientType type} of this application. The type determines
		 * the OAuth2 grant types, client authentication methods, and security constraints
		 * applied when the application is registered with the authorization server.
		 *
		 * @param type the intended purpose of the application
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder type(NamespaceClientType type) {
			this.type = type;
			return this;
		}

		/**
		 * Specify the user-friendly name for this {@link NamespaceApplicationDefinition}.
		 *
		 * @param name namespace application name
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Specify which OAuth {@code scope} should be granted to this {@link NamespaceApplication}.
		 *
		 * @param scopes the OAuth scopes to be granted
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder scopes(String scopes) {
			return scopes(OAuthScopes.parse(scopes));
		}

		/**
		 * Specify which OAuth {@code scope} should be granted to this {@link NamespaceApplication}.
		 *
		 * @param scopes the OAuth scopes to be granted
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder scopes(OAuthScopes scopes) {
			this.scopes = scopes;
			return this;
		}

		/**
		 * Specify which the expiry date for this {@link NamespaceApplication}. Set this to {@literal null}
		 * if the application should never expire.
		 *
		 * @param expiration the expiration date
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder expiration(OffsetDateTime expiration) {
			this.expiration = expiration;
			return this;
		}

		/**
		 * Specify the type-specific {@link NamespaceApplicationSettings} for this application.
		 * <p>
		 * Required for {@link NamespaceClientType#AGENT} and {@link NamespaceClientType#PIPELINE};
		 * leave {@literal null} for {@link NamespaceClientType#SERVICE_ACCOUNT}.
		 *
		 * @param settings type-specific application settings, or {@literal null}
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder settings(@Nullable NamespaceApplicationSettings settings) {
			this.settings = settings;
			return this;
		}

		/**
		 * Creates a new instance of the {@link NamespaceApplicationDefinition} using the values defined in the builder.
		 *
		 * @return namespace application definition instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public NamespaceApplicationDefinition build() {
			Assert.notNull(namespace, "Namespace can not be null");
			Assert.notNull(type, "Namespace client type can not be null");
			Assert.hasText(name, "Namespace application name can not be blank");
			Assert.notNull(scopes, "OAuth scopes can not be null");

			return new NamespaceApplicationDefinition(namespace.id(), type, name, scopes, expiration, settings);
		}

	}
}
