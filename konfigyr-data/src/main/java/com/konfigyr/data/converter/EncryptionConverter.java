package com.konfigyr.data.converter;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;
import org.jooq.impl.AbstractConverter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link Converter jOOQ Converter} that would perform encryption operations on
 * the {@link ByteArray} using {@link KeysetOperations}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class EncryptionConverter extends AbstractConverter<ByteArray, ByteArray> {

	private final KeysetOperations operations;

	/**
	 * Creates a new {@link EncryptionConverter} with the {@link KeysetOperations} that would perform
	 * encrypt or decrypt operations on the underlying {@link ByteArray}.
	 *
	 * @param operations keyset operations, can't be {@literal null}
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static Converter<ByteArray, ByteArray> create(KeysetOperations operations) {
		Assert.notNull(operations, "Keyset operations must not be null");
		return new EncryptionConverter(operations);
	}

	EncryptionConverter(@NonNull KeysetOperations operations) {
		super(ByteArray.class, ByteArray.class);
		this.operations = operations;
	}

	@Override
	public ByteArray from(ByteArray value) {
		if (value == null) {
			return null;
		}

		if (value.isEmpty()) {
			return ByteArray.empty();
		}

		try {
			return operations.decrypt(value);
		} catch (CryptoException e) {
			throw new DataTypeException("Error when decrypting data", e);
		}
	}

	@Override
	public ByteArray to(ByteArray value) {
		if (value == null) {
			return null;
		}

		if (value.isEmpty()) {
			return ByteArray.empty();
		}

		try {
			return operations.encrypt(value);
		} catch (CryptoException e) {
			throw new DataTypeException("Error when encrypting data", e);
		}
	}
}
