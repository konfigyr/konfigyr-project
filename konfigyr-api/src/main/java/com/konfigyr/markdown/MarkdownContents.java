package com.konfigyr.markdown;

import com.konfigyr.io.ByteArray;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Immutable value type representing raw Markdown content paired with its {@code SHA-256} checksum.
 * <p>
 * Instances are created exclusively via the {@link #of(String)} factory method, which guarantees
 * that the checksum is always derived from the content and never accepted from an external source.
 * This makes the checksum safe to use as a cache key without any additional validation.
 * <p>
 * Implements {@link CharSequence} so that {@code MarkdownContents} can be passed directly to APIs
 * that accept plain text, such as Markdown parsers or loggers, without unwrapping the value explicitly.
 * <p>
 * Equality and hashing are based on the checksum rather than the string value. Since the checksum is
 * a deterministic function of the content, this is semantically equivalent but significantly cheaper
 * for large documents.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@EqualsAndHashCode(of = "checksum")
public final class MarkdownContents implements CharSequence {

	/**
	 * The raw Markdown string exactly as provided by the author, never {@code null}.
	 */
	private final String value;

	/**
	 * 32 byte long byte array that contains the SHA-256 digest of {@link #value} encoded as UTF-8.
	 */
	private final ByteArray checksum;

	/**
	 * Creates a new {@code MarkdownContents} instance with the given raw Markdown string, and
	 * it's checksum value.
	 *
	 * @param value the raw Markdown string, must not be {@code null}
	 * @param checksum the checksum value, must not be {@code null} and must be a 32-byte long array
	 */
	public MarkdownContents(String value, byte[] checksum) {
		this(value, new ByteArray(checksum));
	}

	/**
	 * Creates a new {@code MarkdownContents} instance with the given raw Markdown string, and
	 * it's checksum value.
	 *
	 * @param value the raw Markdown string, must not be {@code null}
	 * @param checksum the checksum value, must not be {@code null} and must be a 32-byte long array
	 */
	public MarkdownContents(String value, ByteArray checksum) {
		Assert.notNull(value, "Markdown contents can not be null");
		Assert.notNull(checksum, "Markdown checksum can not be null");
		Assert.isTrue(checksum.size() == 32, "Markdown checksum must be a 32-byte SHA-256 digest");

		this.value = value;
		this.checksum = checksum;
	}

	/**
	 * Creates a new {@code MarkdownContents} from the given raw Markdown string.
	 * <p>
	 * The {@code SHA-256} checksum is computed from {@code value} encoded as {@code UTF-8}. The checksum
	 * is used as a stable cache key by {@link MarkdownParser} implementations, as identical content
	 * always produces the same checksum.
	 *
	 * @param value the raw Markdown string; must not be {@code null}
	 * @return a new {@code MarkdownContents} instance
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 * @throws IllegalStateException if the {@code SHA-256} digest algorithm is not supported
	 */
	public static MarkdownContents of(String value) {
		try {
			final byte[] checksum = MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));

			return new MarkdownContents(value, checksum);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Could not create Markdown contents as digest algorithm is not supported", e);
		}
	}

	/**
	 * Returns the raw Markdown string as provided at construction time.
	 *
	 * @return the raw Markdown content, never {@code null}.
	 */
	public String value() {
		return value;
	}

	/**
	 * Returns the 32 byte long byte array that contains the SHA-256 digest of {@link #value}
	 * encoded as {@code UTF-8}.
	 *
	 * @return a 32-byte SHA-256 digest, never {@code null}.
	 */
	public ByteArray checksum() {
		return checksum;
	}

	/**
	 * Returns the number of characters in the raw Markdown string.
	 *
	 * @return the length of {@link #value()}
	 */
	@Override
	public int length() {
		return value.length();
	}

	/**
	 * Returns the character at the specified index in the raw Markdown string.
	 *
	 * @param index the index of the character to return
	 * @return the character at {@code index}
	 * @throws IndexOutOfBoundsException if {@code index} is negative or not less than {@link #length()}
	 */
	@Override
	public char charAt(int index) {
		return value.charAt(index);
	}

	/**
	 * Returns a {@link CharSequence} that is a subsequence of the raw Markdown string.
	 *
	 * @param start the start index, inclusive
	 * @param end   the end index, exclusive
	 * @return the specified subsequence
	 * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative, if {@code end} is
	 * greater than {@link #length()}, or if {@code start} is greater than {@code end}
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		return value.subSequence(start, end);
	}

	/**
	 * Returns the raw Markdown string.
	 * <p>
	 * This allows {@code MarkdownContents} to be used transparently wherever a {@link String}
	 * or {@link CharSequence} is expected.
	 *
	 * @return the raw Markdown content, never {@code null}.
	 */
	@Override
	public String toString() {
		return value;
	}

}
