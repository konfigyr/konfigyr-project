package com.konfigyr.vault;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.test.TestKeysetOperations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

class PropertiesTest {

	static ResourceLoader resourceLoader = new DefaultResourceLoader();
	static KeysetOperations keysetOperations = TestKeysetOperations.create(ByteArray.fromBase64String(
			"CmHPIXcF8Dw8h5iEbk6W39vWaemo7nJTd4_4-pqSGto="
	));

	static final Profile profile = Profile.builder()
			.id(EntityId.from(26451L))
			.service(EntityId.from(1))
			.slug("test-profile")
			.name("Test profile")
			.policy(ProfilePolicy.PROTECTED)
			.build();

	@Test
	@DisplayName("should create properties from the builder")
	void createProperties() {
		final var properties = createSpringProperties();

		assertThat(properties)
				.as("The deserialized properties should contain 3 entries")
				.hasSize(3);

		assertThat(properties.get("spring.application.name"))
				.hasValue(propertyValueFor("spring.application.name", "konfigyr-api"));

		assertThat(properties.has("server.port"))
				.isTrue();

		assertThat(properties.get("server.address"))
				.isEmpty();

		assertThat(properties.has("server.address"))
				.isFalse();
	}

	@Test
	@DisplayName("should apply property changes to existing property state")
	void applyChanges() {
		final var properties = createSpringProperties();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("spring.application.group", "api")
				.modifyProperty("server.port", "8081")
				.removeProperty("server.ssl.enabled")
				.build();

		final var updated = properties.apply(changes, keysetOperations);

		assertThatObject(updated)
				.isNotNull()
				.isNotEqualTo(properties)
				.returns(3, Properties::size)
				.satisfies(hasProperty("spring.application.name", "konfigyr-api"))
				.satisfies(hasProperty("spring.application.group", "api"))
				.satisfies(hasProperty("server.port", "8081"));

		assertThat(updated.has("server.ssl.enabled"))
				.isFalse();
	}

	@Test
	@DisplayName("should not seal and modify property value when value checksum is the same")
	void assertModifiedPropertyChangeIdentity() {
		final var properties = createSpringProperties();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.modifyProperty("server.port", "8080")
				.build();

		final var keyset = Mockito.spy(keysetOperations);
		final var updated = properties.apply(changes, keyset);

		assertThatObject(updated)
				.isNotNull()
				.isEqualTo(properties);

		Mockito.verifyNoInteractions(keyset);
	}

	@Test
	@DisplayName("should fail to apply property changes to when existing property is being added")
	void failToCreateExistingProperty() {
		final var properties = createSpringProperties();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("spring.application.name", "api")
				.build();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> properties.apply(changes, keysetOperations))
				.withMessageContaining("Property 'spring.application.name' already exists")
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to apply property changes to when missing property is being modified")
	void failToModifyUnknownProperty() {
		final var properties = createSpringProperties();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.modifyProperty("spring.application.group", "api")
				.build();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> properties.apply(changes, keysetOperations))
				.withMessageContaining("Property 'spring.application.group' does not exist")
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to apply property changes to when missing property is being removed")
	void failToRemoveUnknownProperty() {
		final var properties = createSpringProperties();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.removeProperty("spring.application.group")
				.build();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> properties.apply(changes, keysetOperations))
				.withMessageContaining("Property 'spring.application.group' does not exist")
				.withNoCause();
	}

	@Test
	@DisplayName("should deserialize properties from an input stream")
	void deserializeFromInputStream() throws IOException {
		final var properties = Properties.from(loadFixtures("test-properties-sealed"));

		assertThat(properties)
				.as("The deserialized properties should contain 26 entries")
				.hasSize(26);
	}

	@ValueSource(strings = {"", " ", "\n", "\r\n", "server.port", "! ignored comment", "# ignored comment"})
	@ParameterizedTest(name = "deserialized properties contents: {0}")
	@DisplayName("should deserialize properties from an empty source")
	void deserializeFromEmptySource(String contents) throws IOException {
		assertThat(Properties.from(contents))
				.isEmpty();
	}

	@ValueSource(strings = {
			"server.port=",
			"server.port= ",
			"server.port:8080",
			"server.port=8080",
			"server.port={}",
			"server.port={crypto:invalid}",
			"server.port={checksum:invalid}",
			"server.port={crypto:invalid,checksum:invalid}"
	})
	@ParameterizedTest(name = "deserialized properties contents: {0}")
	@DisplayName("should fail to deserialize properties from invalid source")
	void deserializeFromInvalidSource(String contents) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Properties.from(contents))
				.withMessageContaining("Invalid serialized property value");
	}

	@Test
	@DisplayName("should serialize properties to an input stream")
	void serializeToInputStream() throws IOException {
		final var properties = createPropertiesFromSource();

		assertThat(properties.getInputStream())
				.asString(StandardCharsets.UTF_8)
				.hasLineCount(properties.size());

		assertThatObject(Properties.from(properties.getInputStream()))
				.as("The serialized properties should be equal to the original properties")
				.isEqualTo(properties);
	}

	@Test
	@DisplayName("should serialize properties to a byte array")
	void serializeToInputArray() throws IOException {
		final var properties = createPropertiesFromSource();
		final var serialized = properties.getInputStream();

		assertThat(serialized)
				.asString(StandardCharsets.UTF_8)
				.hasLineCount(properties.size());

		assertThatObject(Properties.from(serialized))
				.as("The serialized properties should be equal to the original properties")
				.isEqualTo(properties);
	}

	@Test
	@DisplayName("should serialize properties directly to the output stream")
	void serializeToOutputStream() throws IOException {
		final var properties = createPropertiesFromSource();
		final var os = new ByteArrayOutputStream();

		assertThatNoException().isThrownBy(() -> properties.transferTo(os));

		assertThat(os.toByteArray())
				.asString(StandardCharsets.UTF_8)
				.hasLineCount(properties.size());

		assertThatObject(Properties.from(new ByteArrayInputStream(os.toByteArray())))
				.as("The serialized properties should be equal to the original properties")
				.isEqualTo(properties);
	}

	static Consumer<Properties> hasProperty(String name, String value) {
		return properties -> assertThat(properties.get(name))
				.hasValue(propertyValueFor(name, value));
	}

	static PropertyValue propertyValueFor(String name, String value) {
		return PropertyValue.create(profile.id(), name, value).seal(keysetOperations);
	}

	static Properties createSpringProperties() {
		return Properties.builder()
				.add("spring.application.name", propertyValueFor("spring.application.name", "konfigyr-api"))
				.add("server.port", propertyValueFor("server.port", "8080"))
				.add("server.ssl.enabled", propertyValueFor("server.ssl.enabled", "false"))
				.build();
	}

	static InputStream loadFixtures(String name) throws IOException {
		return resourceLoader
				.getResource("classpath:fixtures/" + name + ".properties")
				.getInputStream();
	}

	static Properties createPropertiesFromSource() throws IOException {
		final var properties = new java.util.Properties();
		properties.load(loadFixtures("test-properties-codec"));

		final var builder = Properties.builder();

		properties.forEach((key, value) -> builder.add(
				key.toString(), propertyValueFor(key.toString(), value.toString())
		));

		return builder.build();
	}

}
