package com.konfigyr.vault;


import com.google.crypto.tink.subtle.Hex;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.io.ByteArray;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.map.LinkedMap;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an immutable representation of configuration properties and their encrypted value states.
 * <p>
 * A {@link Properties} is a core domain value object that represents a fully materialized, sealed
 * configuration state. It guarantees that all values are encrypted and sealed, which prevents any operation
 * on plaintext configuration values. This type also ensures that the state is immutable and can be safely
 * cached.
 * <p>
 * Ordering of configuration properties must be deterministic and preserved. This type ensures that properties
 * are sorted based on their insertion order, much like an append-only log. When an update is performed on
 * an existing property, their position does not change, just the value. In case of a property removal, the
 * property is simply removed from the state, and the next property takes its place.
 * <p>
 * This type can be converted to a serialized state, in form of an {@link InputStream}, that can be safely
 * stored in a perstent storage, in our case the {@link com.konfigyr.vault.state.StateRepository}. Property
 * values within this value object can be decrypted using the {@link com.konfigyr.crypto.Keyset} associated
 * with the {@link com.konfigyr.namespace.Service} that owns the configuration.
 * <p>
 * Note about the serialized properties state. The serialized state uses a lightweight, UTF-8 encoded codec
 * for key-value property mapping of {@link Properties} which is designed to be a <i>cleaner</i> alternative
 * to {@code java.util.Properties}.
 * <p>
 * This codec tries to achieve three primary goals:
 * <ul>
 *     <li>
 *         <b>Metadata omission:</b> Unlike the standard library, it generates no timestamps or comments.
 *     </li>
 *     <li>
 *         <b>Modern encoding:</b> Uses {@link java.nio.charset.StandardCharsets#UTF_8} by default,
 *         eliminating the need for escaping for non-Latin characters.
 *      </li>
 *      <li>
 *          <b>Structural integrity:</b> Handles key and value escaping (newlines, tabs, and delimiters) to
 *          ensure that the serialized format can be reliably re-read.
 *      </li>
 *      <li>
 *          <b>Deterministic ordering:</b> Uses the {@link OrderedMapIterator} to preserve the insertion order
 *          of properties, ensuring that the output file remains stable across writes.
 *      </li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
@EqualsAndHashCode
public final class Properties implements InputStreamSource, Iterable<String> {

	private static final Pattern SEALED_PROPERTY_VALUE_PATTERN =
			Pattern.compile("\\{crypto:(?<crypto>[^,]+),\\s*checksum:(?<checksum>[^}]+)}");

	private static final CharsetDecoder STRICT_DECODER = StandardCharsets.UTF_8.newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);

	private final LinkedMap<String, PropertyValue> properties;

	/**
	 * Creates a new {@link Properties} instance from the given string contents. The given
	 * data should contain the properties and sealed values that are formatted according to the
	 * Konfigyr properties format.
	 *
	 * @param contents the string containing the serialized properties, must not be {@literal null}.
	 * @return the deserialized properties, never {@literal null}.
	 * @throws IOException when there is an I/O error while reading the serialized properties.
	 */
	public static Properties from(String contents) throws IOException {
		return from(new StringReader(contents));
	}

	/**
	 * Creates a new {@link Properties} instance from the given {@link InputStreamSource}. The given
	 * data should contain the properties and sealed values that are formatted according to the
	 * Konfigyr properties format.
	 *
	 * @param source the input stream source containing the serialized properties, must not be {@literal null}.
	 * @return the deserialized properties, never {@literal null}.
	 * @throws IOException when there is an I/O error while reading the serialized properties.
	 */
	public static Properties from(InputStreamSource source) throws IOException {
		return from(source.getInputStream());
	}

	/**
	 * Creates a new {@link Properties} instance from the given {@link InputStream}. The given
	 * data should contain the properties and sealed values that are formatted according to the
	 * Konfigyr properties format.
	 *
	 * @param is the input stream containing the serialized properties, must not be {@literal null}.
	 * @return the deserialized properties, never {@literal null}.
	 * @throws IOException when there is an I/O error while reading the serialized properties.
	 */
	public static Properties from(InputStream is) throws IOException {
		return from(new InputStreamReader(is, STRICT_DECODER));
	}

	/**
	 * Creates a new {@link Properties} instance from the given {@link Reader}. The given reader
	 * should contain the properties and sealed values that are formatted according to the
	 * Konfigyr properties format.
	 *
	 * @param reader the reader containing the serialized properties, must not be {@literal null}.
	 * @return the deserialized properties, never {@literal null}.
	 * @throws IOException when there is an I/O error while reading the serialized properties.
	 */
	public static Properties from(Reader reader) throws IOException {
		final Properties.Builder builder = Properties.builder();

		try (BufferedReader br = new BufferedReader(reader)) {
			String line;

			while ((line = br.readLine()) != null) {
				line = line.stripLeading();
				// just in case if the properties were written via java.util.Properties...
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
					continue;
				}

				int pivot = findPivot(line);

				if (pivot != -1) {
					builder.add(
							unescape(line.substring(0, pivot).trim()),
							parse(line.substring(pivot + 1).trim())
					);
				}
			}
		}

		return builder.build();
	}

	/**
	 * Creates a new {@link Builder} for constructing immutable {@link Properties} instances.
	 *
	 * @return the new properties builder, never {@literal null}.
	 */
	public static Builder builder() {
		return new Builder(Collections.emptyMap());
	}

	private Properties(LinkedMap<String, PropertyValue> properties) {
		this.properties = properties;
	}

	/**
	 * Returns the {@link PropertyValue} for the given property name.
	 *
	 * @param name the property name, cannot be {@literal null} or empty.
	 * @return the property value or an empty {@link Optional} if not found, never {@literal null}.
	 */
	public Optional<PropertyValue> get(String name) {
		return Optional.ofNullable(properties.get(name));
	}

	/**
	 * Checks if the property with the given name has a {@link PropertyValue} set.
	 *
	 * @param name the property name, cannot be {@literal null} or empty.
	 * @return {@literal true} if the property has a value, {@literal false} otherwise.
	 */
	public boolean has(String name) {
		return properties.containsKey(name);
	}

	/**
	 * The size of this {@link Properties} instance.
	 *
	 * @return the size of this {@link Properties} instance.
	 */
	public int size() {
		return properties.size();
	}

	/**
	 * Applies the given {@link PropertyChanges} to this {@link Properties} instance.
	 * <p>
	 * This method would use the {@link KeysetOperations} to seal the property values that should be
	 * added or modified and return a new {@link Properties} instance with the updated values.
	 *
	 * @param changes changes to be applied to this {@link Properties} instance, cannot be {@literal null}.
	 * @param keyset keyset operations used to seal the property values, cannot be {@literal null}.
	 * @return the new {@link Properties} instance with the applied changes, never {@literal null}.
	 */
	public Properties apply(PropertyChanges changes, KeysetOperations keyset) {
		final LinkedMap<String, PropertyValue> properties = new LinkedMap<>(this.properties);

		for (PropertyChange change : changes) {
			// try to resolve the previous/current value from the original properties map
			// as we need to check if the value exists to be modified or removed and if it
			// does not exist when a value is being added/created
			final PropertyValue previous = this.properties.get(change.name());

			switch (change.operation()) {
				case CREATE:
					Assert.isNull(previous, "Property '%s' already exists".formatted(change.name()));
					Assert.hasText(change.value(), "Property value must not be null or empty");

					properties.put(change.name(), PropertyValue.create(
							changes.profile().id(),
							change.name(),
							change.value()
					).seal(keyset));

					break;
				case MODIFY:
					Assert.notNull(previous, "Property '%s' does not exist".formatted(change.name()));
					Assert.hasText(change.value(), "Property value must not be null or empty");

					final PropertyValue value = PropertyValue.create(
							changes.profile().id(),
							change.name(),
							change.value()
					);

					if (!previous.checksum().equals(value.checksum())) {
						properties.put(change.name(), value.seal(keyset));
					}

					break;
				case REMOVE:
					Assert.notNull(previous, "Property '%s' does not exist".formatted(change.name()));

					properties.remove(change.name());

					break;
			}
		}

		return new Properties(properties);
	}

	/**
	 * Converts this {@link Properties} instance to an {@link InputStream}. The returned stream can
	 * be safely read or stored at rest as the property values are encrypted and sealed.
	 *
	 * @return the input stream containing the property values, never {@literal null}.
	 * @throws IOException when there is an I/O error while generating the serialized properties state.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			transferTo(output);
			return new ByteArrayInputStream(output.toByteArray());
		}
	}

	/**
	 * Transfer this {@link Properties} instance to the given destination {@link OutputStream}.
	 * <p>
	 * The data recieved by the target {@link OutputStream} does not contain sensitive information
	 * about the property values. The property values are encrypted and sealed, and can only be
	 * decrypted using the configured {@link com.konfigyr.crypto.Keyset}.
	 *
	 * @param os the output stream to transfer the serialized properties to, must not be {@literal null}.
	 * @throws IOException when there is an I/O error while transferring the serialized properties state.
	 */
	public void transferTo(OutputStream os) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
			final OrderedMapIterator<String, PropertyValue> iterator = iterator();

			while (iterator.hasNext()) {
				writer.write(escape(iterator.next()));
				writer.write("=");

				final PropertyValue value = iterator.getValue();
				Assert.state(value.isSealed(), () ->
						"Attempted to serialize unsealed property value for property: " + iterator.getKey());

				writer.write("{crypto:");
				writer.write(encode(value.get()));
				writer.write(",checksum:");
				writer.write(encode(value.checksum()));
				writer.write("}");
				writer.newLine();
			}

			writer.flush();
		}
	}

	@Override
	public OrderedMapIterator<String, PropertyValue> iterator() {
		return properties.mapIterator();
	}

	@Override
	public String toString() {
		return "Properties(" + properties + ")";
	}

	private static int findPivot(String line) {
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '=' || c == ':') {
				if (i == 0 || line.charAt(i - 1) != '\\') {
					return i;
				}
			}
		}
		return -1;
	}

	private static String escape(String input) {
		return input.replace("\\", "\\\\")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t")
				.replace("=", "\\=");
	}

	private static String unescape(String input) {
		return input.replace("\\n", "\n")
				.replace("\\r", "\r")
				.replace("\\t", "\t")
				.replace("\\=", "=")
				.replace("\\\\", "\\");
	}

	private static PropertyValue parse(String value) {
		final Matcher matcher = SEALED_PROPERTY_VALUE_PATTERN.matcher(value);

		if (matcher.matches()) {
			try {
				return PropertyValue.sealed(
						decode(matcher.group("crypto")),
						decode(matcher.group("checksum"))
				);
			} catch (Exception ex) {
				throw new IllegalArgumentException("Invalid serialized property value of: " + value, ex);
			}
		}

		throw new IllegalArgumentException("Invalid serialized property value of: " + value);
	}

	private static String encode(ByteArray bytes) {
		return Hex.encode(bytes.array());
	}

	private static ByteArray decode(String value) {
		return new ByteArray(Hex.decode(value));
	}

	public static final class Builder {
		private final LinkedMap<String, PropertyValue> properties;

		private Builder(Map<String, PropertyValue> properties) {
			this.properties = new LinkedMap<>(properties);
		}

		public Builder add(String name, PropertyValue value) {
			Assert.hasText(name, "Property name must not be null or empty");
			Assert.notNull(value, "Property value must not be null");
			Assert.isTrue(value.isSealed(), "Value for '%s' property is not sealed".formatted(name));
			properties.put(name, value);
			return this;
		}

		public Properties build() {
			return new Properties(properties);
		}
	}

}
