package com.konfigyr.mcp.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Konfigyr MCP component: it is registered as a Spring bean (this
 * annotation is meta-annotated with {@link Component}), and its methods are eligible to be
 * scanned for {@link McpResource}/{@link McpTool} annotations and registered with the running
 * MCP server instance.
 * <p>
 * A class without this annotation is never scanned for {@link McpResource}/{@link McpTool}
 * methods, regardless of whether it is a Spring bean through some other mechanism.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@ConditionalOnMcpServer
public @interface McpComponent {

	/**
	 * The Spring bean name for this component, aliased to {@link Component#value()}.
	 *
	 * @return the bean name, never {@literal null}
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
