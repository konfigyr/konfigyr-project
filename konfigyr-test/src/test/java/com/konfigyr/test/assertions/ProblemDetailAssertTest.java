package com.konfigyr.test.assertions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProblemDetailAssertTest {

	@Test
	@DisplayName("should assert problem details")
	void shouldAssertProblemDetails() {
		final var problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problem.setTitle("Unauthorized");
		problem.setDetail("Unauthorized access");
		problem.setInstance(URI.create("https://example.com/namespaces"));
		problem.setProperty("foo", "bar");

		assertThat(problem)
				.asInstanceOf(ProblemDetailAssert.factory())
				.hasDefaultType()
				.hasInstance("https://example.com/namespaces")
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.hasTitle("Unauthorized")
				.hasTitle("Unauthorized")
				.hasDetail("Unauthorized access")
				.hasDetailContaining("access")
				.hasProperty("foo", "bar")
				.hasPropertySatisfying("foo", it -> assertThat(it).isEqualTo("bar"));

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasType("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that Problem Details should have a type of \"invalid\" but was \"null\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasInstance("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that Problem Details should have an instance of \"invalid\" but was \"https://example.com/namespaces\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasStatus(500))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that Problem Details should have a status of \"500\" but was \"401\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasTitle("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that Problem Details should have a title of \"invalid\" but was \"Unauthorized\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasTitleContaining("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Problem Details should have a title containing \"invalid\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasDetail("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Expected that Problem Details should have a detail of \"invalid\" but was \"Unauthorized access\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasDetailContaining("invalid"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Problem Details should have a detail containing \"invalid\"");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem).hasProperty("foo", "baz"))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining("Problem Details should have a \"foo\" property that has a value of: baz");

		assertThatThrownBy(() -> ProblemDetailAssert.assertThat(problem)
				.hasPropertySatisfying("foo", it -> assertThat(it).isEqualTo("baz")))
				.isInstanceOf(AssertionError.class);
	}

}
