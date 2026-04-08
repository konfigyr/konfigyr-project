package com.konfigyr.vault.history;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@NullMarked
@ValueObject
record HistoryCursorToken(String value, long identifier, boolean reversed, OffsetDateTime timestamp) {

	private static final byte VERSION = 0x1;

	static HistoryCursorToken of(long identifier, OffsetDateTime timestamp) {
		return of(identifier, timestamp, false);
	}

	static HistoryCursorToken of(long identifier, OffsetDateTime timestamp, boolean reversed) {
		final ByteBuffer buffer = ByteBuffer.allocate(18);
		buffer.put(VERSION);
		buffer.put(reversed ? (byte) 0x1 : (byte) 0x0);
		buffer.putLong(identifier);
		buffer.putLong(timestamp.toInstant().toEpochMilli());
		return new HistoryCursorToken(Hex.encodeHexString(buffer.array()), identifier, reversed, timestamp);
	}

	static HistoryCursorToken decode(String encoded) {
		if (encoded.length() != 36) {
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

		// read identifier and timestamp from the buffer...
		final long identifier = buffer.getLong();
		final long timestamp = buffer.getLong();

		if (identifier < 0 || timestamp < 0) {
			throw new IllegalArgumentException("Invalid cursor token of: " + encoded);
		}

		return new HistoryCursorToken(encoded, identifier, reversed != 0,
				Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC));
	}

}
