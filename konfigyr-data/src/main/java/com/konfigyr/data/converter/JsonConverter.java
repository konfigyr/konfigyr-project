package com.konfigyr.data.converter;

import org.jooq.Converter;
import org.jooq.exception.DataTypeException;
import org.jooq.impl.AbstractConverter;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Implementation of the {@link Converter jOOQ Converter} that would convert String based
 * column values into desired Java types using {@link tools.jackson.databind.json.JsonMapper}.
 *
 * @param <T> generic converter target type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class JsonConverter<T> extends AbstractConverter<String, T> {

	private final ObjectMapper mapper;

	/**
	 * Creates a new {@link JsonConverter} with the default {@link ObjectMapper} that would convert
	 * values to or from desired type.
	 *
	 * @param <T> generic converter target type
	 * @param type target type, can't be {@literal null}
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static <T> Converter<String, T> create(Class<T> type) {
		Assert.notNull(type, "Target converter type must not be null");

		return new JsonConverter<>(JsonMapper.builder().build(), type);
	}

	/**
	 * Creates a new {@link JsonConverter} with an already customized {@link JsonMapper} that would
	 * convert values to or from desired type.
	 *
	 * @param mapper object mapper instance to be used, can't be {@literal null}
	 * @param type target type, can't be {@literal null}
	 * @param <T> generic converter target type
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static <T> Converter<String, T> create(ObjectMapper mapper, Class<T> type) {
		Assert.notNull(mapper, "Object mapper must not be null");
		Assert.notNull(type, "Target converter type must not be null");

		return new JsonConverter<>(mapper, type);
	}

	JsonConverter(@NonNull ObjectMapper mapper, @NonNull Class<T> type) {
		super(String.class, type);
		this.mapper = mapper;
	}

	@Override
	public T from(String value) {
		if (value == null) {
			return null;
		}

		try {
			return mapper.readValue(value, toType());
		} catch (JacksonException e) {
			throw new DataTypeException("Error when converting JSON to " + toType(), e);
		}
	}

	@Override
	public String to(T value) {
		if (value == null) {
			return null;
		}

		try {
			return mapper.writeValueAsString(value);
		} catch (JacksonException e) {
			throw new DataTypeException("Error when converting object of type " + toType() + " to JSON", e);
		}
	}
}
