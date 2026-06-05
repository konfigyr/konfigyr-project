package com.konfigyr.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * AssertJ custom assertion for {@link McpSchema.Tool} values.
 * <p>
 * Provides fluent, domain-specific assertions for MCP tool definitions:
 * name, description, input schema properties, and tool annotations.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public final class McpToolAssert extends AbstractObjectAssert<McpToolAssert, McpSchema.Tool> {

	/**
	 * Creates a new {@link McpToolAssert} for the given tool.
	 *
	 * @param tool the actual tool to verify, may be {@literal null}
	 * @return a new assertion instance, never {@literal null}
	 */
	@NonNull
	public static McpToolAssert assertThat(McpSchema.Tool tool) {
		return new McpToolAssert(tool);
	}

	/**
	 * Creates an {@link InstanceOfAssertFactory} that produces {@link McpToolAssert}
	 * instances, suitable for use with AssertJ's {@code asInstanceOf()} navigation.
	 *
	 * @return the MCP tool assert factory, never {@literal null}
	 */
	@NonNull
	public static InstanceOfAssertFactory<McpSchema.Tool, McpToolAssert> factory() {
		return new InstanceOfAssertFactory<>(McpSchema.Tool.class, McpToolAssert::assertThat);
	}

	private McpToolAssert(McpSchema.Tool actual) {
		super(actual, McpToolAssert.class);
	}

	/**
	 * Asserts that the tool's {@link McpSchema.Tool#name() name} matches the expected value.
	 *
	 * @param expected expected tool name, may be {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert hasName(@Nullable String expected) {
		isNotNull();

		if (!Objects.equals(expected, actual.name())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool name to be <%s> but was <%s>",
					expected, actual.name()
			));
		}

		return myself;
	}

	/**
	 * Asserts that the tool's {@link McpSchema.Tool#description() description} matches the expected value.
	 *
	 * @param expected expected description, may be {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert hasDescription(@Nullable String expected) {
		isNotNull();

		if (!Objects.equals(expected, actual.description())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool description to be <%s> but was <%s>",
					expected, actual.description()
			));
		}

		return myself;
	}

	/**
	 * Asserts that the tool's {@link McpSchema.Tool#description() description} contains the given substring.
	 *
	 * @param substring substring expected to appear in the description, never {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert descriptionContains(@NonNull String substring) {
		isNotNull();

		final String description = actual.description();

		if (description == null || !description.contains(substring)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool description to contain <%s> but was <%s>",
					substring, description
			));
		}

		return myself;
	}

	/**
	 * Asserts that the tool's {@link McpSchema.Tool#inputSchema() input schema} contains a
	 * property with the given name.
	 *
	 * @param propertyName name of the property expected to be present in the input schema, never {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert hasInputSchemaProperty(@NonNull String propertyName) {
		isNotNull();

		final Map<String, Object> schema = actual.inputSchema();

		if (schema == null) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to have an input schema with property <%s> but input schema was null",
					propertyName
			));
			return myself;
		}

		final Object propertiesNode = schema.get("properties");

		if (!(propertiesNode instanceof Map<?, ?> properties) || !properties.containsKey(propertyName)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool input schema to contain property <%s> but properties were <%s>",
					propertyName, propertiesNode
			));
		}

		return myself;
	}

	/**
	 * Asserts that the given input schema property is listed in the {@code required} array.
	 *
	 * @param propertyName name of the property expected to be required, never {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public McpToolAssert hasRequiredInputProperty(@NonNull String propertyName) {
		isNotNull();

		final Map<String, Object> schema = actual.inputSchema();

		if (schema == null) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to have required input property <%s> but input schema was null",
					propertyName
			));
			return myself;
		}

		final Object requiredNode = schema.get("required");

		if (!(requiredNode instanceof Iterable required)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool input schema to have <%s> as a required property but 'required' was <%s>",
					propertyName, requiredNode
			));
			return myself;
		}

		Assertions.assertThat(required)
				.as("Expected MCP tool input schema to have contain required property: <%s>", propertyName)
				.contains(propertyName);

		return myself;
	}

	/**
	 * Asserts that the given input schema properties are listed in the {@code required} array.
	 *
	 * @param propertyNames names of the property expected to be required, never {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert hasRequiredInputProperties(@NonNull String... propertyNames) {
		return hasRequiredInputProperties(Arrays.asList(propertyNames));
	}

	/**
	 * Asserts that the given input schema properties are listed in the {@code required} array.
	 *
	 * @param propertyNames names of the property expected to be required, never {@literal null}
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public McpToolAssert hasRequiredInputProperties(@NonNull Collection<String> propertyNames) {
		isNotNull();

		final Map<String, Object> schema = actual.inputSchema();

		if (schema == null) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to have required input properties <%s> but input schema was null",
					String.join(", ", propertyNames)
			));
			return myself;
		}

		final Object requiredNode = schema.get("required");

		if (!(requiredNode instanceof Iterable required)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool input schema to have <%s> as a required property but 'required' was <%s>",
					String.join(", ", propertyNames), requiredNode
			));
			return myself;
		}

		Assertions.assertThat(required)
				.as("Expected MCP tool input schema to have following required properties: <%s>", String.join(", ", propertyNames))
				.containsExactlyInAnyOrderElementsOf(propertyNames);

		return myself;
	}

	/**
	 * Asserts that the tool has no {@link McpSchema.Tool#annotations() annotations}.
	 *
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert hasNoAnnotations() {
		isNotNull();

		if (actual.annotations() != null) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to have no annotations but found <%s>",
					actual.annotations()
			));
		}

		return myself;
	}

	/**
	 * Asserts that the tool's {@link McpSchema.ToolAnnotations#readOnlyHint()} is {@code true}.
	 *
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert isReadOnly() {
		isNotNull();

		final McpSchema.ToolAnnotations annotations = actual.annotations();

		if (annotations == null || !Boolean.TRUE.equals(annotations.readOnlyHint())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to be read-only (readOnlyHint=true) but annotations were <%s>",
					annotations
			));
		}

		return myself;
	}

	/**
	 * Asserts that the tool's {@link McpSchema.ToolAnnotations#destructiveHint()} is {@code true}.
	 *
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert isDestructive() {
		isNotNull();

		final McpSchema.ToolAnnotations annotations = actual.annotations();

		if (annotations == null || !Boolean.TRUE.equals(annotations.destructiveHint())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to be destructive (destructiveHint=true) but annotations were <%s>",
					annotations
			));
		}

		return myself;
	}

	/**
	 * Asserts that the tool's {@link McpSchema.ToolAnnotations#idempotentHint()} is {@code true}.
	 *
	 * @return this MCP tool assert instance for chaining, never {@literal null}
	 */
	@NonNull
	public McpToolAssert isIdempotent() {
		isNotNull();

		final McpSchema.ToolAnnotations annotations = actual.annotations();

		if (annotations == null || !Boolean.TRUE.equals(annotations.idempotentHint())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected MCP tool to be idempotent (idempotentHint=true) but annotations were <%s>",
					annotations
			));
		}

		return myself;
	}

}
