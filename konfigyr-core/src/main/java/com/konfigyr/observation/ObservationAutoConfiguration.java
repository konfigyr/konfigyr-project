package com.konfigyr.observation;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;

@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
public class ObservationAutoConfiguration {

	@Bean
	ValueExpressionResolver spelValueExpressionResolver(ConversionService conversionService) {
		return new KonfigyrValueExpressionResolver(conversionService);
	}

}
