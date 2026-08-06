package com.konfigyr.mcp.invoke;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

import java.lang.annotation.Annotation;

/**
 * One parameter of a {@link McpHandlerMethod}, paired with the name it's bound by and its
 * resolved {@link ResolvableType}.
 * <p>
 * The name comes from any annotation meta-annotated with {@link com.konfigyr.mcp.annotation.McpParameter}
 * declared on the parameter, or its own compiled-in name otherwise. Check out the {@link McpHandlerMethod}
 * for how it's resolved.
 * <p>
 * {@link #annotation(Class)} gives access to any other annotation declared on the parameter, so
 * construct-specific code (e.g., building a tool's input schema from a {@code @McpToolParam}-annotated parameter)
 * can read its own annotations without {@link McpHandlerMethod} needing to know about them.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@EqualsAndHashCode(of = "parameter")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpHandlerParameter {

	private final String name;
	private final ResolvableType type;
	private final MethodParameter parameter;
	private final MergedAnnotations annotations;

	static McpHandlerParameter of(String name, MergedAnnotations annotations, MethodParameter parameter) {
		return new McpHandlerParameter(name, ResolvableType.forMethodParameter(parameter), parameter, annotations);
	}

	/**
	 * Returns the name this parameter is bound by.
	 *
	 * @return the parameter name, never {@literal null}
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns this parameter's resolved type.
	 *
	 * @return the parameter type, never {@literal null}
	 */
	public ResolvableType type() {
		return type;
	}

	/**
	 * Returns the underlying Spring {@link MethodParameter}.
	 *
	 * @return the method parameter, never {@literal null}
	 */
	public MethodParameter parameter() {
		return parameter;
	}

	/**
	 * Returns the merged annotation of the given type declared on this parameter, if present.
	 *
	 * @param annotationType the annotation type to look up, can't be {@literal null}
	 * @param <A> the annotation type
	 * @return the merged annotation, never {@literal null} but may be
	 * {@link MergedAnnotation#isPresent() absent}
	 */
	public <A extends Annotation> MergedAnnotation<A> annotation(Class<A> annotationType) {
		return annotations.get(annotationType);
	}

	@Override
	public String toString() {
		return "McpHandlerParameter[name='" + name + "', type=" + type + ']';
	}
}
