package com.konfigyr.data.converter;

import com.konfigyr.crypto.CryptoException;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;
import org.jooq.impl.AbstractConverter;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link Converter jOOQ Converter} that would perform encryption operations on
 * the {@link ByteArray} using {@link KeysetOperations}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class EncryptionConverter extends AbstractConverter<ByteArray, ByteArray> {

	private final KeysetOperations operations;
	private final ByteArray context;

	/**
	 * Creates a new {@link EncryptionConverter} with the {@link KeysetOperations} that would perform
	 * encrypt or decrypt operations on the underlying {@link ByteArray}.
	 *
	 * @param operations keyset operations, can't be {@literal null}
	 * @return Encryption converter, never {@literal null}
	 */
	@NonNull
	public static EncryptionConverter create(KeysetOperations operations) {
		return create(operations, null);
	}

	/**
	 * Creates a new {@link EncryptionConverter} with the {@link KeysetOperations} that would perform
	 * encrypt or decrypt operations on the underlying {@link ByteArray}. The operations would be executed
	 * with the given authentication context.
	 *
	 * @param operations keyset operations, can't be {@literal null}
	 * @param context authentication context, can be {@literal null}
	 * @return Encryption converter, never {@literal null}
	 */
	@NonNull
	public static EncryptionConverter create(KeysetOperations operations, ByteArray context) {
		Assert.notNull(operations, "Keyset operations must not be null");
		return new EncryptionConverter(operations, context);
	}

	EncryptionConverter(@NonNull KeysetOperations operations, @Nullable ByteArray context) {
		super(ByteArray.class, ByteArray.class);
		this.operations = operations;
		this.context = context;
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
			return operations.decrypt(value, context);
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
			return operations.encrypt(value, context);
		} catch (CryptoException e) {
			throw new DataTypeException("Error when encrypting data", e);
		}
	}

	/**
	 * Creates a new {@link EncryptionConverter} with the given authentication context.
	 *
	 * @param context authentication context as a plain string, can be {@literal null}
	 * @return Encryption converter, never {@literal null}
	 */
	@NonNull
	public EncryptionConverter with(@Nullable String context) {
		return new EncryptionConverter(operations, StringUtils.hasText(context) ? ByteArray.fromString(context) : null);
	}

	/**
	 * Creates a new {@link EncryptionConverter} with the given authentication context.
	 *
	 * @param context authentication context, can be {@literal null}
	 * @return Encryption converter, never {@literal null}
	 */
	@NonNull
	public EncryptionConverter with(@Nullable ByteArray context) {
		return new EncryptionConverter(operations, context);
	}

}
