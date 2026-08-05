package com.konfigyr.mcp.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Binds a tool call argument to the annotated parameter of an {@link McpTool}-annotated handler
 * method, and doubles as the source of that parameter's entry in the tool's generated input
 * schema.
 * <p>
 * Left blank, the annotated parameter's own (compiled-in) name is used as the argument name
 * instead - handy when the tool call argument and the parameter happen to share a name.
 * <p>
 * Can also be used as a meta-annotation to build more specialized tool-parameter annotations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Documented
@McpParameter
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface McpToolParam {

	/**
	 * Alias for {@link #name()}, letting the argument name be set without the {@code name=}
	 * prefix, e.g. {@code @McpToolParam("query")}.
	 */
	@AliasFor(annotation = McpParameter.class, attribute = "name")
	String value() default "";

	/**
	 * The name of the tool call argument to extract and bind to the annotated parameter, and
	 * the key this parameter is registered under in the tool's generated input schema.
	 * <p>
	 * Left blank, the annotated parameter's own name is used instead.
	 */
	@AliasFor(annotation = McpParameter.class, attribute = "name")
	String name() default "";

	/**
	 * Whether this argument is included in the tool's input schema's {@code required} list.
	 */
	boolean required() default true;

	/**
	 * Human-readable description of this argument, included in the tool's input schema to
	 * help the model understand what value to supply. Left blank, no description is included.
	 */
	String description() default "";

}
