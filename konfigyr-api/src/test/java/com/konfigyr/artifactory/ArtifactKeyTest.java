package com.konfigyr.artifactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ArtifactKeyTest {

	final JsonMapper mapper = JsonMapper.builder().build();

	@ValueSource(strings = {
			"com.konfigyr:konfigyr-api",
			"org.eclipse.aether:aether-impl",
			"com.google.code.findbugs:annotations",
			"org.sonatype.sisu:sisu-guice"
	})
	@ParameterizedTest(name = "should parse key: {0}")
	@DisplayName("should create ArtifactKey from Maven groupId:artifactId string")
	void shouldParseKey(String key) {
		assertThat(ArtifactKey.parse(key))
				.isNotNull()
				.isInstanceOf(SimpleArtifactKey.class)
				.hasToString(key);
	}

	@Test
	@DisplayName("should fail to parse ArtifactKey from null, empty, blank or invalid key")
	void shouldFailToParseKey() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.parse(null))
				.withMessageContaining("Artifact key must not be null or blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.parse(""))
				.withMessageContaining("Artifact key must not be null or blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.parse("   "))
				.withMessageContaining("Artifact key must not be null or blank")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.parse("invalid key"))
				.withMessageContaining("Invalid Artifact key: invalid key")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.parse("com.konfigyr:konfigyr-api:1.0.0"))
				.withMessageContaining("Invalid Artifact key: com.konfigyr:konfigyr-api:1.0.0")
				.withNoCause();
	}

	@Test
	@DisplayName("should create Artifact key from fields")
	void createKey() {
		assertThat(ArtifactKey.of("com.konfigyr", "konfigyr-api"))
				.returns("com.konfigyr", ArtifactKey::groupId)
				.returns("konfigyr-api", ArtifactKey::artifactId)
				.hasToString("com.konfigyr:konfigyr-api")
				.isEqualTo(ArtifactKey.parse("com.konfigyr:konfigyr-api"))
				.isNotEqualTo(ArtifactKey.parse("com.konfigyr:konfigyr-identity"));
	}

	@Test
	@DisplayName("should fail to create Artifact key from blank fields")
	void shouldFailToCreateKeyFromBlankFields() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.of("", "konfigyr-api"))
				.withMessageContaining("Group ID cannot be empty")
				.withNoCause();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> ArtifactKey.of("com.konfigyr", ""))
				.withMessageContaining("Artifact ID cannot be empty")
				.withNoCause();
	}

	@Test
	@DisplayName("should serialize Artifact key to JSON string")
	void serializeKey() throws IOException {
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-api");

		assertThat(mapper.writeValueAsString(key))
				.isEqualTo("\"com.konfigyr:konfigyr-api\"");
	}

	@Test
	@DisplayName("should deserialize Artifact key from JSON string")
	void deserializeKey() throws IOException {
		assertThat(mapper.readValue("\"com.konfigyr:konfigyr-api\"", ArtifactKey.class))
				.isEqualTo(ArtifactKey.of("com.konfigyr", "konfigyr-api"))
				.isInstanceOf(SimpleArtifactKey.class);
	}

}
