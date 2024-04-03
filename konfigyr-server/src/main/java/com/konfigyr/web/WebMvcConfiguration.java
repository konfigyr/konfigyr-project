package com.konfigyr.web;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Spring Web MVC configuration.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer {

	private final WebProperties properties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		final CacheControl cacheControl = Optional.of(properties.getResources())
				.map(WebProperties.Resources::getCache)
				.map(WebProperties.Resources.Cache::getCachecontrol)
				.map(WebProperties.Resources.Cache.Cachecontrol::toHttpCacheControl)
				.orElseGet(() -> CacheControl.maxAge(180, TimeUnit.DAYS));

		// Our static assets are configured under `/assets/**` path so the
		// default `/favicon.ico` request not would not find the static resource.
		registry.addResourceHandler("/favicon.ico")
				.addResourceLocations("classpath:/static/")
				.setCacheControl(cacheControl);
	}

}
