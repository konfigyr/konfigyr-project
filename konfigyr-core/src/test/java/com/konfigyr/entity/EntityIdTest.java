package com.konfigyr.entity;

import org.assertj.core.api.Assertions;
import org.assertj.core.internal.StandardComparisonStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class EntityIdTest {

	@Test
	@DisplayName("should parse entity identifier value")
	void shouldParseLongEntityValue() {
		assertThat(EntityId.from(31854375469809910L))
				.isNotNull()
				.returns(31854375469809910L, EntityId::get)
				.returns("00W9BCAZ6Q07P", EntityId::serialize);
	}

	@Test
	@DisplayName("should parse entity identifier serialized form")
	void shouldParseSerializedEntityValue() {
		assertThat(EntityId.from("00W9BCAZYQ01Q"))
				.isNotNull()
				.returns(31854375494975543L, EntityId::get)
				.returns("00W9BCAZYQ01Q", EntityId::serialize);
	}

	@Test
	@DisplayName("entity identifier should be sortable")
	void shouldSortEntityIdentifiers() {
		final var identifiers = Set.of(
				EntityId.from("00W9BCAZYQ01Q"),
				EntityId.from("00W9BCAZYQ01R"),
				EntityId.from("00W9BCB02Q01W"),
				EntityId.from("00W9BCAZ6Q07P")
		);

		assertThat(identifiers.stream().sorted())
				.isSorted()
				.containsExactly(
						EntityId.from(31854375469809910L),
						EntityId.from(31854375494975543L),
						EntityId.from(31854375494975544L),
						EntityId.from(31854375499169852L)
				);
	}

	@Test
	@DisplayName("should be serializable into JSON via Jackson")
	void toJSON() {
		final var id = EntityId.from(31854375494975544L);
		final var mapper = new ObjectMapper();

		assertThat(mapper.writeValueAsString(id))
				.as("Should write entity identifier using external form")
				.isEqualTo("\"00W9BCAZYQ01R\"");

		assertThat(mapper.writeValueAsString(Map.of("id", id)))
				.as("Should write entity identifier as a map value")
				.isEqualTo("{\"id\":\"00W9BCAZYQ01R\"}");

		assertThat(mapper.writeValueAsString(new EntityIdHolder(id)))
				.as("Should write entity identifier as holder value")
				.isEqualTo("{\"id\":\"00W9BCAZYQ01R\"}");

		Assertions.assertThat(mapper.readValue("\"00W9BCAZYQ01R\"", EntityId.class))
				.as("Should generate entity identifier from external form")
				.isEqualTo(id);

		Assertions.assertThat(mapper.readValue("{\"id\":\"00W9BCAZYQ01R\"}", EntityIdHolder.class))
				.as("Should generate entity identifier holder from external form")
				.isEqualTo(new EntityIdHolder(id));

		assertThatThrownBy(() -> mapper.readValue("31854375494975544", EntityId.class))
				.as("Should not generate entity identifier from internal form")
				.isInstanceOf(JacksonException.class);
	}

	@Test
	@DisplayName("should be serializable into JSON via Jackson using Spring Security default typing")
	void assertSpringSecurityCompatibility() {
		final var id = EntityId.from(31854375494975544L);
		final var validator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(EntityId.class);

		final var mapper = JsonMapper.builder()
				.addModules(SecurityJacksonModules.getModules(ClassLoaderUtils.getDefaultClassLoader(), validator))
				.build();

		assertThat(mapper.writeValueAsString(id))
				.as("Should write entity identifier using external form")
				.isEqualTo("\"00W9BCAZYQ01R\"");

		assertThat(mapper.writeValueAsString(Map.of("id", id)))
				.as("Should write entity identifier with type information")
				.isEqualTo("{\"id\":[\"com.konfigyr.entity.TimeSortedEntityId\",\"00W9BCAZYQ01R\"]}");

		assertThat(mapper.writeValueAsString(new EntityIdHolder(id)))
				.as("Should write entity identifier with type information")
				.isEqualTo("{\"id\":[\"com.konfigyr.entity.TimeSortedEntityId\",\"00W9BCAZYQ01R\"]}");

		Assertions.assertThat(mapper.readValue("[\"com.konfigyr.entity.TimeSortedEntityId\",\"00W9BCAZYQ01R\"]", EntityId.class))
				.as("Should generate entity identifier from external form and type information")
				.isEqualTo(id);

		Assertions.assertThat(mapper.readValue("{\"id\":[\"com.konfigyr.entity.TimeSortedEntityId\",\"00W9BCAZYQ01R\"]}", EntityIdHolder.class))
				.as("Should generate entity identifier holder from external form and type information")
				.isEqualTo(new EntityIdHolder(id));
	}

	@ValueSource(longs = { -124, 0 })
	@ParameterizedTest(name = "should not create identifier from {0}")
	@DisplayName("should not create entity identifier from invalid internal form")
	void shouldValidateInternalIdentifierValues(long value) {
		assertThatThrownBy(() -> EntityId.from(value))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Internal entity identifier must be a positive number: " + value);
	}

	@ValueSource(strings = { "", " ", "abc", "invalid hash", "00W9BCAZYQ01", "00W9BCAZYQ01Q1" })
	@ParameterizedTest(name = "should not create identifier from \"{0}\"")
	@DisplayName("should not create entity identifier from invalid external form")
	void shouldValidateExternalIdentifierValues(String value) {
		assertThatThrownBy(() -> EntityId.from(value))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid external entity identifier value: " + value);
	}

	@MethodSource("scenarios")
	@DisplayName("should generate unique entity identifier")
	@ParameterizedTest(name = "should generate ~{1} identifiers using {0} thread(s)")
	void shouldGenerateEntityIdentifiers(int threads, int iterations) {
		final var executor = new SimpleAsyncTaskExecutorBuilder()
				.concurrencyLimit(threads)
				.virtualThreads(true)
				.build();

		final List<EntityId> generated = new CopyOnWriteArrayList<>();
		final List<CompletableFuture<Void>> tasks = new ArrayList<>();

		final Runnable task = () -> EntityId.generate().ifPresent(generated::add);

		for (int i = 0; i < iterations; i++) {
			tasks.add(CompletableFuture.runAsync(task, executor));
		}

		final var future = CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));

		assertThatNoException()
				.as("should generate identifiers within 300 milliseconds")
				.isThrownBy(() -> future.get(300, TimeUnit.MILLISECONDS));

		assertThat(generated)
				.as("should generate %s identifiers", iterations)
				.hasSizeGreaterThanOrEqualTo(iterations);

		assertThat(StandardComparisonStrategy.instance().duplicatesFrom(generated))
				.as("should not encounter any collisions")
				.isEmpty();

		executor.close();
	}

	static Stream<Arguments> scenarios() {
		return Stream.of(
				Arguments.of(1, 10),
				Arguments.of(1, 100),
				Arguments.of(5, 100),
				Arguments.of(1, 100),
				Arguments.of(10, 100),
				Arguments.of(20, 100),
				Arguments.of(2, 1000),
				Arguments.of(15, 1000),
				Arguments.of(20, 1000),
				Arguments.of(5, 5000),
				Arguments.of(15, 5000),
				Arguments.of(20, 5000),
				Arguments.of(10, 10000),
				Arguments.of(20, 10000)
		);
	}

	record EntityIdHolder(EntityId id) {

	}

}
