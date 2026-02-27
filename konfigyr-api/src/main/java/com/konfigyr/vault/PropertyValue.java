package com.konfigyr.vault;

import com.google.crypto.tink.subtle.Hex;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

/**
 * Base type used for representing different states of configuration property values.
 * <p>
 * A {@link PropertyValue} represents a configuration value together with its integrity metadata
 * represented by the {@code checksum}. Concrete subclasses define whether the value is sealed
 * (encrypted or protected) or unsealed (plaintext).
 * <p>
 * This abstraction intentionally separates different states (sealed or unsealed) of the value
 * and their cryptographic responsibilities, while trying to share their common denominator, the
 * {@code checksum} that is used to provide integrity verification.
 * <p>
 * The {@code checksum} is a deterministic cryptographic hash of the original unsealed, plaintext
 * value of the property, the property path to which this property is assigned to, and the unique
 * profile identifier that owns the configuration property. This {@code checksum} is used to
 * perform equality checks of the property value without needing to perform unnecessary cryptographic
 * operations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public abstract sealed class PropertyValue implements Supplier<ByteArray>, Serializable
		permits PropertyValue.Sealed, PropertyValue.Unsealed {

	@Serial
	private static final long serialVersionUID = 535177591751947928L;

	/**
	 * Creates a new <b>unsealed</b> {@link PropertyValue} instance for the given profile,
	 * property name, and value.
	 * <p>
	 * This method would try to generate the {@code checksum} for the given property name,
	 * value, and the given profile identifier.
	 *
	 * @param profile the profile identifier, cannot be {@literal null}.
	 * @param name the configuration property name, cannot be {@literal null}.
	 * @param value the configuration property value, cannot be {@literal null}.
	 * @return the new unsealed {@link PropertyValue} instance.
	 * @throws IllegalStateException if the digest algorithm is not supported.
	 * @throws IllegalArgumentException if the given {@code value} is empty or if the {@code name} is invalid
	 */
	public static PropertyValue create(EntityId profile, String name, String value) {
		final ByteArray valueBytes = ByteArray.fromString(value, StandardCharsets.UTF_8);
		final ConfigurationPropertyName normalizedName = ConfigurationPropertyName.adapt(name, '.');

		if (normalizedName.isEmpty()) {
			throw new IllegalArgumentException("Invalid configuration property name: " + name);
		}

		return unsealed(valueBytes, generateChecksum(profile, normalizedName, valueBytes));
	}

	/**
	 * Creates a new <b>sealed</b> {@link PropertyValue} instance for the given encrypted
	 * property value, and it's checksum.
	 *
	 * @param value the encrypted property value, cannot be {@literal null} or empty.
	 * @param checksum the checksum of the encrypted value, cannot be {@literal null}.
	 * @return the sealed {@link PropertyValue} instance.
	 */
	public static PropertyValue sealed(ByteArray value, ByteArray checksum) {
		return new Sealed(value, checksum);
	}

	/**
	 * Creates a new <b>unsealed</b> {@link PropertyValue} instance for the given raw
	 * property value, and it's checksum.
	 *
	 * @param value the raw property value, cannot be {@literal null} or empty.
	 * @param checksum the checksum of the value, cannot be {@literal null}.
	 * @return the sealed {@link PropertyValue} instance.
	 */
	public static PropertyValue unsealed(ByteArray value, ByteArray checksum) {
		return new Unsealed(value, checksum);
	}

	protected final ByteArray value;
	protected final ByteArray checksum;

	protected PropertyValue(ByteArray value, ByteArray checksum) {
		this.value = value;
		this.checksum = checksum;
	}

	/**
	 * Returns the underlying value of this configuration property. In case this value is unsealed,
	 * the returned {@link ByteArray} is a raw representation of the original plaintext value,
	 * otherwise the ciphertext of an unencrypted value is returned.
	 *
	 * @return the configuration property value, never {@literal null}.
	 */
	@Override
	public ByteArray get() {
		return value;
	}

	/**
	 * Returns the checksum associated with this configuration property value.
	 * <p>
	 * The checksum is used to verify integrity and equality without requiring decryption.
	 *
	 * @return the property value checksum, never {@literal null}.
	 */
	public final ByteArray checksum() {
		return checksum;
	}

	/**
	 * Method that checks if this configuration property value is sealed.
	 *
	 * @return {@literal true} if this configuration property value is sealed, {@literal false} otherwise.
	 */
	public abstract boolean isSealed();

	/**
	 * Method that checks if this configuration property value is unsealed.
	 *
	 * @return {@literal true} if this configuration property value is unsealed, {@literal false} otherwise.
	 */
	public abstract boolean isUnsealed();

	/**
	 * Seals this configuration property value using the given {@link KeysetOperations}.
	 *
	 * @param operations the keyset operations to use for sealing the value, cannot be {@literal null}.
	 * @return the sealed configuration property value, never {@literal null}.
	 */
	public abstract PropertyValue seal(KeysetOperations operations);

	/**
	 * Unseals this configuration property value using the given {@link KeysetOperations}.
	 *
	 * @param operations the keyset operations to use for unsealing the value, cannot be {@literal null}.
	 * @return the unsealed configuration property value, never {@literal null}.
	 */
	public abstract PropertyValue unseal(KeysetOperations operations);

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof PropertyValue that)) {
			return false;
		}
		return isSealed() == that.isSealed() && checksum.equals(that.checksum);
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode(isSealed());
		result = 31 * result + checksum.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return (isSealed() ? "Sealed" : "Unsealed") + '(' + Hex.encode(checksum.array()) + ')';
	}

	/**
	 * Attempts to generate a deterministic checksum for the given property value. The unique profile identifier
	 * that owns the property is added to the checksum to provide a certain extent of randomness to the checksum
	 * generation process.
	 *
	 * @param profile the entity identifier of the profile owning the property, cannot be {@literal null}.
	 * @param name the normalized configuration property name, cannot be {@literal null}.
	 * @param value the property value, cannot be {@literal null}.
	 * @return the generated checksum, never {@literal null}.
	 */
	private static ByteArray generateChecksum(EntityId profile, ConfigurationPropertyName name, ByteArray value) {
		final MessageDigest digest;

		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Could not create generator as digest algorithm is not supported", e);
		}

		final ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES)
				.putLong(profile.get())
				.putLong(name.hashCode());

		digest.update(buffer);
		digest.update(value.array());
		return new ByteArray(digest.digest());
	}

	@NullMarked
	private static final class Sealed extends PropertyValue {

		Sealed(ByteArray value, ByteArray checksum) {
			super(value, checksum);
		}

		@Override
		public boolean isSealed() {
			return true;
		}

		@Override
		public boolean isUnsealed() {
			return false;
		}

		@Override
		public PropertyValue seal(KeysetOperations operations) {
			return this;
		}

		@Override
		public PropertyValue unseal(KeysetOperations operations) {
			return new Unsealed(operations.decrypt(value, checksum), checksum);
		}
	}

	@NullMarked
	private static final class Unsealed extends PropertyValue {

		private Unsealed(ByteArray value, ByteArray checksum) {
			super(value, checksum);
		}

		@Override
		public boolean isSealed() {
			return false;
		}

		@Override
		public boolean isUnsealed() {
			return true;
		}

		@Override
		public PropertyValue seal(KeysetOperations operations) {
			return new Sealed(operations.encrypt(value, checksum), checksum);
		}

		@Override
		public PropertyValue unseal(KeysetOperations operations) {
			return this;
		}
	}
}
