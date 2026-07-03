package com.konfigyr.artifactory.digest;

import com.konfigyr.artifactory.Deprecation;
import com.konfigyr.artifactory.JsonSchema;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.artifactory.StringSchema;
import com.konfigyr.io.ByteArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyDescriptorChecksumGeneratorTest {

	final PropertyDescriptorChecksumGenerator generator = PropertyDescriptorChecksumGenerator.getInstance();

	@Test
	@DisplayName("should generate unique checksum from property metadata")
	void generateUnique() {
		final var other = metadata(
				"spring.application.name",
				"java.lang.String",
				"Application name. Typically used with logging to help identify the application.",
				StringSchema.instance()
		);

		assertThat(generator.generate(charset()))
				.isEqualTo(generator.generate(charset()))
				.isNotEqualTo(generator.generate(other));
	}

	@Test
	@DisplayName("should generate checksum from required property metadata properties")
	void generate() {
		assertThat(generator.generate(charset()))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("IC4XNSmUErqHEMLvL5F_LN9EOUaOsOLUBCuZ0wUkwdM=");
	}

	@Test
	@DisplayName("should generate same checksum when default value is null, empty or blank")
	void generateWithBlankDefaultValue() {
		final var metadata = charset();

		doReturn(null).when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("IC4XNSmUErqHEMLvL5F_LN9EOUaOsOLUBCuZ0wUkwdM=");

		doReturn("").when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("IC4XNSmUErqHEMLvL5F_LN9EOUaOsOLUBCuZ0wUkwdM=");

		doReturn("  ").when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("IC4XNSmUErqHEMLvL5F_LN9EOUaOsOLUBCuZ0wUkwdM=");
	}

	@Test
	@DisplayName("should generate different checksum when default value is added")
	void generateWithDefaultValue() {
		final var metadata = charset();

		doReturn(StandardCharsets.UTF_8.name()).when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("byzB0PVMcR2z9GW4jzJlVRiudwPj7gaS-E5SlKxAN1I=");
	}

	@Test
	@DisplayName("should generate different checksum when deprecation is added")
	void generateWithDeprecation() {
		final var metadata = charset();

		doReturn(new Deprecation(null, null)).when(metadata).deprecation();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("IC4XNSmUErqHEMLvL5F_LN9EOUaOsOLUBCuZ0wUkwdM=");

		doReturn(new Deprecation("Some reason", null)).when(metadata).deprecation();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("13pldiY3R2clnRTUGyYyP7Hn6BPpPjOV5tamhvwLyWM=");

		doReturn(new Deprecation("Some reason", "No replacement")).when(metadata).deprecation();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("PpGuvtgsDuftqvBlK7yzsLKqHwOqk51V9TtQTXSBvYk=");
	}

	@Test
	@DisplayName("should generate different checksum when JSON schema changes")
	void generateWithDifferentSchema() {
		final var metadata = charset();

		doReturn(StringSchema.builder().format("charset").pattern("[a-z]").build()).when(metadata).schema();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encodeBase64Url)
				.isEqualTo("hqfPc-kKqUGD6uqxs6bwiHWVeNGYP-A6-AbORvO-j9o=");
	}

	static PropertyDescriptor charset() {
		return metadata(
				"spring.banner.charset",
				"java.nio.charset.Charset",
				"The charset to use for the banner. Defaults to UTF-8.",
				StringSchema.builder().format("charset").build()
		);
	}

	static PropertyDescriptor metadata(String name, String typeName, String description, JsonSchema schema) {
		final var metadata = mock(PropertyDescriptor.class, withSettings().strictness(Strictness.LENIENT));
		doReturn(name).when(metadata).name();
		doReturn(typeName).when(metadata).typeName();
		doReturn(description).when(metadata).description();
		doReturn(schema).when(metadata).schema();
		return metadata;
	}

}
