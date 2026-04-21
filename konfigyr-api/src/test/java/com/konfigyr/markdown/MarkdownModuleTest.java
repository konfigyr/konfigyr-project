package com.konfigyr.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;

import static org.assertj.core.api.Assertions.*;

class MarkdownModuleTest {

	final JsonMapper mapper = JsonMapper.builder()
			.addModule(new MarkdownModule(new CommonmarkMarkdownParser()))
			.build();

	@Test
	@DisplayName("should serialize markdown contents")
	void serializeMarkdown() {
		assertThat(mapper.writeValueAsString(MarkdownContents.of("# Hello world")))
				.isEqualTo("{\"markdown\":\"# Hello world\",\"html\":\"<h1>Hello world</h1>\\n\"}");
	}

	@ValueSource(strings = { "", " ", "  ", "# Hello world" })
	@DisplayName("should deserialize markdown contents")
	@ParameterizedTest(name = "should deserialize markdown contents: {0}")
	void deserializeMarkdown(String markdown) {
		assertThat(mapper.readValue(StringNode.valueOf(markdown).toString(), MarkdownContents.class))
				.isEqualTo(MarkdownContents.of(markdown));
	}

	@Test
	@DisplayName("should deserialize markdown contents as JSON Object")
	void deserializeMarkdownObject() {
		final var json = "{\"checksum\":\"Uurv4bDcpiAQww9v8yHn9C/JRb0M1mXtLcliew8y7ro=\",\"markdown\":\"# Hello world\"}";

		assertThat(mapper.readValue(json, MarkdownContents.class))
				.isEqualTo(MarkdownContents.of("# Hello world"));
	}

	@ValueSource(strings = {
			"true",
			"1234567",
			"[\"# Heading\"]",
			"{\"checksum\":\"Uurv4bDcpiAQww9v8yHn9C/JRb0M1mXtLcliew8y7ro=\"}"
	})
	@DisplayName("should fail to deserialize markdown contents")
	@ParameterizedTest(name = "should fail to deserialize markdown JSON: {0}")
	void deserializeInvalidMarkdown(String json) {
		assertThatExceptionOfType(MismatchedInputException.class)
				.isThrownBy(() -> mapper.readValue(json, MarkdownContents.class))
				.withMessageContaining("MarkdownContents can only be deserialized from a JSON string");
	}

}
