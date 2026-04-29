package com.konfigyr.vault.history;

import com.konfigyr.data.CursorPageable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@NullMarked
@ValueObject
record HistoryCursorToken(String value, UUID identifier, boolean reversed, OffsetDateTime timestamp) {

	private static final byte VERSION = 0x1;

	static HistoryCursorToken of(UUID identifier, OffsetDateTime timestamp) {
		return of(identifier, timestamp, false);
	}

	static HistoryCursorToken of(UUID identifier, OffsetDateTime timestamp, boolean reversed) {
		final ByteBuffer buffer = ByteBuffer.allocate(26);
		buffer.put(VERSION);
		buffer.put(reversed ? (byte) 0x1 : (byte) 0x0);
		buffer.putLong(identifier.getMostSignificantBits());
		buffer.putLong(identifier.getLeastSignificantBits());
		buffer.putLong(timestamp.toInstant().toEpochMilli());
		return new HistoryCursorToken(Hex.encodeHexString(buffer.array()), identifier, reversed, timestamp);
	}

	@Nullable
	static HistoryCursorToken decode(CursorPageable pageable) {
		final String token = pageable.token();
		return token == null ? null : decode(token);
	}

	static HistoryCursorToken decode(String encoded) {
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

		return new HistoryCursorToken(encoded, new UUID(mostSigBits, leastSigBits), reversed != 0,
				Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC));
	}

}
