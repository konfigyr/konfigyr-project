package com.konfigyr.mcp.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Binds a variable from a resource's URI template to the annotated parameter of an
 * {@link McpResource}-annotated handler method. When the handler is invoked, the named
 * variable is extracted from the incoming request's URI and passed as the parameter's
 * argument.
 * <p>
 * Left blank, the annotated parameter's own (compiled-in) name is used as the variable name
 * instead - handy when the URI template variable and the parameter happen to share a name.
 * <p>
 * Can also be used as a meta-annotation to build more specialized template-variable
 * annotations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Documented
@McpParameter
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface McpTemplateVariable {

	/**
	 * The name of the URI template variable to extract and bind to the annotated parameter,
	 * e.g. {@code service} for the {@code {service}} variable in
	 * {@code konfigyr://services/{service}/manifest}.
	 * <p>
	 * Left blank, the parameter's own name is used instead. Either way, the parameter is
	 * bound to {@code null} if the resolved name doesn't match a variable actually present
	 * in the enclosing resource's URI template.
	 */
	@AliasFor(annotation = McpParameter.class, attribute = "name")
	String value() default "";

}
