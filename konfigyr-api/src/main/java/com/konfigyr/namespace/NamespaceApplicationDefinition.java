package com.konfigyr.namespace;

import com.google.crypto.tink.subtle.Base64;
import com.google.crypto.tink.subtle.Hkdf;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScopes;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;

/**
 * Record used to define one {@link NamespaceApplication} that would be created via {@link NamespaceManager}.
 *
 * @param namespace entity identifier of the {@link Namespace} used as the {@link NamespaceApplication} owner
 * @param name human friendly name for the namespace application; can't be null
 * @param scopes OAuth2 scopes that are granted to the {@link NamespaceApplication}; can't be null
 * @param expiration the expiration date for {@link NamespaceApplication}; can be null
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceApplication
 **/
@ValueObject
public record NamespaceApplicationDefinition(
		@NonNull EntityId namespace,
		@NonNull String name,
		@NonNull OAuthScopes scopes,
		@Nullable OffsetDateTime expiration
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 4546458573827169841L;

	static final String CLIENT_ID_PREFIX = "kfg-";
	static final byte[] CLIENT_SECRET_VERSION = "konfigyr-v1".getBytes(StandardCharsets.UTF_8);

	/**
	 * Generates a random {@code client_id} for the given {@link NamespaceApplicationDefinition}.
	 * <p>
	 * The value is generated using the following parts:
	 * <ul>
	 *     <li>
	 *         Namespace prefix (8 bytes): deterministic, ensures uniqueness across namespaces.
	 *     </li>
	 *     <li>
	 *         Random part (16 bytes): 128 bits of entropy (≈ 3.4×10³⁸ possibilities).
	 *     </li>
	 * </ul>
	 *
	 * This makes sure that the generated {@code client_id} is cryptographically strong and non-colliding
	 * in any realistic lifetime.
	 *
	 * @param definition the namespace application definition to generate the client id for, can't be {@literal null}.
	 * @return the generated {@code client_id} that is {@code Base64 URL} encoded, never {@literal null}
	 */
	static String generateClientId(@NonNull NamespaceApplicationDefinition definition) {
		final ByteBuffer buffer = ByteBuffer.allocate(24)
				.putLong(definition.namespace().get())
				.put(KeyGenerators.secureRandom(16).generateKey());

		return CLIENT_ID_PREFIX + Base64.urlSafeEncode(buffer.array());
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
	 *         Salt: 24 bytes derived from the {@code client_id} that ensures per-client uniqueness.
	 *     </li>
	 *     <li>
	 *         Info: Version bytes used for future-proofing of different derivation contexts.
	 *     </li>
	 *     <li>
	 *         HMAC-SHA256: algorithm that gives us the 256-bit output.
	 *     </li>
	 * </ul>
	 *
	 * This makes sure that the generated {@code client_id} is cryptographically strong and non-colliding
	 * in any realistic lifetime.
	 *
	 * @param clientId the {@code client_id} for which the secret is generated for, can't be {@literal null}.
	 * @return the generated {@code client_id} that is {@code Base64 URL} encoded, never {@literal null}
	 */
	static String generateClientSecret(@NonNull String clientId) {
		Assert.hasText(clientId, "The OAuth client_id can not be blank");
		Assert.state(clientId.startsWith(CLIENT_ID_PREFIX), "The OAuth client_id is invalid");

		final byte[] bytes;

		try {
			bytes = Hkdf.computeHkdf(
					HmacAlgorithms.HMAC_SHA_256.getName(),
					KeyGenerators.secureRandom(32).generateKey(),
					Base64.urlSafeDecode(clientId.replace(CLIENT_ID_PREFIX, "")),
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

		private EntityId namespace;
		private String name;
		private OAuthScopes scopes;
		private OffsetDateTime expiration;

		private Builder() {
		}

		/**
		 * Specify the internal {@link EntityId} of the {@link Namespace} for which the
		 * {@link NamespaceApplication} would be created.
		 *
		 * @param namespace internal namespace identifier
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder namespace(Long namespace) {
			return namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the external {@link EntityId} of the {@link Namespace} for which the
		 * {@link NamespaceApplication} would be created.
		 *
		 * @param namespace external namespace identifier
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder namespace(String namespace) {
			return namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the {@link EntityId} of the {@link Namespace} for which the
		 * {@link NamespaceApplication} would be created.
		 *
		 * @param namespace namespace identifier
		 * @return namespace application definition builder
		 */
		@NonNull
		public Builder namespace(EntityId namespace) {
			this.namespace = namespace;
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
		 * Creates a new instance of the {@link NamespaceApplicationDefinition} using the values defined in the builder.
		 *
		 * @return namespace application definition instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		@NonNull
		public NamespaceApplicationDefinition build() {
			Assert.notNull(namespace, "Namespace entity identifier can not be null");
			Assert.hasText(name, "Namespace application name can not be blank");
			Assert.notNull(scopes, "OAuth scopes can not be null");

			return new NamespaceApplicationDefinition(namespace, name, scopes, expiration);
		}

	}
}
