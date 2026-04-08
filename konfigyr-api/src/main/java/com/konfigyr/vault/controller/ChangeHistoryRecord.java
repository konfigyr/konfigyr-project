package com.konfigyr.vault.controller;

import com.konfigyr.vault.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

record ChangeHistoryRecord(
		String id,
		String revision,
		String name,
		PropertyTransitionType action,
		String from,
		String to,
		String appliedBy,
		OffsetDateTime appliedAt
) {

	static ChangeHistoryRecord from(PropertyHistory history, Vault vault) {
		return new ChangeHistoryRecord(
				idFor(history.revision(), history.name()),
				history.revision(),
				history.name(),
				history.action(),
				unseal(vault, history.from()),
				unseal(vault, history.to()),
				history.appliedBy(),
				history.appliedAt()
		);
	}

	private static String idFor(String revisionNumber, String propertyName) {
		final byte[] revision = revisionNumber.getBytes(StandardCharsets.UTF_8);
		final byte[] name = propertyName.getBytes(StandardCharsets.UTF_8);

		byte[] input = new byte[revision.length + name.length];
		System.arraycopy(revision, 0, input, 0, revision.length);
		System.arraycopy(name, 0, input, revision.length, name.length);

		final long[] hash = MurmurHash3.hash128x64(input);
		final ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.putLong(hash[0]);
		buffer.putLong(hash[1]);

		return Hex.encodeHexString(buffer.array());
	}

	private static String unseal(Vault vault, PropertyValue value) {
		if (value == null) {
			return null;
		}
		final PropertyValue unsealed = vault.unseal(value);
		return new String(unsealed.get().array(), StandardCharsets.UTF_8);
	}

}
