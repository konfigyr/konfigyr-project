package com.konfigyr.data;

import com.konfigyr.data.web.CursorPageableHandlerMethodArgumentResolver;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Data autoconfiguration that would register the {@link WebMvcConfigurer} that would
 * customize the Spring Web MVC configuration.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration
public class DataWebAutoConfiguration implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.addFirst(new CursorPageableHandlerMethodArgumentResolver());
	}

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer pageableArgumentResolverCustomizer() {
		return resolver -> resolver.setOneIndexedParameters(true);
	}

}
