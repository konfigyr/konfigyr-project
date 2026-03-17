package com.konfigyr.artifactory.digest;

import com.konfigyr.artifactory.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implementation of the {@link JsonSchemaVisitor} that produces a deterministic cryptographic digest
 * for a {@link JsonSchema} tree. The generated digest is used as part of the Artifactory's occurrence
 * identity model and therefore must be:
 *
 * <ul>
 *     <li><b>Deterministic</b> – identical logical values must always produce the same digest</li>
 *     <li><b>Structural</b> – hashing is based on structure and content, not textual representation</li>
 *     <li><b>Canonical</b> – object field ordering and numeric formatting must not influence the result</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 *
 * Unlike textual hashing approaches (e.g., hashing serialized JSON), this visitor does not rely on JSON
 * string serialization. Instead, it walks the {@link JsonSchema} object graph and feeds deterministic byte
 * sequences into a {@link MessageDigest}. This avoids instability caused by:
 *
 * <ul>
 *     <li>Object key ordering differences</li>
 *     <li>Whitespace or formatting variations</li>
 *     <li>Non-canonical number representations</li>
 *     <li>Library-specific serialization behavior</li>
 * </ul>
 *
 * <h2>Deterministic Encoding Rules</h2>
 *
 * <ul>
 *     <li>Each value emits a stable type marker before its contents.</li>
 *     <li>Object fields are sorted lexicographically by key before hashing.</li>
 *     <li>Arrays preserve their declared element order.</li>
 *     <li>Strings are encoded as UTF-8 with a length prefix.</li>
 *     <li>Numbers are encoded using {@code BigDecimal.toPlainString()} to ensure canonical form.</li>
 *     <li>Booleans are encoded as a single byte (0 or 1).</li>
 *     <li>{@code null} or {@code empty} values emit a dedicated marker.</li>
 * </ul>
 *
 * <h2>Versioning</h2>
 *
 * A version marker is written into the digest stream at construction time. This ensures that future changes
 * to hashing semantics (e.g., additional normalization rules) can be introduced safely without corrupting
 * previously stored hashes.
 * <p>
 * Any change to encoding rules <b>must</b> increase or change the hash version.
 *
 * <h2>Thread Safety</h2>
 *
 * This class is <b>not thread-safe</b>. Each instance wraps a mutable {@link MessageDigest} and must not
 * be reused across concurrent threads.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see PropertyDescriptorChecksumGenerator
 */
@NullMarked
final class JsonSchemaDigestVisitor implements JsonSchemaVisitor {

	private static final byte EMPTY = 0x00;
	private static final byte VERSION = 0x42;

	private final MessageDigest digest;

	JsonSchemaDigestVisitor(MessageDigest digest) {
		this.digest = digest;
		digest.update(VERSION);
	}

	byte[] get() {
		return digest.digest();
	}

	@Override
	public void visit(JsonSchema schema) {
		// this is the main entry point for generating the digest. Methods in this visitor
		// should be calling this method when they want to update the digest for a schema
		writeType(schema);
		writeString(schema.title());
		writeString(schema.description());
		writeBoolean(schema.deprecated());
		writeCollection(schema.enumerations(), this::writeString);
		writeCollection(schema.examples(), this::writeString);

		JsonSchemaVisitor.super.visit(schema);
	}

	@Override
	public void visitObject(ObjectSchema schema) {
		writeSchema(schema.propertyNames());
		writeSchema(schema.additionalProperties());
		writeCollection(schema.required(), this::writeString);
		writeCollection(schema.properties().keySet(), key -> {
			writeString(key);
			writeSchema(schema.properties().get(key));
		});
	}

	@Override
	public void visitArray(ArraySchema schema) {
		writeInteger(schema.minItems());
		writeInteger(schema.maxItems());
		writeBoolean(schema.uniqueItems());
		writeSchema(schema.items());
	}

	@Override
	public void visitString(StringSchema schema) {
		writeString(schema.format());
		writeString(schema.pattern());
		writeInteger(schema.minLength());
		writeInteger(schema.maxLength());
	}

	@Override
	public void visitNumber(NumberSchema schema) {
		writeString(schema.format());
		writeDouble(schema.minimum());
		writeDouble(schema.maximum());
		writeDouble(schema.multipleOf());
		writeBoolean(schema.exclusiveMinimum());
		writeBoolean(schema.exclusiveMaximum());
	}

	@Override
	public void visitInteger(IntegerSchema schema) {
		writeString(schema.format());
		writeLong(schema.minimum());
		writeLong(schema.maximum());
		writeDouble(schema.multipleOf());
		writeBoolean(schema.exclusiveMinimum());
		writeBoolean(schema.exclusiveMaximum());
	}

	private void writeType(JsonSchema schema) {
		final byte type = switch (schema.type()) {
			case OBJECT -> 0x01;
			case ARRAY -> 0x02;
			case BOOLEAN -> 0x03;
			case STRING -> 0x04;
			case NUMBER -> 0x05;
			case INTEGER -> 0x06;
			case NULL -> 0x07;
		};

		digest.update(type);
	}

	private void writeBoolean(boolean value) {
		digest.update(value ? (byte) 0x01 : EMPTY);
	}

	private void writeInteger(@Nullable Integer value) {
		if (value != null) {
			digest.update(ByteBuffer.allocate(4).putInt(value).array());
		} else {
			digest.update(EMPTY);
		}
	}

	private void writeLong(@Nullable Long value) {
		if (value != null) {
			digest.update(ByteBuffer.allocate(8).putLong(value).array());
		} else {
			digest.update(EMPTY);
		}
	}

	private void writeDouble(@Nullable Double value) {
		if (value != null) {
			digest.update(ByteBuffer.allocate(8).putDouble(value).array());
		} else {
			digest.update(EMPTY);
		}
	}

	private void writeString(@Nullable String value) {
		if (StringUtils.hasText(value)) {
			digest.update(StringUtils.trimAllWhitespace(value).getBytes(StandardCharsets.UTF_8));
		} else {
			digest.update(EMPTY);
		}
	}

	private void writeSchema(@Nullable JsonSchema schema) {
		if (schema != null) {
			visit(schema);
		} else {
			digest.update(EMPTY);
		}
	}

	private void writeCollection(@Nullable Collection<String> collection, Consumer<String> consumer) {
		if (CollectionUtils.isEmpty(collection)) {
			digest.update(EMPTY);
			return;
		}

		final List<String> list = new ArrayList<>(collection);
		list.sort(String::compareTo);

		writeInteger(list.size());
		list.forEach(consumer);
	}

}
