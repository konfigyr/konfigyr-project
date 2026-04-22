package com.konfigyr.data.converter;

import org.jooq.Converter;
import org.jooq.JSONB;
import org.jooq.exception.DataTypeException;
import org.jooq.impl.AbstractConverter;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Implementation of the {@link Converter jOOQ Converter} that would convert {@link JSONB} based
 * column values into desired Java types using {@link JsonMapper}.
 *
 * @param <T> generic converter target type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class JsonbConverter<T> extends AbstractConverter<JSONB, T> {

	private final ObjectMapper mapper;
	private final JavaType javaType;

	/**
	 * Creates a new {@link JsonbConverter} with the default {@link ObjectMapper} that would convert
	 * values to or from desired type.
	 *
	 * @param <T> generic converter target type
	 * @param type target type, can't be {@literal null}
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static <T> Converter<JSONB, T> create(Class<T> type) {
		Assert.notNull(type, "Target converter type must not be null");

		return create(JsonMapper.builder().build(), type);
	}

	/**
	 * Creates a new {@link JsonbConverter} with an already customized {@link JsonMapper} that would
	 * convert values to or from desired type.
	 *
	 * @param mapper object mapper instance to be used, can't be {@literal null}
	 * @param type target type, can't be {@literal null}
	 * @param <T> generic converter target type
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static <T> Converter<JSONB, T> create(ObjectMapper mapper, Class<T> type) {
		Assert.notNull(mapper, "Object mapper must not be null");
		Assert.notNull(type, "Target converter type must not be null");

		return new JsonbConverter<>(mapper, mapper.constructType(type));
	}

	/**
	 * Creates a new {@link JsonbConverter} with an already customized {@link JsonMapper} that would
	 * convert values to or from desired type.
	 *
	 * @param mapper object mapper instance to be used, can't be {@literal null}
	 * @param type target type, can't be {@literal null}
	 * @param <T> generic converter target type
	 * @return JSON converter, never {@literal null}
	 */
	@NonNull
	public static <T> Converter<JSONB, T> create(ObjectMapper mapper, JavaType type) {
		Assert.notNull(mapper, "Object mapper must not be null");
		Assert.notNull(type, "Target converter type must not be null");

		return new JsonbConverter<>(mapper, type);
	}

	@SuppressWarnings("unchecked")
	JsonbConverter(@NonNull ObjectMapper mapper, @NonNull JavaType type) {
		super(JSONB.class, (Class<T>) type.getRawClass());
		this.mapper = mapper;
		this.javaType = type;
	}

	@Override
	public T from(JSONB value) {
		if (value == null) {
			return null;
		}

		try {
			return mapper.readValue(value.data(), javaType);
		} catch (JacksonException e) {
			throw new DataTypeException("Error when converting JSON to " + toType(), e);
		}
	}

	@Override
	public JSONB to(T value) {
		if (value == null) {
			return null;
		}

		try {
			return JSONB.jsonbOrNull(mapper.writeValueAsString(value));
		} catch (JacksonException e) {
			throw new DataTypeException("Error when converting object of type " + toType() + " to JSON", e);
		}
	}
}
