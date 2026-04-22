package com.konfigyr.audit;

import com.konfigyr.data.CursorPageable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Opaque cursor token used by the {@link AuditEventRepository} for bidirectional,
 * cursor-based pagination over audit records.
 * <p>
 * Each token encodes the position of the last-seen record so that the next page can
 * be fetched without offset-based skipping. A reversed flag indicates backward navigation.
 * <p>
 * Encoding scheme (26 bytes, 52 hex chars):
 * {@code VERSION (1B) | REVERSED (1B) | UUID MSB (8B) | UUID LSB (8B) | TIMESTAMP_MILLIS (8B)}
 *
 * @param value hex-encoded token string, can't be {@literal null}.
 * @param identifier the record UUID used for cursor positioning, can't be {@literal null}.
 * @param reversed whether this token navigates backward.
 * @param timestamp the record timestamp used for cursor positioning, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
record AuditCursorToken(String value, UUID identifier, boolean reversed, OffsetDateTime timestamp) {

	private static final byte VERSION = 0x1;

	static AuditCursorToken of(UUID identifier, OffsetDateTime timestamp) {
		return of(identifier, timestamp, false);
	}

	static AuditCursorToken of(UUID identifier, OffsetDateTime timestamp, boolean reversed) {
		final ByteBuffer buffer = ByteBuffer.allocate(26);
		buffer.put(VERSION);
		buffer.put(reversed ? (byte) 0x1 : (byte) 0x0);
		buffer.putLong(identifier.getMostSignificantBits());
		buffer.putLong(identifier.getLeastSignificantBits());
		buffer.putLong(timestamp.toInstant().toEpochMilli());
		return new AuditCursorToken(Hex.encodeHexString(buffer.array()), identifier, reversed, timestamp);
	}

	@Nullable
	static AuditCursorToken decode(CursorPageable pageable) {
		final String token = pageable.token();
		return StringUtils.isBlank(token) ? null : AuditCursorToken.decode(token);
	}

	static AuditCursorToken decode(String encoded) {
		if (encoded.length() != 52) {
			throw new IllegalArgumentException("Invalid cursor token of: " + encoded);
		}

		final ByteBuffer buffer;

		try {
			buffer = ByteBuffer.wrap(Hex.decodeHex(encoded));
		} catch (DecoderException ex) {
			throw new IllegalArgumentException("Invalid cursor token of: " + encoded, ex);
		}

		// skip version
		buffer.get();
		// check if this is a reversed token
		final byte reversed = buffer.get();

		// read UUID most/least significant bits and timestamp from the buffer
		final long mostSigBits = buffer.getLong();
		final long leastSigBits = buffer.getLong();
		final long timestamp = buffer.getLong();

		if (timestamp < 0) {
			throw new IllegalArgumentException("Invalid cursor token of: " + encoded);
		}

		return new AuditCursorToken(encoded, new UUID(mostSigBits, leastSigBits), reversed != 0,
				Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC));
	}

}
