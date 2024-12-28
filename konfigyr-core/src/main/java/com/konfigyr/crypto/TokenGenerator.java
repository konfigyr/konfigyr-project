package com.konfigyr.crypto;

import io.hypersistence.tsid.TSID;
import org.apache.commons.codec.binary.Base16;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Implementation of the {@link StringKeyGenerator} that would generate a unique Base16 encoded token
 * that is consisted out of two parts:
 * <ul>
 *     <li>{@link TSID} unique identifier - 8 bytes</li>
 *     <li>Random bytes by {@link java.security.SecureRandom} - length is specified in the constructor</li>
 * </ul>
 * The resulting bytes are merged and encoded using {@link Base16} encoder using lowercase characters.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TSID
 */
public final class TokenGenerator implements StringKeyGenerator {

	static final Supplier<StringKeyGenerator> instance = SingletonSupplier.of(TokenGenerator::new);

	/**
	 * Creates a new {@link TokenGenerator} with a random key part length of <code>8</code>. The resulting
	 * token would be consisted of <code>32</code> lowercase alphanumeric characters.
	 *
	 * @return token key generator, never {@literal null}
	 */
	public static StringKeyGenerator getInstance() {
		return instance.get();
	}

	/**
	 * Creates a new {@link TokenGenerator} with a specified random key part length of <code>8</code>.
	 * The resulting token would be consisted of <code>16 + (${length} * 2)</code> lowercase alphanumeric
	 * characters.
	 *
	 * @param size the byte size of the random part of the token, must be between 8 and 256
	 * @return token key generator, never {@literal null}
	 */
	public static StringKeyGenerator getInstance(int size) {
		return new TokenGenerator(size);
	}

	private final BytesKeyGenerator generator;
	private final Base16 encoder = new Base16(true);

	private TokenGenerator() {
		this(8);
	}

	private TokenGenerator(int size) {
		Assert.isTrue(size >= 8, "Key length must be greater than or equal to 8");
		Assert.isTrue(size <= 256, "Key length must be less than or equal to 256");

		this.generator = KeyGenerators.secureRandom(size);
	}

	@Override
	public String generateKey() {
		final byte[] id = TSID.fast().toBytes();
		final byte[] key = generator.generateKey();

		final ByteBuffer buffer = ByteBuffer.allocate(id.length + key.length)
				.put(id)
				.put(key);

		return encoder.encodeAsString(buffer.array());
	}

}
