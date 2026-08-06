package com.konfigyr.mcp.annotation;

import java.lang.annotation.*;

/**
 * Marks a method to be discovered by classpath scanning and registered as an MCP tool: the
 * attributes below are used to build the tool's specification (its {@code Tool} definition and
 * annotation hints), which is then registered with the running MCP server instance with the
 * annotated method as its call handler.
 * <p>
 * Can also be used as a meta-annotation to build more specialized tool annotations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface McpTool {

	/**
	 * The name of the tool, used when calling it. If not provided, the method name will be
	 * used.
	 *
	 * @return unique tool name, never {@literal null}
	 */
	String name() default "";

	/**
	 * Intended for UI and end-user contexts — optimized to be human-readable and easily
	 * understood, even by those unfamiliar with domain-specific terminology. If not
	 * provided, {@link #name()} should be used for display instead.
	 * <p>
	 * This value is used both as the tool's own title and as its annotation hint's title;
	 * clients should give this precedence over {@link #name()} when displaying the tool.
	 *
	 * @return human-readable tool title
	 */
	String title() default "";

	/**
	 * A human-readable description of what the tool does. Clients can use this to improve
	 * the LLM's understanding of available tools.
	 *
	 * @return description for the MCP tool
	 */
	String description() default "";

	/**
	 * If true, the tool will generate an output schema for non-primitive output types. If
	 * false, the tool will not automatically generate an output schema.
	 *
	 * @return whether to generate an output schema, defaults to {@code false}
	 */
	boolean generateOutputSchema() default false;

	/**
	 * If true, the tool does not modify its environment.
	 *
	 * @return whether the tool is read-only, defaults to {@code true}
	 */
	boolean readOnlyHint() default true;

	/**
	 * If true, the tool may perform destructive updates to its environment. If false,
	 * the tool performs only additive updates.
	 * <p>
	 * (This property is meaningful only when readOnlyHint == false)
	 *
	 * @return whether the tool is destructive, defaults to {@code false}
	 */
	boolean destructiveHint() default false;

	/**
	 * If true, calling the tool repeatedly with the same arguments will have no
	 * additional effect on its environment.
	 * <p>
	 * (This property is meaningful only when readOnlyHint == false)
	 *
	 * @return whether the tool is idempotent, defaults to {@code false}
	 */
	boolean idempotentHint() default false;

	/**
	 * If true, this tool may interact with an "open world" of external entities. If
	 * false, the tool's domain of interaction is closed. For example, the world of a
	 * web search tool is open, whereas that of a memory tool is not.
	 *
	 * @return whether the tool is open world, defaults to {@code false}
	 */
	boolean openWorldHint() default false;

}
