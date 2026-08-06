package com.konfigyr.mcp.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for {@link ConditionalOnProperty} that gates a component on the
 * {@code konfigyr.mcp.enabled} property, defaulting to {@code true} when the property is unset.
 * <p>
 * Applied to every component that makes up the Konfigyr MCP server: the autoconfiguration, the
 * {@code /mcp} endpoint, and its tools. Disabling the MCP server removes all of them, not just
 * the beans declared in MCP autoconfiguration.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@ConditionalOnProperty(prefix = "konfigyr.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnMcpServer {

}
