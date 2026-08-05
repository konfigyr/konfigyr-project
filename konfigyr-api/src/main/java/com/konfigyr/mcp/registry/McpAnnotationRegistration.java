package com.konfigyr.mcp.registry;

import com.konfigyr.mcp.annotation.McpResource;
import com.konfigyr.mcp.annotation.McpTool;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.annotation.MergedAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@NullMarked
public record McpAnnotationRegistration<T extends Annotation>(Object bean, Method method, T annotation) {

	public McpAnnotationRegistration(Object bean, Method method, MergedAnnotation<T> annotation) {
		this(bean, method, annotation.synthesize());
	}

	public Class<? extends Annotation> type() {
		return annotation.annotationType();
	}

	public String name() {
		return switch (annotation) {
			case McpTool tool -> tool.name();
			case McpResource resource -> resource.name();
			default -> throw new IllegalArgumentException("Unsupported annotation type: " + type());
		};
	}

	@Override
	public String toString() {
		return "McpAnnotationRegistration(type=" + type().getSimpleName() + ", name=" + name() + ", method=" + method + ")";
	}
}
