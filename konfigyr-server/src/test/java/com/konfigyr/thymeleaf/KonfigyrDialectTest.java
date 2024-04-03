package com.konfigyr.thymeleaf;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.testing.templateengine.engine.TestExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class KonfigyrDialectTest {

	static TestExecutor executor;
	static MockEnvironment environment;

	@BeforeAll
	static void createExecutor() {
		final var builder = new KonfigyrProcessingContextBuilder();
		builder.setApplicationContextConfigLocation("classpath:/thymeleaf/test-context.xml");

		environment = new MockEnvironment();
		executor = new TestExecutor(builder);
		executor.setDialects(List.of(new SpringStandardDialect(), new KonfigyrDialect(environment)));
	}

	@BeforeEach
	void setupEnvironment() {
		environment.setActiveProfiles("test");
	}

	@AfterEach
	void resetExecutor() {
		executor.reset();
	}

	@ValueSource(strings = { "test", "local" })
	@ParameterizedTest(name = "should render test selector for profile: {0}")
	@DisplayName("should render test selector data attribute")
	void shouldRenderTestSelector(String profile) {
		environment.setActiveProfiles(profile);

		assertThatNoException().isThrownBy(() -> executor.execute("classpath:thymeleaf/test-selectors"));

		assertThat(executor.isAllOK())
				.as("Should execute all test selector scenarios without errors")
				.isTrue();
	}

	@ValueSource(strings = { "other", "staging", "production" })
	@ParameterizedTest(name = "should not render test selector for profile: {0}")
	@DisplayName("should not render test selector data attribute")
	void shouldNotRenderTestSelector(String profile) {
		environment.setActiveProfiles(profile);

		assertThatNoException().isThrownBy(() -> executor.execute("classpath:thymeleaf/ignore-test-selectors"));

		assertThat(executor.isAllOK())
				.as("Should execute all test selector scenarios without errors")
				.isTrue();
	}

	@Test
	@DisplayName("should create form control for a field using `#forms.create`")
	void shouldCreateFormControl() {
		assertThatNoException().isThrownBy(() -> executor.execute("classpath:thymeleaf/form-control"));

		assertThat(executor.isAllOK())
				.as("Should execute all test selector scenarios without errors")
				.isTrue();
	}

}
