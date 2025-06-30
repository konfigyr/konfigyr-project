package com.konfigyr.test;

import com.konfigyr.account.Account;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.test.assertions.ProblemDetailAssert;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import org.assertj.core.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.core.ResolvableType;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.RequestContextFilter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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

	@BeforeEach
	final void registerSigningKeyStub() {
		final JWK key = KeyGenerator.getInstance().get();

		stubFor(
				get(urlPathEqualTo("/oauth/jwks"))
						.willReturn(jsonResponse(
								new JWKSet(key).toString(true), 200
						))
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
	 * Creates a consumer that is used to assert if {@link MvcTestResult} contained a resolved exception
	 * with a matching type.
	 *
	 * @param expectedType expected exception type, can't be {@literal null}
	 * @return the MVC resolved exception consumer
	 * @param <T> expected exception type
	 */
	protected static <T extends Throwable> ThrowingConsumer<MvcTestResult> hasFailedWithException(@NonNull Class<T> expectedType) {
		return hasFailedWithException(expectedType, ignore -> {});
	}

	/**
	 * Creates a consumer that is used to assert if {@link MvcTestResult} contained a resolved exception
	 * with a matching type.
	 *
	 * @param expectedType expected exception type, can't be {@literal null}
	 * @param consumer consumer function to assert resolved exception, can't be {@literal null}
	 * @return the MVC resolved exception consumer
	 * @param <T> expected exception type
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends Throwable> ThrowingConsumer<MvcTestResult> hasFailedWithException(
			@NonNull Class<T> expectedType,
			@NonNull ThrowingConsumer<ThrowableAssert<T>> consumer
	) {
		return result -> consumer.accept(
				(ThrowableAssert<T>) Assertions.assertThat(result.getMvcResult().getResolvedException())
						.isInstanceOf(expectedType)
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

	/**
	 * Creates consumer that can be used to assert that the {@link ProblemDetail} contains a
	 * {@link HttpStatus#FORBIDDEN} response status.
	 *
	 * @return the problem detail consumer
	 * @see #problemDetailFor(HttpStatusCode, ThrowingConsumer)
	 */
	protected static ThrowingConsumer<MvcTestResult> forbidden() {
		return forbidden(ignore -> { /* noop */ });
	}

	/**
	 * Creates consumer that can be used to assert that the {@link ProblemDetail} contains a
	 * {@link HttpStatus#FORBIDDEN} response status.
	 *
	 * @param consumer consumer function to assert {@link ProblemDetail}
	 * @return the problem detail consumer
	 * @see #problemDetailFor(HttpStatusCode, ThrowingConsumer)
	 */
	protected static ThrowingConsumer<MvcTestResult> forbidden(@NonNull ThrowingConsumer<ProblemDetailAssert> consumer) {
		return problemDetailFor(HttpStatus.FORBIDDEN, consumer);
	}

	/**
	 * Creates consumer that can be used to assert the {@link ProblemDetail} for missing {@link OAuthScope}.
	 * <p>
	 * This method would also assert the following values from the {@link MvcTestResult}:
	 * <ul>
	 *     <li>The HTTP status code should match {@link HttpStatus#FORBIDDEN}</li>
	 *     <li>The HTTP response content type should match {@link MediaType#APPLICATION_PROBLEM_JSON}</li>
	 *     <li>The {@link ProblemDetail} should contain the given scopes in the error details</li>
	 * </ul>
	 *
	 * @param scopes scopes to be required, can not be {@literal null}
	 * @return the problem detail consumer
	 */
	protected static ThrowingConsumer<MvcTestResult> forbidden(@NonNull OAuthScope... scopes) {
		return forbidden(problem -> problem
						.hasTitle("Access Denied")
						.hasDetailContaining(OAuthScopes.of(scopes).toString())
		);
	}

	/**
	 * Creates a {@link RequestPostProcessor} that would generate a JWT OAuth2 Access Token using the
	 * given subject claim and append it as an {@link HttpHeaders#AUTHORIZATION} header to the mock request.
	 * <p>
	 * The JWT contains the {@code iss} claim that uses the current Wiremock Server URL and a {@code sub} claim
	 * that uses the value extracted from the authentication name.
	 *
	 * @param subject subject claim for which access token is generated, can't be {@literal null}
	 * @return the post processor, never {@literal null}
	 */
	@NonNull
	protected static RequestPostProcessor authentication(@NonNull String subject, OAuthScope... scopes) {
		return authentication(claims -> claims
				.subject(subject)
				.claim("scope", Arrays.stream(scopes)
						.map(OAuthScope::getAuthority)
						.collect(Collectors.joining(" "))
				)
		);
	}

	/**
	 * Creates a {@link RequestPostProcessor} that would generate a JWT OAuth2 Access Token from this
	 * {@link Authentication} and append it as an {@link HttpHeaders#AUTHORIZATION} header to the mock request.
	 * <p>
	 * The JWT contains the {@code iss} claim that uses the current Wiremock Server URL and a {@code sub} claim
	 * that uses the value extracted from the authentication name.
	 *
	 * @param authentication authentication for which access token is generated, can't be {@literal null}
	 * @return the post processor, never {@literal null}
	 */
	@NonNull
	protected static RequestPostProcessor authentication(@NonNull Authentication authentication, OAuthScope... scopes) {
		return authentication(authentication.getName(), scopes);
	}

	/**
	 * Creates a {@link RequestPostProcessor} that would generate a JWT OAuth2 Access Token from this
	 * {@link Account} and append it as an {@link HttpHeaders#AUTHORIZATION} header to the mock request.
	 * <p>
	 * The JWT contains the {@code iss} claim that uses the current Wiremock Server URL and a {@code sub} claim
	 * that uses the serialized account entity identifier value.
	 *
	 * @param account account for which access token is generated, can't be {@literal null}
	 * @return the post processor, never {@literal null}
	 */
	@NonNull
	protected static RequestPostProcessor authentication(Account account, OAuthScope... scopes) {
		return authentication(TestPrincipals.from(account), scopes);
	}

	/**
	 * Creates a {@link RequestPostProcessor} that would generate a JWT OAuth2 Access Token and append
	 * it as an {@link HttpHeaders#AUTHORIZATION} header to the mock request.
	 * <p>
	 * The JWT contains the {@code iss} claim that uses the current Wiremock Server URL. Use supplier function
	 * append add claims to the JWT.
	 *
	 * @param customizer supplier function that is used to customize JWT claims, can't be {@literal null}
	 * @return the post processor, never {@literal null}
	 */
	@NonNull
	protected static RequestPostProcessor authentication(@NonNull Consumer<JWTClaimsSet.Builder> customizer) {
		return request -> {
			final var instant = Instant.now();

			final var claims = new JWTClaimsSet.Builder()
					.jwtID(String.valueOf(instant.toEpochMilli()))
					.notBeforeTime(Date.from(instant))
					.issueTime(Date.from(instant))
					.issuer(wiremock.baseUrl());

			customizer.accept(claims);

			final var token = KeyGenerator.getInstance()
					.sign(claims.build())
					.serialize();

			request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

			return request;
		};
	}

}
