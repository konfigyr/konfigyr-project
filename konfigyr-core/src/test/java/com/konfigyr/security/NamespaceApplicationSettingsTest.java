package com.konfigyr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NamespaceApplicationSettingsTest {

	final JsonMapper mapper = JsonMapper.shared();

	@Test
	@DisplayName("should serialize and deserialize AgentSettings with redirect URIs")
	void shouldSerializeAndDeserializeAgentSettings() {
		final var settings = new NamespaceApplicationSettings.AgentSettings(
				List.of("http://localhost/callback", "http://127.0.0.1/callback")
		);

		final var json = mapper.writeValueAsString(settings);

		assertThat(json)
				.contains("\"type\":\"agent\"")
				.contains("\"redirectUris\":[\"http://localhost/callback\",\"http://127.0.0.1/callback\"]");

		assertThat(mapper.readValue(json, NamespaceApplicationSettings.class))
				.isInstanceOf(NamespaceApplicationSettings.AgentSettings.class)
				.isEqualTo(settings);
	}

	@Test
	@DisplayName("should serialize and deserialize PipelineSettings with issuer URI and subject pattern")
	void shouldSerializeAndDeserializePipelineSettingsWithSubjectPattern() {
		final var settings = new NamespaceApplicationSettings.PipelineSettings(
				"https://token.actions.githubusercontent.com",
				"repo:acme/*:ref:refs/heads/main"
		);

		final var json = mapper.writeValueAsString(settings);

		assertThat(json)
				.contains("\"type\":\"pipeline\"")
				.contains("\"issuerUri\":\"https://token.actions.githubusercontent.com\"")
				.contains("\"subjectPattern\":\"repo:acme/*:ref:refs/heads/main\"");

		assertThat(mapper.readValue(json, NamespaceApplicationSettings.class))
				.isInstanceOf(NamespaceApplicationSettings.PipelineSettings.class)
				.isEqualTo(settings);
	}

	@Test
	@DisplayName("should serialize and deserialize PipelineSettings with issuer URI only")
	void shouldSerializeAndDeserializePipelineSettingsWithoutSubjectPattern() {
		final var settings = new NamespaceApplicationSettings.PipelineSettings(
				"https://token.actions.githubusercontent.com",
				null
		);

		final var json = mapper.writeValueAsString(settings);

		assertThat(json)
				.contains("\"type\":\"pipeline\"")
				.contains("\"issuerUri\":\"https://token.actions.githubusercontent.com\"");

		assertThat(mapper.readValue(json, NamespaceApplicationSettings.class))
				.isInstanceOf(NamespaceApplicationSettings.PipelineSettings.class)
				.isEqualTo(settings);
	}

}
