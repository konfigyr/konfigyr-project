package com.konfigyr.mcp.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;
import com.konfigyr.artifactory.ArraySchema;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.ObjectSchema;
import com.konfigyr.artifactory.StringSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.util.function.SingletonSupplier;
import reactor.core.publisher.Mono;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Default implementation of the {@link JsonSchemaGenerator}.
 * <p>
 * A type is first offered to each {@link JsonSchemaDefinitionProvider} in turn. If none of them
 * recognize it, this generator falls back to structural handling on its own: arrays, {@code Optional},
 * {@code Mono}, {@code Publisher} unwrapping, collections, and maps. Anything still unresolved after
 * that is introspected as a POJO, honoring {@code @JsonValue} (the type is treated as whatever that
 * method returns instead of an object), {@code @JsonIgnore}, and {@code @JsonProperty} renames, the
 * same way Jackson itself would serialize it.
 * <p>
 * Self-referential or mutually recursive types are cut short with a bare object schema rather than
 * recursed into forever. The resulting schema model has no {@code $ref} definition mechanism to
 * express a cycle. A type that resolves to nothing at all falls back to an untyped string schema,
 * logged as a warning, rather than failing the whole generation.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
final class DefaultJsonSchemaGenerator implements JsonSchemaGenerator {

	static final Supplier<JsonSchemaGenerator> factory = SingletonSupplier.of(() -> new DefaultJsonSchemaGenerator(
			new PrimitiveJsonSchemaDefinitionProvider(),
			new EnumJsonSchemaDefinitionProvider()
	));

	private final List<JsonSchemaDefinitionProvider> providers;

	DefaultJsonSchemaGenerator(JsonSchemaDefinitionProvider... providers) {
		this(Arrays.asList(providers));
	}

	@Override
	public JsonSchema generate(ResolvableType type, JsonSchemaGeneratorHints hints) {
		return generate(type, hints, new HashSet<>());
	}

	private JsonSchema generate(ResolvableType type, JsonSchemaGeneratorHints hints, Set<Class<?>> visiting) {
		JsonSchema.Builder<?, ?> builder = resolve(type, hints, visiting);

		if (builder == null) {
			log.warn("Could not generate a JSON schema for type '{}', falling back to an untyped string schema", type);
			builder = StringSchema.builder();
		}

		if (hints.description() != null) {
			builder.description(hints.description());
		}

		return builder.build();
	}

	private JsonSchema.@Nullable Builder<?, ?> resolve(ResolvableType type, JsonSchemaGeneratorHints hints, Set<Class<?>> visiting) {
		for (JsonSchemaDefinitionProvider provider : providers) {
			final JsonSchema.Builder<?, ?> schema = provider.provide(type, this);

			if (schema != null) {
				return schema;
			}
		}

		final Class<?> target = type.toClass();

		if (Mono.class.isAssignableFrom(target) || Publisher.class.isAssignableFrom(target)) {
			return resolve(elementType(type, 0), hints, visiting);
		}

		if (Optional.class.isAssignableFrom(target)) {
			return resolve(elementType(type, 0), hints, visiting);
		}

		if (!visiting.add(target)) {
			return ObjectSchema.builder();
		}

		try {
			if (type.isArray()) {
				return ArraySchema.builder()
						.items(generate(type.getComponentType(), JsonSchemaGeneratorHints.none(), visiting));
			}

			if (Collection.class.isAssignableFrom(target)) {
				return ArraySchema.builder()
						.items(generate(elementType(type, 0), JsonSchemaGeneratorHints.none(), visiting));
			}

			if (Map.class.isAssignableFrom(target)) {
				return ObjectSchema.builder()
						.additionalProperties(generate(elementType(type, 1), JsonSchemaGeneratorHints.none(), visiting));
			}

			return introspect(target, hints, visiting);
		} finally {
			visiting.remove(target);
		}
	}

	private JsonSchema.@Nullable Builder<?, ?> introspect(Class<?> target, JsonSchemaGeneratorHints hints, Set<Class<?>> visiting) {
		final Method jsonValue = findJsonValueMethod(target);

		if (jsonValue != null) {
			return resolve(ResolvableType.forMethodReturnType(jsonValue), hints, visiting);
		}

		final ObjectSchema.Builder builder = ObjectSchema.builder();

		if (target.isRecord()) {
			for (RecordComponent component : target.getRecordComponents()) {
				if (component.isAnnotationPresent(JsonIgnore.class)
						|| component.getAccessor().isAnnotationPresent(JsonIgnore.class)) {
					continue;
				}

				addProperty(builder, new PropertyCandidate(component), visiting);
			}

			return builder;
		}

		for (Field field : target.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic() || field.isAnnotationPresent(JsonIgnore.class)) {
				continue;
			}

			addProperty(builder, new PropertyCandidate(field), visiting);
		}

		return builder;
	}

	private void addProperty(ObjectSchema.Builder builder, PropertyCandidate candidate, Set<Class<?>> visiting) {
		builder.property(candidate.name(), generate(candidate.type(), candidate.hints(), visiting));

		if (candidate.isPrimitive()) {
			builder.required(candidate.name());
		}
	}

	private static String propertyName(String declaredName, @Nullable JsonProperty override) {
		if (override != null && !override.value().isBlank()) {
			return override.value();
		}

		return declaredName;
	}

	private static JsonSchemaGeneratorHints createHints(AnnotatedElement element) {
		JsonPropertyDescription description = element.getAnnotation(JsonPropertyDescription.class);

		if (description != null && !description.value().isBlank()) {
			return new JsonSchemaGeneratorHints(description.value());
		}

		return JsonSchemaGeneratorHints.none();
	}

	@Nullable
	private static Method findJsonValueMethod(Class<?> target) {
		return Arrays.stream(target.getMethods())
				.filter(method -> method.getParameterCount() == 0)
				.filter(method -> method.isAnnotationPresent(JsonValue.class))
				.findFirst()
				.orElse(null);
	}

	private static ResolvableType elementType(ResolvableType type, int index) {
		final ResolvableType generic = type.getGeneric(index);
		return ResolvableType.NONE.equals(generic) ? ResolvableType.forClass(Object.class) : generic;
	}

	private record PropertyCandidate(String name, ResolvableType type, JsonSchemaGeneratorHints hints) {

		private PropertyCandidate(RecordComponent component) {
			this(component.getName(), component.getAccessor(), ResolvableType.forType(component.getGenericType()));
		}

		private PropertyCandidate(Field field) {
			this(field.getName(), field, ResolvableType.forField(field));
		}

		private PropertyCandidate(String name, AnnotatedElement element, ResolvableType type) {
			this(propertyName(name, element.getAnnotation(JsonProperty.class)), type, createHints(element));
		}

		boolean isPrimitive() {
			return type.toClass().isPrimitive();
		}

	}

}
