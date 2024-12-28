package com.konfigyr.data.converter;

import com.konfigyr.io.ByteArray;
import org.jooq.Converter;
import org.jooq.impl.AbstractConverter;
import org.springframework.util.function.SingletonSupplier;

import java.util.function.Supplier;

/**
 * Implementation of the {@link Converter jOOQ Converter} that would wrap the byte array values into
 * a {@link ByteArray} type.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ByteArrayConverter extends AbstractConverter<byte[], ByteArray> {

	private static final Supplier<ByteArrayConverter> instance = SingletonSupplier.of(ByteArrayConverter::new);

	public static Converter<byte[], ByteArray> getInstance() {
		return instance.get();
	}

	public ByteArrayConverter() {
		super(byte[].class, ByteArray.class);
	}

	@Override
	public ByteArray from(byte[] bytes) {
		if (bytes == null) {
			return null;
		}

		if (bytes.length == 0) {
			return ByteArray.empty();
		}

		return new ByteArray(bytes);
	}

	@Override
	public byte[] to(ByteArray bytes) {
		return bytes == null ? null : bytes.array();
	}

}
