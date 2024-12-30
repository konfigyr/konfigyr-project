package com.konfigyr.data;

import org.jooq.ConverterProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;

/**
 * Spring Data autoconfiguration that would configure our jOOQ {@link org.jooq.DSLContext}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration(before = JooqAutoConfiguration.class)
class DataAutoConfiguration {

	/**
	 * Registers the {@link DefaultConfigurationCustomizer} that would register a customer jOOQ
	 * {@link ConverterProvider} that would use the {@link ConversionService} to provide additional
	 * {@link org.jooq.Converter conveters}.
	 *
	 * @param conversionService Spring conversion service
	 * @return configuration customizer
	 */
	@Bean
	DefaultConfigurationCustomizer conversionServiceConverterConfigurationCustomizer(
			final ObjectProvider<ConversionService> conversionService
	) {
		final ConverterProvider converterProvider = new SpringConverterProvider(
				() -> conversionService.getIfAvailable(ApplicationConversionService::getSharedInstance)
		);

		return configuration -> configuration.set(converterProvider);
	}
}
