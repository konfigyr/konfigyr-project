package com.konfigyr.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.io.ByteArrayCodec;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Immutable value type that represents a namespace application OAuth {@code client_id}.
 * <p>
 * The value is encoded as {@code kfg-{base64url(32 bytes)}} where the 32-byte payload
 * is laid out as follows:
 * <pre>
 *  byte 0: format version ({@link #VERSION})
 *  byte 1: application type ({@link NamespaceClientType#code()})
 *  bytes 2–9: namespace entity ID, big-endian {@code long}
 *  bytes 10–17: creation timestamp, seconds since epoch, big-endian {@code long}
 *  bytes 18–31: 14 bytes of cryptographic random
 * </pre>
 * <p>
 * The version byte lets {@link #parse(String)} dispatch to a different decoder in a
 * future release without changing the {@code kfg-} prefix. The type byte means the
 * intended purpose of the client is always recoverable from the {@code client_id}
 * string alone, no database lookup needed. The timestamp makes IDs naturally sortable
 * by issuance time. The 14-byte random suffix (112 bits of entropy) prevents collisions
 * even among clients of the same type created within the same second.
 * <p>
 * This type is the single authoritative definition of how namespace client IDs are
 * generated and parsed. Any code that needs to create or interpret a {@code kfg-...}
 * client ID must go through this type.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see NamespaceClientType
 */
@NullMarked
@ValueObject
public final class NamespaceClientId implements Comparable<NamespaceClientId>, Supplier<String>, Serializable {

	@Serial
	private static final long serialVersionUID = 565544886068437745L;

	private static final ByteArrayCodec CODEC = ByteArrayCodec.BASE64_URL_SAFE_NO_PADDING;

	private static final Comparator<NamespaceClientId> COMPARATOR = Comparator
			.comparing(NamespaceClientId::namespace)
			.thenComparing(NamespaceClientId::timestamp);

	/**
	 * Prefix that all namespace client IDs begin with.
	 */
	public static final String PREFIX = "kfg-";

	/**
	 * Current format version embedded as byte 0 of the encoded payload.
	 * Incrementing this value in a future release allows {@link #parse(String)} to
	 * dispatch to a different decoder without changing the {@code kfg-} prefix.
	 */
	static final byte VERSION = 1;

	/** Total byte-buffer size. */
	static final int BUFFER_SIZE = 32;

	private final NamespaceClientType type;
	private final EntityId namespace;
	private final ByteArray bytes;
	private final String value;
	private final Instant timestamp;

	private NamespaceClientId(
			NamespaceClientType type,
			EntityId namespace,
			ByteArray bytes,
			String value,
			Instant timestamp) {
		Assert.notNull(type, "Namespace client type can not be null");
		Assert.notNull(namespace, "Namespace entity ID can not be null");
		Assert.notNull(bytes, "Payload bytes can not be null");
		Assert.state(bytes.size() == BUFFER_SIZE, () ->
				"Payload must be exactly " + BUFFER_SIZE + " bytes, got " + bytes.size());
		Assert.hasText(value, "Client ID value can not be blank");
		Assert.notNull(timestamp, "Creation timestamp can not be null");

		this.type = type;
		this.namespace = namespace;
		this.bytes = bytes;
		this.value = value;
		this.timestamp = timestamp;
	}

	/**
	 * Generates a new {@link NamespaceClientId} for the given namespace and client type.
	 * <p>
	 * Each call produces a unique value: the creation timestamp prevents collisions
	 * across different seconds, and the 14-byte random suffix prevents collisions
	 * within the same second.
	 *
	 * @param namespaceId entity identifier of the namespace, can't be {@literal null}
	 * @param type        intended purpose of the application, can't be {@literal null}
	 * @return new namespace client ID, never {@literal null}
	 */
	public static NamespaceClientId of(EntityId namespaceId, NamespaceClientType type) {
		Assert.notNull(namespaceId, "Namespace entity ID can not be null");
		Assert.notNull(type, "Namespace client type can not be null");

		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		final ByteArray payload = ByteArray.from(ByteBuffer.allocate(BUFFER_SIZE)
				.put(VERSION)
				.put(type.code())
				.putLong(namespaceId.get())
				.putLong(now.getEpochSecond())
				.put(KeyGenerators.secureRandom(14).generateKey()));

		return new NamespaceClientId(type, namespaceId, payload, PREFIX + payload.encode(CODEC), now);
	}

	/**
	 * Parses a {@link NamespaceClientId} from the given {@code client_id} string.
	 *
	 * @param clientId the full {@code kfg-...} client ID string, can't be {@literal null}
	 * @return parsed namespace client ID, never {@literal null}
	 * @throws IllegalArgumentException if the string is not a valid namespace client ID
	 */
	@JsonCreator
	public static NamespaceClientId parse(String clientId) {
		Assert.hasText(clientId, "Namespace client ID can not be blank");

		if (!clientId.startsWith(PREFIX)) {
			throw new IllegalArgumentException("Invalid namespace client ID prefix: " + clientId);
		}

		final ByteArray payload;

		try {
			payload = ByteArray.decode(clientId.substring(PREFIX.length()), CODEC);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid namespace client ID encoding: " + clientId, ex);
		}

		if (payload.size() != BUFFER_SIZE) {
			throw new IllegalArgumentException("Invalid namespace client ID length: " + clientId);
		}

		final ByteBuffer buffer = ByteBuffer.wrap(payload.array());
		final byte version = buffer.get();

		if (version != VERSION) {
			throw new IllegalArgumentException(
					"Unsupported namespace client ID version " + version + ": " + clientId);
		}

		final NamespaceClientType type;

		try {
			type = NamespaceClientType.of(buffer.get());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unsupported namespace client ID type in: " + clientId, ex);
		}

		final EntityId namespaceId = EntityId.from(buffer.getLong());
		final Instant timestamp = Instant.ofEpochSecond(buffer.getLong());

		return new NamespaceClientId(type, namespaceId, payload, clientId, timestamp);
	}

	/**
	 * Returns {@code true} when the given string begins with the {@link #PREFIX namespace
	 * client ID prefix}. This is an inexpensive guard check; it does not validate the payload
	 * length, version byte, or encoding. Use {@link #tryParse(String)} when you need a
	 * fully validated result.
	 *
	 * @param clientId the string to inspect, may be {@literal null}
	 * @return {@code true} if the string starts with {@value #PREFIX}
	 */
	public static boolean isPotentialClientId(@Nullable String clientId) {
		return clientId != null && clientId.startsWith(PREFIX);
	}

	/**
	 * Attempts to parse a {@link NamespaceClientId} from the given string, returning an
	 * empty {@link Optional} for any input that is not a valid namespace client ID.
	 *
	 * @param clientId the client ID string to parse, may be {@literal null}
	 * @return parsed namespace client ID, or empty if the string is not valid
	 */
	public static Optional<NamespaceClientId> tryParse(@Nullable String clientId) {
		if (!isPotentialClientId(clientId)) {
			return Optional.empty();
		}

		try {
			return Optional.of(parse(clientId));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	/**
	 * Returns the {@link NamespaceClientType type} that describes the intended purpose
	 * of this application and the OAuth2 security profile it operates under.
	 *
	 * @return client type, never {@literal null}
	 */
	public NamespaceClientType type() {
		return type;
	}

	/**
	 * Returns the {@link EntityId} of the namespace that owns this client.
	 *
	 * @return namespace entity identifier, never {@literal null}
	 */
	public EntityId namespace() {
		return namespace;
	}

	/**
	 * Returns the decoded 32-byte payload of this client ID.
	 *
	 * @return payload bytes, never {@literal null}
	 */
	public ByteArray bytes() {
		return bytes;
	}

	/**
	 * Returns the instant at which this client ID was generated, truncated to
	 * second precision.
	 *
	 * @return creation timestamp, never {@literal null}
	 */
	public Instant timestamp() {
		return timestamp;
	}

	@Override
	public String get() {
		return value;
	}

	@Override
	public int compareTo(NamespaceClientId o) {
		return COMPARATOR.compare(this, o);
	}

	/**
	 * Two {@link NamespaceClientId} instances are equal when their {@link #get()} strings
	 * match — all other components are derived from the value, so it is the canonical key.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NamespaceClientId other)) {
			return false;
		}
		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	@JsonValue
	public String toString() {
		return get();
	}
}
