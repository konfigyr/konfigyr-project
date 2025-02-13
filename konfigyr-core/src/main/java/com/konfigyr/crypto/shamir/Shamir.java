package com.konfigyr.crypto.shamir;

import com.konfigyr.io.ByteArray;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * An implementation of Shamir's Secret Sharing over {@code GF(256)} to securely split secrets into
 * {@code N} parts, of which any {@code K} can be joined to recover the original secret.
 * <p>
 * {@link Shamir} uses the same GF(256) field polynomial as the Advanced Encryption Standard
 * (AES): {@code 0x11b}, or {@code x}<sup>8</sup> + {@code x}<sup>4</sup> + {@code x}<sup>3</sup> +
 * {@code x} + 1.
 * <p>
 * This implementation has been borrowed from <a href="https://github.com/codahale/shamir">
 * codahale/shamir</a> GitHub Repository.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Shamir%27s_Secret_Sharing">Shamir's Secret Sharing</a>
 * @see <a href="http://www.cs.utsa.edu/~wagner/laws/FFM.html">The Finite Field {@code GF(256)}</a>
 * @see <a href="https://github.com/codahale/shamir">Original source code</a>
 */
public final class Shamir {

	private static final Supplier<Shamir> instance = SingletonSupplier.of(() -> new Shamir(10, 3));

	private final SecureRandom random;
	private final int shares;
	private final int parts;

	/**
	 * Returns the default instance to be used by the Konfigyr Application. This instance is using the
	 * following configuration:
	 * <ul>
	 *     <li>Total of {@link Share shares} generated: <code>10</code></li>
	 *     <li>Number of {@link Share shares} required to recover the secret key: <code>3</code></li>
	 * </ul>
	 *
	 * @return the default {@link Shamir} instance, never {@literal null}
	 */
	@NonNull
	public static Shamir getInstance() {
		return instance.get();
	}

	public Shamir(int shares, int parts) {
		this(new SecureRandom(), shares, parts);
	}

	public Shamir(SecureRandom random, int shares, int parts) {
		Assert.notNull(random, "Secure random generator must not be null");
		Assert.isTrue(parts > 1, () -> "Number of shares needed to recover the secret must be greater than 1");
		Assert.isTrue(shares > parts, () -> "Number of generated shares needs to greater than " + parts);
		Assert.isTrue(shares <= 255, () -> "Number of generated shares can not be greater than 255");

		this.random = random;
		this.shares = shares;
		this.parts = parts;
	}

	/**
	 * Generates a random 256 bit long secret into {@code n} {@link Share parts}, of which any {@code k}
	 * or more can be combined to recover the original secret.
	 *
	 * @return a list of generated {@link Share shares}, never {@literal null}
	 */
	@NonNull
	public List<Share> generate() {
		return generate(32);
	}

	/**
	 * Generates a random secret with given byte length into {@code n} {@link Share parts}, of which any {@code k}
	 * or more can be combined to recover the original secret.
	 *
	 * @param length byte length, or size, of the generated secret to be split into parts
	 * @return a list of generated {@link Share shares}, never {@literal null}
	 */
	@NonNull
	public List<Share> generate(final int length) {
		final byte[] secret = KeyGenerators.secureRandom(length).generateKey();

		return split(secret);
	}

	/**
	 * Splits the given secret into {@code n} {@link Share parts}, of which any {@code k} or more
	 * can be combined to recover the original secret.
	 *
	 * @param secret secret that should be split in {@code n} parts
	 * @return a list of generated {@link Share shares}, never {@literal null}
	 * @throws IllegalArgumentException when secret has less than 32 bytes in size
	 */
	@NonNull
	public List<Share> split(final byte[] secret) {
		return split(new ByteArray(secret));
	}

	/**
	 * Splits the given secret into {@code n} {@link Share parts}, of which any {@code k} or more
	 * can be combined to recover the original secret.
	 *
	 * @param secret secret that should be split in {@code n} parts
	 * @return a list of generated {@link Share shares}, never {@literal null}
	 * @throws IllegalArgumentException when secret has less than 32 bytes in size
	 */
	@NonNull
	public List<Share> split(final ByteArray secret) {
		Assert.isTrue(secret.size() >= 32, () -> "Your secret must be at least 32 bytes or 128 bits long");

		final byte[][] values = new byte[shares][secret.size()];

		for (int i = 0; i < secret.size(); i++) {
			// for each byte, generate a random polynomial, p
			final byte[] p = GF256.generate(random, parts - 1, secret.array()[i]);
			for (int x = 1; x <= shares; x++) {
				// each part's byte is p(partId)
				values[x - 1][i] = GF256.eval(p, (byte) x);
			}
		}

		// return as a set of objects
		final List<Share> parts = new ArrayList<>(shares);
		for (int i = 0; i < values.length; i++) {
			parts.add(new Share(i + 1, values[i]));
		}
		return Collections.unmodifiableList(parts);
	}

	/**
	 * Joins the given {@link Share shared parts} to recover the original secret.
	 * <p>
	 * There is no way to determine if the returned value is actually the original secret. If the parts are
	 * incorrect, or are under the threshold value used to split the secret, a random value will be returned.
	 *
	 * @param shares a list of shares used to recover the secret, can not be {@literal null} or empty
	 * @return the original secret
	 * @throws IllegalArgumentException if {@code shares} are empty or contains values of varying lengths
	 */
	public ByteArray join(final List<Share> shares) {
		Assert.notNull(shares, "Shares list must not be null");
		Assert.isTrue(shares.size() >= parts, () -> "You must provide at least " + parts + " shares to recover the secret");

		final int[] lengths = shares.stream()
				.mapToInt(Share::length)
				.distinct()
				.toArray();

		Assert.isTrue(lengths.length == 1, "Varying lengths of shared values");

		final byte[] secret = new byte[lengths[0]];

		for (int i = 0; i < secret.length; i++) {
			final byte[][] points = new byte[shares.size()][2];

			int j = 0;
			for (final Share share : shares) {
				points[j][0] = Integer.valueOf(share.index()).byteValue();
				points[j][1] = share.value().array()[i];
				j++;
			}

			secret[i] = GF256.interpolate(points);
		}

		return new ByteArray(secret);
	}
}
