package com.konfigyr.artifactory.digest;

import com.konfigyr.artifactory.DataType;
import com.konfigyr.artifactory.Deprecation;
import com.konfigyr.artifactory.PropertyMetadata;
import com.konfigyr.artifactory.PropertyType;
import com.konfigyr.io.ByteArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyMetadataChecksumGeneratorTest {

	final PropertyMetadataChecksumGenerator generator = PropertyMetadataChecksumGenerator.getInstance();

	@Test
	@DisplayName("should generate unique checksum from property metadata")
	void generateUnique() {
		final var other = metadata(
				"spring.application.name",
				PropertyType.STRING,
				DataType.ATOMIC,
				"java.lang.String",
				"Application name. Typically used with logging to help identify the application."
		);

		assertThat(generator.generate(charset()))
				.isEqualTo(generator.generate(charset()))
				.isNotEqualTo(generator.generate(other));
	}

	@Test
	@DisplayName("should generate checksum from required property metadata properties")
	void generate() {
		assertThat(generator.generate(charset()))
				.extracting(ByteArray::encode)
				.isEqualTo("6-gplbdPGj3WwzUt7fWeOBCSijvuAtqR5pWwCkKFl8U=");
	}

	@Test
	@DisplayName("should generate same checksum when default value is null, empty or blank")
	void generateWithBlankDefaultValue() {
		final var metadata = charset();

		doReturn(null).when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("6-gplbdPGj3WwzUt7fWeOBCSijvuAtqR5pWwCkKFl8U=");

		doReturn("").when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("6-gplbdPGj3WwzUt7fWeOBCSijvuAtqR5pWwCkKFl8U=");

		doReturn("  ").when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("6-gplbdPGj3WwzUt7fWeOBCSijvuAtqR5pWwCkKFl8U=");
	}

	@Test
	@DisplayName("should generate different checksum when default value is added")
	void generateWithDefaultValue() {
		final var metadata = charset();

		doReturn(StandardCharsets.UTF_8.name()).when(metadata).defaultValue();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("DVRDDp4bZZ5DGcaIuDh3pp3EuknHu2sV2Sbyne3fvlE=");
	}

	@Test
	@DisplayName("should generate different checksum when hints are added")
	void generateWithHints() {
		final var metadata = charset();

		doReturn(List.of(StandardCharsets.UTF_8.name(), StandardCharsets.US_ASCII.name())).when(metadata).hints();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("ntowsAAUDkwL2uS0eNfqfLwbSUwvZUazLLcrH1dcMcs=");
	}

	@Test
	@DisplayName("should generate same checksum when hint values are in different positions")
	void generateWithRandomHintPositions() {
		final var hints = Stream.of(
				StandardCharsets.UTF_8.name(),
				StandardCharsets.ISO_8859_1.name(),
				StandardCharsets.US_ASCII.name(),
				StandardCharsets.UTF_32.name()
		).collect(Collectors.toList());

		Collections.shuffle(hints);

		final var metadata = charset();
		doReturn(hints).when(metadata).hints();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("3iumRJhHgvmw0dP7GyNv3coVoowFulrQETn2OSYZa4E=");

		// do another shuffle...
		Collections.shuffle(hints);

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("3iumRJhHgvmw0dP7GyNv3coVoowFulrQETn2OSYZa4E=");
	}

	@Test
	@DisplayName("should generate different checksum when deprecation is added")
	void generateWithDeprecation() {
		final var metadata = charset();

		doReturn(new Deprecation(null, null)).when(metadata).deprecation();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("6-gplbdPGj3WwzUt7fWeOBCSijvuAtqR5pWwCkKFl8U=");

		doReturn(new Deprecation("Some reason", null)).when(metadata).deprecation();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("a7JwIoa_W29MtIOMswJjFoPB0vTaXJwsHzX97v24NwY=");

		doReturn(new Deprecation("Some reason", "No replacement")).when(metadata).deprecation();

		assertThat(generator.generate(metadata))
				.extracting(ByteArray::encode)
				.isEqualTo("0GwJWbXGIFcI9zKlG8irfkmCpBuAmdwlZZgV-woibjE=");
	}

	static PropertyMetadata charset() {
		return metadata(
				"spring.banner.charset",
				PropertyType.CHARSET,
				DataType.COLLECTION,
				"java.nio.charset.Charset",
				"The charset to use for the banner. Defaults to UTF-8."
		);
	}

	static PropertyMetadata metadata(String name, PropertyType type, DataType dataType, String typeName, String description) {
		final var metadata = mock(PropertyMetadata.class);
		doReturn(name).when(metadata).name();
		doReturn(type).when(metadata).type();
		doReturn(dataType).when(metadata).dataType();
		doReturn(typeName).when(metadata).typeName();
		doReturn(description).when(metadata).description();
		return metadata;
	}

}
