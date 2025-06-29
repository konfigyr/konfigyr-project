package com.konfigyr.web.filter;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.*;

class ContextFilterTest {

	private ContextFilter filter;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	void setup() {
		filter = new ContextFilter();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		request.setMethod("DELETE");
		request.setRequestURI("/test-url");
		request.setRemoteAddr("remote-address");
	}

	@AfterEach
	void cleanup() {
		MDC.clear();
	}

	@Test
	@DisplayName("should filter request when matcher matches")
	void shouldFilterRequestWhenMatcherMatches() throws Exception {
		final var filter = new ContextFilter(PathPatternRequestMatcher.withDefaults().matcher("/test-url/**"));

		filter(filter, "/test-url", (request, response) ->
				assertThat(MDC.getCopyOfContextMap()).isNotEmpty());

		filter(filter, "/test-url/sub-path", (request, response) ->
				assertThat(MDC.getCopyOfContextMap()).isNotEmpty());

		filter(filter, "/accounts", (request, response) ->
				assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty());
	}

	@Test
	@DisplayName("should clear context when chain throws an exception")
	void shouldClearContext() {
		final var cause = new RuntimeException("context must be cleared");

		assertThatException().isThrownBy(() -> filter(filter, "/test-url", (request, response) -> {
			assertThat(MDC.getCopyOfContextMap())
					.isNotEmpty();

			assertThat(LocaleContextHolder.getLocaleContext())
					.isNotNull();

			throw cause;
		})).isEqualTo(cause);

		assertThat(response.getHeader(ContextFilter.X_REQUEST_ID))
				.isNotBlank();

		assertThat(MDC.getCopyOfContextMap())
				.isNullOrEmpty();

		assertThat(LocaleContextHolder.getLocaleContext())
				.isNull();
	}

	@Test
	@DisplayName("should extract MDC context from the incoming request")
	void shouldSetContext() throws Exception {
		MDC.put("existing-key", "existing-value");

		filter(filter, "/test-url", (request, response) -> {
			assertThat(MDC.getCopyOfContextMap())
					.hasSize(7)
					.containsEntry("existing-key", "existing-value")
					.containsEntry("ip", "remote-address")
					.containsEntry("host", "http://localhost")
					.containsEntry("method", "DELETE")
					.containsEntry("uri", "/test-url")
					.containsEntry("locale", request.getLocale().toString())
					.containsEntry("rid", response.getHeader(ContextFilter.X_REQUEST_ID));

			assertThat(LocaleContextHolder.getLocaleContext())
					.isNotNull()
					.returns(request.getLocale(), LocaleContext::getLocale)
					.returns(TimeZone.getDefault(), LocaleContextHolder::getTimeZone);
		});

		assertThat(response.getHeader(ContextFilter.X_REQUEST_ID))
				.hasSize(32)
				.isHexadecimal()
				.isPrintable()
				.isLowerCase();

		assertThat(MDC.getCopyOfContextMap())
				.hasSize(1)
				.containsEntry("existing-key", "existing-value");

		assertThat(LocaleContextHolder.getLocaleContext())
				.isNull();
	}

	@Test
	@DisplayName("should extract MDC context from proxied request")
	void shouldSetContextWithExistingRequestIdentifier() throws Exception {
		request.addHeader(ContextFilter.X_REQUEST_ID, "existing-rid");
		request.addHeader(ContextFilter.X_FORWARDED_FOR, "forwarded-address");
		request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, Locale.GERMANY.toLanguageTag());
		request.setScheme("http");
		request.setServerName("konfigyr.com");

		filter(filter, "/test-url", (request, response) -> {
			assertThat(MDC.getCopyOfContextMap())
					.hasSize(6)
					.containsEntry("ip", "forwarded-address")
					.containsEntry("host", "http://konfigyr.com")
					.containsEntry("method", "DELETE")
					.containsEntry("uri", "/test-url")
					.containsEntry("rid", "existing-rid")
					.containsEntry("locale", Locale.GERMANY.toString());

			assertThat(LocaleContextHolder.getLocaleContext())
					.isNotNull()
					.returns(Locale.GERMANY, LocaleContext::getLocale)
					.returns(TimeZone.getDefault(), LocaleContextHolder::getTimeZone);
		});

		assertThat(response.getHeader(ContextFilter.X_REQUEST_ID))
				.isEqualTo("existing-rid");

		assertThat(MDC.getCopyOfContextMap())
				.isEmpty();

