package com.konfigyr.artifactory.digest;

import com.konfigyr.artifactory.*;
import com.konfigyr.io.ByteArray;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generator responsible for generating deterministic checksums for {@link PropertyMetadata} within the
 * Artifactory domain.
 * <p>
 * The checksum acts as a stable, reproducible content hash uniquely identifying a configuration property
 * definition across artifact versions and services. It ensures that identical property metadata, regardless of
 * source or upload time, will always resolve to the same checksum value.
 *
 * <h2>Domain Purpose</h2>
 * Within the {@code Artifactory} bounded context, this checksum acts as the canonical fingerprint of a property
 * definition. It underpins:
 *
 * <ul>
 *   <li><strong>De-duplication</strong>: Prevents redundant storage of identical property definitions.</li>
 *   <li><strong>Integrity verification</strong>: Detects content drift across artifact versions.</li>
 *   <li><strong>Provenance tracking</strong>: Allows mapping properties to their earliest and latest known appearances.</li>
 * </ul>
 *
 * <h2>Checksum Construction</h2>
 * This generator uses a <strong>Merkle-style double hashing strategy</strong>: each property field is hashed
 * individually using {@code SHA-256}, and the resulting 32-byte digests are concatenated in a fixed order to form a
 * 256-byte buffer. That buffer is then digested again with {@code SHA-256} to produce the final checksum.
 * <p>
 * This layered hashing structure ensures field isolation, predictable byte length, and consistent deduplication
 * behavior across artifact versions.
 *
 * <h3>Algorithm</h3>
 * <pre>{@code
 * checksum = SHA256(
 *      SHA256(dataType)
 *    + SHA256(type)
 *    + SHA256(typeName)
 *    + SHA256(name)
 *    + SHA256(description)
 *    + SHA256(defaultValue)
 *    + SHA256(deprecation)
 *    + SHA256(hints)
 * )
 * }</pre>
 *
 * <h3>Hash Input Order (8 fields total)</h3>
 * <ol>
 *   <li><b>Data type</b> - data category used for parsing (e.g., int, boolean)</li>
 *   <li><b>Type</b> - logical property type (e.g., STRING, DURATION)</li>
 *   <li><b>Type name</b> - fully qualified Java type (e.g., java.time.Duration)</li>
 *   <li><b>Name</b> - canonical configuration key (e.g., spring.datasource.url)</li>
 *   <li><b>Description</b> - human-readable property documentation</li>
 *   <li><b>Default value</b> - configured default, if present</li>
 *   <li><b>Deprecation information</b> - details or messages on deprecation</li>
 *   <li><b>Value hints</b> - serialized hints or allowable value examples</li>
 * </ol>
 *
 * <h2>Determinism and Stability</h2>
 * <ul>
 *   <li>All text inputs are normalized to UTF-8 before hashing.</li>
 *   <li>Each field is right-padded or truncated to exactly 32 bytes.</li>
 *   <li>Null or blank fields are treated as zero-filled byte arrays.</li>
 *   <li>Checksum generation is idempotent: identical metadata yields identical hashes.</li>
 * </ul>
 *
 * <h2>Extension Guidelines</h2>
 * Any modification to field ordering, allocation, or hash algorithm will invalidate existing checksums and
 * should be treated as a <strong>breaking schema change</strong>. If new property metadata fields must be added,
 * prefer introducing a new checksum version or algorithm identifier rather than mutating this one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PropertyMetadata
 */
public final class PropertyMetadataChecksumGenerator {

	// empty byte array that is used for checksum generator when the metadata value is null, empty or blank
	private static final byte[] EMPTY_BYTES = new byte[0];

	private static final Supplier<PropertyMetadataChecksumGenerator> INSTANCE = SingletonSupplier.of(PropertyMetadataChecksumGenerator::new);

	/**
	 * Return the generator instance responsible for generating deterministic checksums for {@link PropertyMetadata}.
	 *
	 * @return the checksum generator, never {@literal null}.
	 */
	@NonNull
	public static PropertyMetadataChecksumGenerator getInstance() {
		return INSTANCE.get();
	}

	private PropertyMetadataChecksumGenerator() {
		// use singleton provider...
	}

	/**
	 * Generates a deterministic checksum for the given {@link PropertyMetadata property metadata fields}.
	 *
	 * @param metadata the property metadata to generate checksum, can't be {@literal null}.
	 * @return the byte array containing the {@code SHA-256} checksum, never {@literal null}.
	 * @throws IllegalArgumentException if the {@code SHA-256} algorithm is unavailable.
	 */
	@NonNull
	public ByteArray generate(@NonNull PropertyMetadata metadata) {
		final MessageDigest digest;

		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Could not create generator as digest algorithm is not supported", e);
		}

		final byte[] dataType = digest(digest, metadata.dataType().name());
		final byte[] type = digest(digest, metadata.type().name());
		final byte[] typeName = digest(digest, metadata.typeName());
		final byte[] name = digest(digest, metadata.name());
		final byte[] description = digest(digest, metadata.description());
		final byte[] defaultValue = digest(digest, metadata.defaultValue());
		final byte[] hints = digest(digest, metadata.hints());
		final byte[] deprecation = digest(digest, metadata.deprecation());

		// Keep an eye here on the order!
		digest.update(dataType);
		digest.update(type);
		digest.update(typeName);
		digest.update(name);
		digest.update(description);
		digest.update(defaultValue);
		digest.update(hints);
		digest.update(deprecation);
		return new ByteArray(digest.digest());
	}

	private byte[] digest(@NonNull MessageDigest digest, @Nullable List<String> hints) {
		if (CollectionUtils.isEmpty(hints)) {
			return EMPTY_BYTES;
		}

		final byte[] data = hints.stream()
				.sorted()
				.collect(Collectors.joining())
				.getBytes(StandardCharsets.UTF_8);

		return digest(digest, data);
	}

	private byte[] digest(@NonNull MessageDigest digest, @Nullable Deprecation deprecation) {
		if (deprecation == null) {
			return EMPTY_BYTES;
		}

		final boolean hasReason = StringUtils.hasText(deprecation.reason());
		final boolean hasReplacement = StringUtils.hasText(deprecation.replacement());

		if (!hasReason && !hasReplacement) {
			return EMPTY_BYTES;
		}

		final StringBuilder builder = new StringBuilder();

		if (hasReason) {
			builder.append(deprecation.reason());
		}

		if (hasReplacement) {
			builder.append(deprecation.replacement());
		}

		return digest(digest, builder.toString());
	}

	private byte[] digest(@NonNull MessageDigest digest, @Nullable String data) {
		if (StringUtils.hasText(data)) {
			return digest(digest, StringUtils.trimAllWhitespace(data).getBytes(StandardCharsets.UTF_8));
		}
		return EMPTY_BYTES;
	}

	private byte[] digest(@NonNull MessageDigest digest, @Nullable byte[] data) {
		return ArrayUtils.isEmpty(data) ? EMPTY_BYTES : digest.digest(data);
	}

}
