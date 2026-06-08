package com.konfigyr.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.util.JsonHelper;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

/**
 * Fluent builder that assembles all Konfigyr MCP tool objects into a single
 * {@link ToolCallbackProvider}, wiring the application's autoconfigured {@link JsonMapper}
 * as the shared {@link ToolCallResultConverter} for every registered
 * {@link Tool @Tool}-annotated method.
 * <p>
 * Spring AI's {@code DefaultToolCallResultConverter} hardcodes its own Jackson mapper with
 * default settings instead of using the application's autoconfigured instance. Please see
 * <a href="https://github.com/spring-projects/spring-ai/issues/6097">spring-ai#6097</a> issue.
 * <p>
 * This builder fixes that by accepting the application's {@link JsonMapper}, already
 * configured by Spring Boot autoconfiguration with all {@code spring.jackson.*} settings and
 * Spring-registered Jackson modules applied, and using it as the single converter for every
 * tool result serialization.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see McpAutoConfiguration
 * @see Converter
 */
@NullMarked
class ToolCallbackProviderBuilder {

	/**
	 * The converter instance used to serialize tool return values, never {@literal null}.
	 */
	private final ToolCallResultConverter toolCallResultConverter;

	/**
	 * Map of {@link Tool @Tool}-annotated methods to their {@link ToolCallback} counterparts.
	 */
	private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();

	/**
	 * Creates a new {@link ToolCallbackProviderBuilder} that uses the given {@link JsonMapper}
	 * to serialize all tool return values.
	 * <p>
	 * Pass the {@code mcpServerJsonMapper} bean here, it is built from the application's primary
	 * mapper via {@link JsonMapper#rebuild()}, so it carries all {@code spring.jackson.*} settings
	 * and every Spring-registered Jackson module, including the Artifactory module required for
	 * correct {@link com.konfigyr.artifactory.JsonSchema} serialization.
	 *
	 * @param jsonMapper the application-derived mapper to use for tool result serialization,
	 *                   never {@literal null}
	 */
	ToolCallbackProviderBuilder(JsonMapper jsonMapper) {
		this.toolCallResultConverter = new Converter(new JsonHelper(jsonMapper));
	}

	/**
	 * Registers a tool object whose {@link Tool @Tool}-annotated methods will be discovered
	 * and wrapped as MCP tool callbacks.
	 * <p>
	 * AOP proxies are unwrapped transparently: the builder reflects on the target class
	 * to find {@link Tool @Tool} methods rather than the proxy surface.
	 *
	 * @param object the tool instance to register, never {@literal null}
	 * @return this builder for chaining, never {@literal null}
	 */
	ToolCallbackProviderBuilder register(Object object) {
		final Class<?> targetClass = AopUtils.isAopProxy(object) ? AopUtils.getTargetClass(object) : object.getClass();

		Stream.of(ReflectionUtils.getDeclaredMethods(targetClass))
				.filter(method -> method.isAnnotationPresent(Tool.class))
				.filter(ReflectionUtils.USER_DECLARED_METHODS::matches)
				.map(method -> MethodToolCallback.builder()
						.toolDefinition(ToolDefinitions.from(method))
						.toolMetadata(ToolMetadata.from(method))
						.toolMethod(method)
						.toolObject(object)
						.toolCallResultConverter(toolCallResultConverter)
						.build()
				).forEach(callback -> {
					final ToolDefinition definition = callback.getToolDefinition();

					if (callbacks.containsKey(definition.name())) {
						throw new IllegalStateException(
								"Failed to register a Tool with name '%s' declared in the '%s' as it already exists."
										.formatted(definition.name(), targetClass.getName())
						);
					}

					callbacks.put(definition.name(), callback);
				});

		return this;
	}

	/**
	 * Builds a single {@link ToolCallbackProvider} from all tool objects registered via
	 * {@link #register(Object)}.
	 * <p>
	 * All discovered {@link MethodToolCallback} instances, one per {@link Tool @Tool}-annotated
	 * method, each sharing the mapper-backed {@link Converter}, are wrapped in a
	 * {@link StaticToolCallbackProvider}. Duplicate detection happens eagerly in
	 * {@link #register(Object)}, so by the time {@code build()} is called the callback map
	 * is already guaranteed to be free of name collisions.
	 *
	 * @return a {@link StaticToolCallbackProvider} exposing every discovered callback,
	 *         never {@literal null}
	 */
	ToolCallbackProvider build() {
		return new StaticToolCallbackProvider(List.copyOf(callbacks.values()));
	}

	/**
	 * {@link ToolCallResultConverter} implementation used by {@link ToolCallbackProviderBuilder}
	 * to serialize tool return values to their MCP wire representation.
	 * <p>
	 * Three cases are handled in priority order:
	 * <ol>
	 *   <li>
	 *       <b>{@code void} / {@link Void} return type</b> — serialized as the conventional
	 *       {@code "Done"} string so that MCP clients always receive a well-formed text
	 *       response rather than an empty body.
	 *   </li>
	 *   <li>
	 *       <b>{@link RenderedImage}</b> — encoded as a lossless PNG and returned as a
	 *       JSON object with {@code mimeType} and base64-encoded {@code data} fields,
	 *       suitable for MCP image content blocks.
	 *   </li>
	 *   <li>
	 *       <b>All other types</b> — delegated to {@link JsonHelper#toJson(Object, boolean)}
	 *       using the configured {@link JsonMapper}, which carries all Spring-registered
	 *       Jackson modules.
	 *   </li>
	 * </ol>
	 */
	@Slf4j
	@RequiredArgsConstructor
	static final class Converter implements ToolCallResultConverter {

		private final JsonHelper jsonHelper;

		/**
		 * Converts {@code result} to the JSON string that will be sent back to the MCP client.
		 *
		 * @param result     value returned by the tool method, may be {@literal null}
		 * @param returnType declared return type of the tool method, may be {@literal null}
		 * @return JSON-encoded result string, never {@literal null}
		 */
		@Override
		public String convert(@Nullable Object result, @Nullable Type returnType) {
			if (returnType == Void.TYPE) {
				log.debug("The tool has no return type. Converting to conventional response.");
				return jsonHelper.toJson("Done");
			}

			if (result instanceof RenderedImage) {
				final var buf = new ByteArrayOutputStream(1024 * 4);

				try {
					ImageIO.write((RenderedImage) result, "PNG", buf);
				} catch (IOException e) {
					return "Failed to convert tool result to a base64 image: " + e.getMessage();
				}

				final String data = Base64.getEncoder().encodeToString(buf.toByteArray());
				return jsonHelper.toJson(Map.of("mimeType", "image/png", "data", data));
			}

			log.debug("Converting tool result to JSON.");
			return jsonHelper.toJson(result, true);
		}

	}
}
