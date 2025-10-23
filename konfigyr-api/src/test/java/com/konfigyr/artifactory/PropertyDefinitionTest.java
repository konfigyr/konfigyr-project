package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.version.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class PropertyDefinitionTest {

	@Test
	@DisplayName("should create property definition")
	void createProperty() {
		final var property = PropertyDefinition.builder()
				.id(1236L)
				.artifact(1453L)
				.type("STRING")
				.dataType("ATOMIC")
				.typeName("java.lang.String")
				.name("spring.application.name")
				.description("The name of the Spring application")
				.checksum(ByteArray.fromString("checksum"))
				.hint("super-app")
				.hints(List.of("awesome-app", "super-app"))
				.defaultValue("default-value")
				.deprecation(new Deprecation("Some reason", null))
				.occurrences(13)
				.firstSeen("1.0.0")
				.lastSeen("1.1.0")
				.build();

		assertThat(property)
				.isNotNull()
				.returns(EntityId.from(1236L), PropertyDefinition::id)
				.returns(EntityId.from(1453L), PropertyDefinition::artifact)
				.returns(PropertyType.STRING, PropertyDefinition::type)
				.returns(DataType.ATOMIC, PropertyDefinition::dataType)
				.returns("java.lang.String", PropertyDefinition::typeName)
				.returns("spring.application.name", PropertyDefinition::name)
				.returns("The name of the Spring application", PropertyDefinition::description)
				.returns(ByteArray.fromString("checksum"), PropertyDefinition::checksum)
				.returns(Version.of("1.0.0"), PropertyDefinition::firstSeen)
				.returns(Version.of("1.1.0"), PropertyDefinition::lastSeen)
				.returns(13, PropertyDefinition::occurrences)
				.returns(List.of("super-app", "awesome-app"), PropertyDefinition::hints)
				.returns("default-value", PropertyDefinition::defaultValue)
				.returns(new Deprecation("Some reason", null), PropertyDefinition::deprecation);
	}

	@Test
	@DisplayName("should sort property definition by name")
	void sortProperties() {
		final var builder = PropertyDefinition.builder()
				.id("000013229FPVS")
				.artifact("000005KK96ZZP")
				.type("STRING")
				.dataType("ATOMIC")
				.typeName("java.lang.String")
				.checksum(ByteArray.fromString("checksum"))
				.occurrences(1)
				.firstSeen("1.0.0")
				.lastSeen("1.1.0");

		final var applicationName = builder.name("spring.application.name").build();
		final var applicationGroup = builder.name("spring.application.group").build();
		final var charset = builder.name("spring.banner.charset").build();

		assertThat(Stream.of(charset, applicationGroup, applicationName).sorted())
				.containsExactly(applicationGroup, applicationName, charset);
	}

}
