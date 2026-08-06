package com.konfigyr.mcp.annotation;

import java.lang.annotation.*;

/**
 * Marks a method to be discovered by classpath scanning and registered as an MCP resource
 * template: the attributes below are used to build the resource template's specification, which
 * is then registered with the running MCP server instance with the annotated method as its read
 * handler.
 * <p>
 * Can also be used as a meta-annotation to build more specialized resource annotations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface McpResource {

	/**
	 * Intended for programmatic or logical use, but used as a display name in past specs
	 * or as a fallback if {@link #title()} isn't present.
	 *
	 * @return unique MCP resource name, never {@literal null}
	 */
	String name() default "";

	/**
	 * Optional human-readable title of this resource, used by clients for display purposes.
	 *
	 * @return human-readable title of this resource, never {@literal null}
	 */
	String title() default "";

	/**
	 * The URI template for this resource, using RFC 6570 syntax (e.g.
	 * {@code konfigyr://services/{service}/manifest}). Variables in the template are
	 * extracted from the incoming request's URI.
	 *
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6570">RFC 6570</a>
	 *
	 * @return URI template for this resource, never {@literal null}
	 */
	String uri() default "";

	/**
	 * A description of what this resource represents. This can be used by clients to
	 * improve the LLM's understanding of available resources. It can be thought of like a
	 * "hint" to the model.
	 *
	 * @return description of this resource, never {@literal null}
	 */
	String description() default "";

	/**
	 * The MIME type of this resource's content, if known. Defaults to {@code text/plain}
	 * when not set.
	 *
	 * @return MIME type of this resource's content, never {@literal null}
	 */
	String mimeType() default "text/plain";

}
