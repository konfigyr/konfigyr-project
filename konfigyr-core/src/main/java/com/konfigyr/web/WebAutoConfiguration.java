package com.konfigyr.web;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Avatar;
import com.konfigyr.web.error.KonfigyrErrorAttributes;
import com.konfigyr.web.filter.ContextFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Printer;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.EnumSet;

/**
 * Spring Web MVC auto configuration.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnWebApplication
public class WebAutoConfiguration implements WebMvcConfigurer {

	@Override
	public void addFormatters(@NonNull FormatterRegistry registry) {
		registry.addConverter(EntityId.class, String.class, EntityId::serialize);
		registry.addConverter(String.class, EntityId.class, EntityId::from);
		registry.addConverter(Long.class, EntityId.class, EntityId::from);
		registry.addConverter(String.class, Avatar.class, Avatar::parse);
		registry.addConverter(Avatar.class, String.class, Avatar::get);

		registry.addFormatterForFieldType(
				EntityId.class,
				(Printer<EntityId>) (id, locale) -> id.serialize(),
				(id, locale) -> EntityId.from(id)
		);
	}

	@Bean
	@Primary
	ErrorAttributes konfigyrErrorAttributes() {
		return new KonfigyrErrorAttributes();
	}

	@Bean
	FilterRegistrationBean<ContextFilter> contextFilter(ObjectProvider<LocaleResolver> localeResolverProvider) {
		final LocaleResolver localeResolver = localeResolverProvider.getIfAvailable(AcceptHeaderLocaleResolver::new);
		final FilterRegistrationBean<ContextFilter> registration = new FilterRegistrationBean<>();
		registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
		registration.setFilter(new ContextFilter(localeResolver));
		registration.setOrder(ContextFilter.DEFAULT_ORDER);
		return registration;
	}

}
