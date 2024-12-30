package com.konfigyr.data.converter;

import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.jooq.impl.AbstractConverter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.security.*;

/**
 * Implementation of the {@link Converter jOOQ Converter} that would perform {@link MessageDigest} operations on
 * the {@link ByteArray}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class MessageDigestConverter extends AbstractConverter<ByteArray, ByteArray> {

	private final MessageDigest digest;

	/**
	 * Creates a new {@link MessageDigestConverter} with the {@link MessageDigest} algorithm name
	 * that would perform hashing operations on the underlying {@link ByteArray}.
	 *
	 * @param algorithm the name of the algorithm requested
	 * @return JSON converter, never {@literal null}
	 * @see MessageDigest
	 */
	@NonNull
	public static Converter<ByteArray, ByteArray> create(String algorithm) {
		try {
			return create(MessageDigest.getInstance(algorithm));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Could not create converter as digest algorithm is not supported", e);
		}
	}

	/**
	 * Creates a new {@link MessageDigestConverter} with the {@link MessageDigest} algorithm name and
	 * provider that would perform hashing operations on the underlying {@link ByteArray}.
	 *
	 * @param algorithm the name of the algorithm requested
	 * @param provider the name of the algorithm provider
	 * @return JSON converter, never {@literal null}
	 * @see MessageDigest
	 */
	@NonNull
	public static Converter<ByteArray, ByteArray> create(String algorithm, String provider) {
		try {
			return create(MessageDigest.getInstance(algorithm, provider));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Could not create converter as digest algorithm is not supported", e);
		} catch (NoSuchProviderException e) {
			throw new IllegalArgumentException("Could not create converter as security provider is not supported", e);
		}
	}

	/**
	 * Creates a new {@link MessageDigestConverter} with the {@link MessageDigest} that would perform
	 * hashing operations on the underlying {@link ByteArray}.
	 *
	 * @param digest message digest, can't be {@literal null}
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static Converter<ByteArray, ByteArray> create(MessageDigest digest) {
		Assert.notNull(digest, "Message digest must not be null");
		return new MessageDigestConverter(digest);
	}

	MessageDigestConverter(@NonNull MessageDigest digest) {
		super(ByteArray.class, ByteArray.class);
		this.digest = digest;
	}

	@Override
	public ByteArray from(ByteArray value) {
		return value;
	}

	@Override
	public ByteArray to(ByteArray value) {
		if (value == null) {
			return null;
		}

		if (value.isEmpty()) {
			return ByteArray.empty();
		}

		final byte[] hash = digest.digest(value.array());
		return new ByteArray(hash);
	}

}
