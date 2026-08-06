package com.konfigyr.mcp.registry;

import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.annotation.McpTool;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public final class McpAnnotationRegistry {

	private final MultiValueMap<Class<? extends Annotation>, McpAnnotationRegistration<?>> annotations;

	public McpAnnotationRegistry() {
		annotations = new LinkedMultiValueMap<>();
	}

	void register(MergedAnnotation<?> annotation, Object bean, Method method) {
		annotations.add(annotation.getType(), new McpAnnotationRegistration<>(bean, method, annotation));
	}

	public List<McpAnnotationRegistration<McpResource>> resources() {
		return get(McpResource.class);
	}

	public List<McpAnnotationRegistration<McpTool>> tools() {
		return get(McpTool.class);
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> List<McpAnnotationRegistration<T>> get(Class<T> annotationType) {
		final List<McpAnnotationRegistration<?>> registrations = annotations.get(annotationType);

		if (CollectionUtils.isEmpty(registrations)) {
			return Collections.emptyList();
		}

		return registrations.stream()
				.map(registration -> (McpAnnotationRegistration<T>) registration)
				.toList();
	}

}
