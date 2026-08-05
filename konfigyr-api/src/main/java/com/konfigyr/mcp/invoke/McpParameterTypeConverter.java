package com.konfigyr.mcp.invoke;

import io.modelcontextprotocol.json.McpJsonMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import java.util.function.Supplier;

/**
 * Converts a raw argument value - a URI template variable, a tool call argument, ... - into the
 * declared type of the {@link McpHandlerParameter} it's bound to.
 * <p>
 * A Spring {@link ConversionService} is tried first, since it already knows how to convert plain
 * scalars: a {@code String} into a number, an enum, a {@code UUID}, and so on. Anything it can't
 * convert falls back to {@link McpJsonMapper#convertValue}, which additionally handles JSON
 * structures. Tool call arguments may arrive as parsed {@code Map}/{@code List} being bound to a
 * record or POJO parameter.
 * <p>
 * The {@link ConversionService} is supplied lazily rather than injected directly, deferring
 * resolution of the actual bean until a conversion is needed instead of pinning it at construction
 * time.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
final class McpParameterTypeConverter {

	private final Supplier<ConversionService> conversionService;
	private final McpJsonMapper jsonMapper;

	/**
	 * Converts {@code value} into the type declared by {@code parameter}.
	 *
	 * @param parameter the parameter whose declared type {@code value} is converted to, can't
	 * be {@literal null}
	 * @param value the raw value to convert, can be {@literal null}
	 * @param <T> the target type
	 * @return the converted value, or {@literal null} if {@code value} is {@literal null}
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	<T> T convert(McpHandlerParameter parameter, @Nullable Object value) {
		if (value == null) {
			return null;
		}

		final ConversionService converter = conversionService.get();
		final TypeDescriptor sourceDescriptor = TypeDescriptor.forObject(value);
		final TypeDescriptor targetDescriptor = new TypeDescriptor(parameter.type(), null, null);

		if (converter.canConvert(sourceDescriptor, targetDescriptor)) {
			return (T) converter.convert(value, sourceDescriptor, targetDescriptor);
		}

		return (T) jsonMapper.convertValue(value, targetDescriptor.getType());
	}

}