		assertThat(LocaleContextHolder.getLocaleContext())
				.isNull();
	}

	@Test
	@DisplayName("should use request identifier from the incoming request")
	void shouldExtractRequestIdentifierFromRequest() {
		final var generator = mock(StringKeyGenerator.class);

		request.addHeader(ContextFilter.X_REQUEST_ID, "existing-rid");

		assertThat(ContextFilter.extractRequestId(generator, request))
				.isEqualTo("existing-rid");

		verifyNoInteractions(generator);
	}

	@Test
	@DisplayName("should generate request identifier")
	void shouldGenerateRequestIdentifier() {
		final var generator = mock(StringKeyGenerator.class);
		doReturn("generated").when(generator).generateKey();

		assertThat(ContextFilter.extractRequestId(generator, request))
				.isEqualTo("generated");

		verify(generator).generateKey();
	}

	@Test
	@DisplayName("should fallback to UUID when request generator fails")
	void shouldFallbackToUUIDRequestIdentifier() {
		final var generator = mock(StringKeyGenerator.class);
		doThrow(IllegalArgumentException.class).when(generator).generateKey();

		assertThat(ContextFilter.extractRequestId(generator, request))
				.isNotBlank()
				.hasSize(36);

		verify(generator).generateKey();
	}

	@Test
	@DisplayName("should extract locale context from locale resolver")
	void shouldExtractContextFromLocaleResolver() {
		request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, Locale.FRANCE.toLanguageTag());

		assertThat(ContextFilter.extractLocaleContext(new AcceptHeaderLocaleResolver(), request))
				.isNotNull()
				.returns(Locale.FRANCE, LocaleContext::getLocale)
				.returns(TimeZone.getDefault(), LocaleContextHolder::getTimeZone);
	}

	@Test
	@DisplayName("should extract locale context from locale context resolver")
	void shouldExtractContextFromLocaleContextResolver() {
		final var resolver = new FixedLocaleResolver(Locale.ITALIAN, TimeZone.getTimeZone("UTC"));

		assertThat(ContextFilter.extractLocaleContext(resolver, request))
				.isNotNull()
				.returns(Locale.ITALIAN, LocaleContext::getLocale)
				.returns(TimeZone.getTimeZone("UTC"), LocaleContextHolder::getTimeZone);
	}

	@Test
	@DisplayName("should extract locale from locale context")
	void shouldExtractLocaleFromLocaleContext() {
		assertThat(ContextFilter.extractLocale(() -> null))
				.isNotBlank()
				.isEqualTo(Locale.US.toString());

		assertThat(ContextFilter.extractLocale(() -> Locale.UK))
				.isNotBlank()
				.isEqualTo(Locale.UK.toString());
	}

	@MethodSource("hosts")
	@DisplayName("should extract host from incoming request")
	@ParameterizedTest(name = "should generate host from: [schema={0}, name={1}, port={2}]")
	void shouldExtractHost(String schema, String name, int port, String expected) {
		request.setScheme(schema);
		request.setServerName(name);
		request.setServerPort(port);

		assertThat(ContextFilter.extractHost(request))
				.describedAs("Should generate %s from [schema=%s, name=%s, port=%s]", expected, schema, name, port)
				.isEqualTo(expected);
	}

	@Test
	@DisplayName("should add non-blank values to MDC context")
	void shouldAddNonBlankValuesToMDC() {
		final List<Closeable> closeables = new ArrayList<>();
		ContextFilter.add("null", () -> null, closeables::add);
		ContextFilter.add("empty", () -> "", closeables::add);
		ContextFilter.add("blank", () -> "  ", closeables::add);
		ContextFilter.add("valid", () -> "value", closeables::add);
		ContextFilter.add("throwing", () -> {
			throw new IllegalArgumentException();
		}, closeables::add);

		assertThat(closeables)
				.hasSize(1);

		assertThat(MDC.getCopyOfContextMap())
				.hasSize(1)
				.containsEntry("valid", "value");

		closeables.forEach(IOUtils::closeQuietly);

		assertThat(MDC.getCopyOfContextMap())
				.isNullOrEmpty();
	}

	void filter(ContextFilter filter, String path, BiConsumer<MockHttpServletRequest, MockHttpServletResponse> chain) throws Exception {
		request.setPathInfo(path);
		request.setRequestURI(path);

		filter.doFilter(request, response, (request, response) -> {
			assertThat(request)
					.isNotNull()
					.isSameAs(this.request);

			assertThat(response)
					.isNotNull()
					.isSameAs(this.response);

			chain.accept(this.request, this.response);
		});
	}

	static Stream<Arguments> hosts() {
		return Stream.of(
				Arguments.of("http", "konfigyr.com", 80, "http://konfigyr.com"),
				Arguments.of("http", "konfigyr.com", 8080, "http://konfigyr.com:8080"),
				Arguments.of("https", "konfigyr.com", 443, "https://konfigyr.com"),
				Arguments.of("https", "konfigyr.com", 8443, "https://konfigyr.com:8443")
		);
	}

}
