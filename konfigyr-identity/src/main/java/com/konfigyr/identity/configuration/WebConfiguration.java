package com.konfigyr.identity.configuration;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void addViewControllers(@NonNull ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("index");
	}

}
