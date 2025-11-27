package com.konfigyr.data;

import lombok.RequiredArgsConstructor;
import org.jooq.Converter;
import org.jooq.ConverterProvider;
import org.jooq.impl.DefaultConverterProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;

import java.util.function.Supplier;

@RequiredArgsConstructor
final class SpringConverterProvider implements ConverterProvider {

	private final ConverterProvider delegate = new DefaultConverterProvider();
	private final Supplier<ConversionService> conversionService;

	@Nullable
	@Override
	public <T, U> Converter<T, U> provide(Class<T> tType, Class<U> uType) {
		Converter<T, U> converter = delegate.provide(tType, uType);

		if (converter == null) {
			converter = provide(conversionService.get(), tType, uType);
		}

		return converter;
	}

	@Nullable
	private static <T, U> Converter<T, U> provide(ConversionService converter, Class<T> tType, Class<U> uType) {
		if (converter.canConvert(tType, uType)) {
			if (converter.canConvert(uType, tType)) {
				return Converter.of(
						tType,
						uType,
						t -> converter.convert(t, uType),
						u -> converter.convert(u, tType)
				);
			} else {
				return Converter.from(tType, uType, t -> converter.convert(t, uType));
			}
		}

		return null;
	}

}
