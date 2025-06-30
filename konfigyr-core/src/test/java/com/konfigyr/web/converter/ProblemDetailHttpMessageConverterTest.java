package com.konfigyr.web.converter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ProblemDetailHttpMessageConverterTest {

	static final ObjectNode PROBLEM_DETAIL_JSON = JsonNodeFactory.instance.objectNode()
				.put("type", "https://konfigyr.com/docs/errors/error-type")
				.put("title", "Error title")
				.put("status", 400)
				.put("detail", "Oops")
				.put("instance", "https://konfigyr.com/uri")
				.put("error-code", "error-type");

	final HttpMessageConverter<Object> converter = new ProblemDetailHttpMessageConverter();

	ProblemDetail problemDetail;
	MockHttpOutputMessage response;

	@BeforeEach
	void setUp() {
		response = new MockHttpOutputMessage();
		problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Oops");
		problemDetail.setTitle("Error title");
		problemDetail.setProperty("error-code", "error-type");
		problemDetail.setType(URI.create("https://konfigyr.com/docs/errors/error-type"));
		problemDetail.setInstance(URI.create("https://konfigyr.com/uri"));
	}

	@Test
	@DisplayName("should be able to write problem detail response without specified content type")
	void shouldWriteWithoutContentType() {
		assertThat(converter.canWrite(problemDetail.getClass(), null))
				.isTrue();

		assertThatNoException().isThrownBy(() -> converter.write(problemDetail, null, response));

		assertProblemDetailResponse(response);
	}

	@Test
	@DisplayName("should be able to read problem detail response without specified content type")
	void shouldReadWithoutContentType() throws IOException {
		assertThat(converter.canRead(problemDetail.getClass(), null))
				.isTrue();

		final var request = new MockHttpInputMessage(PROBLEM_DETAIL_JSON.toPrettyString().getBytes());

		assertThat(converter.read(ProblemDetail.class, request))
				.isEqualTo(problemDetail);
	}

	@Test
	@DisplayName("should be able to write problem detail response with specified content type")
	void shouldWriteWithContentType() {
		assertThat(converter.canWrite(problemDetail.getClass(), MediaType.APPLICATION_PROBLEM_JSON))
				.isTrue();

		assertThatNoException().isThrownBy(() -> converter.write(problemDetail, MediaType.APPLICATION_PROBLEM_JSON, response));

		assertProblemDetailResponse(response);
	}

	@Test
	@DisplayName("should be able to read problem detail response with specified content type")
	void shouldReadWithContentType() throws IOException {
		assertThat(converter.canRead(problemDetail.getClass(), null))
				.isTrue();

		final var request = new MockHttpInputMessage(PROBLEM_DETAIL_JSON.toPrettyString().getBytes());
		request.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

		assertThat(converter.read(ProblemDetail.class, request))
				.isEqualTo(problemDetail);
	}

	@Test
	@DisplayName("converter should not write unsupported content types")
	void shouldNotWriteWithUnsupportedContentType() {
		assertThat(converter.canWrite(problemDetail.getClass(), MediaType.APPLICATION_XML))
				.isFalse();
	}

	@Test
	@DisplayName("converter should not read unsupported content types")
	void shouldNotReadWithUnsupportedContentType() {
		assertThat(converter.canRead(problemDetail.getClass(), MediaType.APPLICATION_XML))
				.isFalse();
	}

	@Test
	@DisplayName("converter should not write unsupported body types")
	void shouldNotWriteWithUnsupportedBodyType() {
		assertThat(converter.canWrite(IllegalArgumentException.class, MediaType.APPLICATION_PROBLEM_JSON))
				.isFalse();
	}

	@Test
	@DisplayName("converter should not read unsupported body types")
	void shouldNotReadWithUnsupportedBodyType() {
		assertThat(converter.canRead(IllegalArgumentException.class, MediaType.APPLICATION_PROBLEM_JSON))
				.isFalse();
	}

	static void assertProblemDetailResponse(MockHttpOutputMessage response) {
		assertThat(response.getHeaders().getContentType())
				.isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

		assertThat(response.getBodyAsString())
				.isNotBlank()
				.isEqualTo(PROBLEM_DETAIL_JSON.toString());
	}

}
