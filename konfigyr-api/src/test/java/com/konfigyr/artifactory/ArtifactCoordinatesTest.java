package com.konfigyr.artifactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.konfigyr.version.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ArtifactCoordinatesTest {

	final JsonMapper mapper = JsonMapper.builder().build();

	@ValueSource(strings = {
			"com.konfigyr:konfigyr-api:1.0.0",
			"org.eclipse.aether:aether-impl:0.9.0-M2",
			"com.google.code.findbugs:annotations:3.0.0",
			"org.sonatype.sisu:sisu-guice:3.1.0"
	})
	@ParameterizedTest(name = "should parse coordinates: {0}")
	@DisplayName("should create ArtifactCoordinates from Maven coordinates string")
	void shouldParseCoordinates(String coordinates) {
		assertThat(ArtifactCoordinates.parse(coordinates))
				.isNotNull()
				.isInstanceOf(SimpleArtifactCoordinates.class)
				.hasToString(coordinates);
	}

	@Test
	@DisplayName("should fail to parse ArtifactCoordinates from null, empty, blank or invalid coordinates")
	void shouldFailToParseCoordinates() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactCoordinates.parse(null))
				.withMessageContaining("Artifact coordinates must not be null or blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactCoordinates.parse(""))
				.withMessageContaining("Artifact coordinates must not be null or blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactCoordinates.parse("   "))
				.withMessageContaining("Artifact coordinates must not be null or blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactCoordinates.parse("invalid coordinates"))
				.withMessageContaining("Invalid Artifact coordinates: invalid coordinates")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactCoordinates.parse("com.group:artifact: "))
				.withMessageContaining("Version must not be empty")
				.withNoCause();
	}

	@Test
	@DisplayName("should create Artifact coordinates from fields")
	void createCoordinates() {
		assertThat(ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", "2.1.0"))
				.returns("com.konfigyr", ArtifactCoordinates::groupId)
				.returns("konfigyr-api", ArtifactCoordinates::artifactId)
				.returns(Version.of("2.1.0"), ArtifactCoordinates::version)
				.hasToString("com.konfigyr:konfigyr-api:2.1.0")
				.isEqualTo(ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:2.1.0"))
				.isNotEqualTo(ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:2.0.0"));
	}

	@Test
	@DisplayName("should sort Artifact coordinates")
	void sortCoordinates() {
		final var coordinates = List.of(
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:2.1.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-identity:0.1.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.1.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-identity:1.0.0")
		);

		assertThat(coordinates.stream().sorted()).containsExactly(
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.0.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:1.1.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-api:2.1.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-identity:0.1.0"),
				ArtifactCoordinates.parse("com.konfigyr:konfigyr-identity:1.0.0")
		);
	}

	@Test
	@DisplayName("should serialize Artifact coordinates to JSON string")
	void serializeCoordinates() throws IOException {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", Version.of("2.1.0"));

		assertThat(mapper.writeValueAsString(coordinates))
				.isEqualTo("\"com.konfigyr:konfigyr-api:2.1.0\"");
	}

	@Test
	@DisplayName("should deserialize Artifact coordinates from JSON string")
	void deserializeCoordinates() throws IOException {
		assertThat(mapper.readValue("\"com.konfigyr:konfigyr-api:2.0.0\"", ArtifactCoordinates.class))
				.isEqualTo(ArtifactCoordinates.of("com.konfigyr", "konfigyr-api", Version.of("2.0.0")))
				.isInstanceOf(SimpleArtifactCoordinates.class);
	}

}
