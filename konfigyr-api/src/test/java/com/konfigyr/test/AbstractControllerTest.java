package com.konfigyr.test;

import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.test.assertions.ProblemDetailAssert;
import org.assertj.core.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.RequestContextFilter;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

public abstract class AbstractControllerTest extends AbstractIntegrationTest {

	protected static MockMvcTester mvc;

	@BeforeAll
	protected static void setup(@NonNull WebApplicationContext context) {
		mvc = MockMvcTester.create(
				MockMvcBuilders.webAppContextSetup(context)
						.addFilter(new RequestContextFilter())
						.apply(springSecurity())
						.build()
		).withHttpMessageConverters(
				context.getBean(HttpMessageConverters.class)
		);
	}

	/**
	 * Constructs an {@link AssertFactory} for {@link PagedModel} with the given contents type.
	 *
	 * @param type the content type of the paged model, can't be {@literal null}
	 * @param <T> content type
	 * @param <A> assert type
	 * @return the paged model assert factory, never {@literal null}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected static <T, A extends AbstractAssert<?, PagedModel<T>>> AssertFactory<PagedModel<T>, A> pagedModel(@NonNull Class<T> type) {
		final ResolvableType resolvableType = ResolvableType.forClassWithGenerics(PagedModel.class, type);

		return new InstanceOfAssertFactory(
				resolvableType.resolve(PagedModel.class),
				resolvableType.resolveGenerics(),
				Assertions::assertThat
		);
	}

	/**
	 * Creates consumer that can be used to assert the {@link ProblemDetail} that is extracted
	 * from {@link MvcTestResult}.
	 * <p>
	 * This method would also assert the following values from the {@link MvcTestResult}:
	 * <ul>
	 *     <li>The HTTP status code should match the given {@link HttpStatusCode}</li>
	 *     <li>The HTTP response content type should match {@link MediaType#APPLICATION_PROBLEM_JSON}</li>
	 * </ul>
	 *
	 * @param statusCode expected status code, can't be {@literal null}
	 * @param consumer consumer function to assert {@link ProblemDetail}
	 * @return the problem detail consumer
	 */
	protected static ThrowingConsumer<MvcTestResult> problemDetailFor(
			@NonNull HttpStatusCode statusCode,
			@NonNull ThrowingConsumer<ProblemDetailAssert> consumer
	) {
		return result -> consumer.accept(
				result.assertThat()
						.hasStatus(statusCode.value())
						.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
						.bodyJson()
						.convertTo(ProblemDetailAssert.factory())
						.hasStatus(statusCode)
		);
	}

}
