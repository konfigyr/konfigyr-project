package com.konfigyr.mcp;

import com.konfigyr.mcp.annotation.McpTool;
import com.konfigyr.mcp.annotation.McpToolParam;
import com.konfigyr.mcp.registry.McpAnnotationRegistration;
import com.konfigyr.mcp.tool.StructuredCollectionOutput;
import com.konfigyr.mcp.tool.StructuredEntityOutput;
import com.konfigyr.mcp.tool.StructuredOutput;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Reusable {@code @McpTool}-annotated fixture methods, shared across tests that need a real
 * annotated tool method to reflect over - building a {@code McpAnnotationRegistration}, a
 * {@code McpHandlerMethod}, or a full tool specification - rather than each test declaring its
 * own throwaway fixture bean.
 */
public final class McpToolFixtures {

	/**
	 * The shared bean instance containing every fixture method below is declared on.
	 */
	public static final ToolBean BEAN = new ToolBean();

	private McpToolFixtures() {
	}

	/**
	 * Looks up one of {@link ToolBean}'s declared methods by name.
	 *
	 * @param name the method name, can't be {@literal null}
	 * @return the method, never {@literal null}
	 */
	public static Method method(String name) {
		return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(ToolBean.class))
				.filter(candidate -> candidate.getName().equals(name))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No fixture tool method named '" + name + "'"));
	}

	/**
	 * Builds a {@link McpAnnotationRegistration} for one of {@link ToolBean}'s declared methods,
	 * as if it had been discovered by {@code McpAnnotationBeanPostProcessor}.
	 *
	 * @param name the method name, can't be {@literal null}
	 * @return the registration, never {@literal null}
	 */
	public static McpAnnotationRegistration<McpTool> registration(String name) {
		final Method method = method(name);
		return new McpAnnotationRegistration<>(BEAN, method, MergedAnnotations.from(method).get(McpTool.class));
	}

	public record Greeting(String message) {

	}

	public static class ToolBean {

		@McpTool(name = "greet", title = "Greet", description = "Greets someone", idempotentHint = true, openWorldHint = true)
		public Greeting greet(@McpToolParam(description = "Who to greet") String name) {
			return new Greeting("Hello, " + name);
		}

		@McpTool(name = "destroy", description = "Does nothing", readOnlyHint = false, destructiveHint = true)
		public void destroy(@McpToolParam(required = false) Integer timeout) {

		}

		@McpTool(name = "find", title = "Find", description = "Finds a greeting", generateOutputSchema = true)
		public StructuredEntityOutput<Greeting> find(@McpToolParam(description = "Who to find") String name) {
			return StructuredOutput.of(new Greeting("Hello, " + name));
		}

		@McpTool(name = "list", title = "List", description = "Lists greetings", generateOutputSchema = true)
		public StructuredCollectionOutput<Greeting> list() {
			return StructuredOutput.of(List.of(new Greeting("Hello, John"), new Greeting("Hello, Jane")));
		}

		@McpTool(name = "whoami", title = "Who Am I", description = "Echoes the current transport context")
		public McpTransportContext whoami(McpTransportContext context) {
			return context;
		}

	}

}
