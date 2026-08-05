package com.konfigyr.mcp.annotation;

import java.lang.annotation.*;

/**
 * Base annotation for binding a named raw argument to a method parameter handled by the shared
 * MCP method invoker.
 * <p>
 * Meant to be used as a meta-annotation. Construct-specific annotations, such as
 * {@link McpTemplateVariable} for a resource's URI template variables, alias their own attribute
 * into {@link #name()} via {@code @AliasFor}. The invoker looks up this one annotation type
 * regardless of which specific annotation was actually applied to the parameter.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface McpParameter {

	/**
	 * The name of the raw argument to bind to the annotated parameter.
	 * <p>
	 * Left blank, the annotated parameter's own (compiled-in) name is used instead.
	 */
	String name() default "";

}
