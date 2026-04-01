package com.konfigyr.data.web;

import com.konfigyr.data.CursorPageable;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.*;

class CursorPageableHandlerMethodArgumentResolverTest {

	MockHttpServletRequest request;
	CursorPageableHandlerMethodArgumentResolver resolver;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		resolver = new CursorPageableHandlerMethodArgumentResolver();
		resolver.setDefaultPageSize(10);
		resolver.setMaxPageSize(100);
	}

	@Test
	@DisplayName("should support method parameter of cursor pageable")
	void supportsCursorPageableMethodParameter() {
		final var method = getParameterOfMethod("supportedMethod", CursorPageable.class);

		assertThat(resolver.supportsParameter(method))
				.isTrue();
	}

	@Test
	@DisplayName("should not support method parameters that are not of cursor pageable type")
	void ignoresNonCursorPageableMethodParameters() {
		final var method = getParameterOfMethod("unsupportedMethod", String.class);

		assertThat(resolver.supportsParameter(method))
				.isFalse();
	}

	@Test
	@DisplayName("should resolve the default cursor pageable when request parameter are not specified")
	void resolveDefaultPageableForMissingParameters() {
		assertThatResolvedCursorPageable(getParameterOfMethod("supportedMethod", CursorPageable.class))
				.isEqualTo(CursorPageable.of(10));
	}

	@Test
	@DisplayName("should resolve the cursor pageable when token request parameter is not specified")
	void resolvePageableForMissingTokenParameter() {
		request.setParameter("size", "14");

		assertThatResolvedCursorPageable(getParameterOfMethod("supportedMethod", CursorPageable.class))
				.isEqualTo(CursorPageable.of(14));
	}

	@Test
	@DisplayName("should resolve the cursor pageable with max specified page size when exceeded")
	void resolvePageableForWithMaxPageSize() {
		request.setParameter("size", "9999");

		assertThatResolvedCursorPageable(getParameterOfMethod("supportedMethod", CursorPageable.class))
				.isEqualTo(CursorPageable.of(100));
	}

	@Test
	@DisplayName("should resolve the cursor pageable with default page size when invalid")
	void resolvePageableForWithInvalidPageSize() {
		request.setParameter("size", "an invalid page size");

		assertThatResolvedCursorPageable(getParameterOfMethod("supportedMethod", CursorPageable.class))
				.isEqualTo(CursorPageable.of(10));
	}

	@Test
	@DisplayName("should resolve the cursor pageable with page size and token")
	void resolvePageableWithSizeAndToken() {
		request.setParameter("size", "7");
		request.setParameter("token", "the-token");

		assertThatResolvedCursorPageable(getParameterOfMethod("supportedMethod", CursorPageable.class))
				.isEqualTo(CursorPageable.of("the-token", 7));
	}

	@Test
	@DisplayName("should resolve the cursor pageable with qualified page size and token parameters")
	void resolveQualifiedPageable() {
		request.setParameter("size", "7");
		request.setParameter("token", "the-token");
		request.setParameter("prefix_size", "17");
		request.setParameter("prefix_token", "qualified-token");

		assertThatResolvedCursorPageable(getParameterOfMethod("qualifiedMethod", CursorPageable.class))
				.isEqualTo(CursorPageable.of("qualified-token", 17));
	}

	ObjectAssert<CursorPageable> assertThatResolvedCursorPageable(MethodParameter parameter) {
		assertThat(resolver.supportsParameter(parameter))
				.as("Method parameter %s should be supported", parameter)
				.isTrue();

		return assertThat(resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null));
	}

	static MethodParameter getParameterOfMethod(String name, Class<?>... argumentTypes) {
		return ReflectionUtils.findMethod(CursorPageableController.class, name, argumentTypes)
				.map(method -> new MethodParameter(method, 0))
				.orElseThrow(() -> new IllegalArgumentException("Method not found: " + name));
	}

	@SuppressWarnings("unused")
	interface CursorPageableController {

		void supportedMethod(CursorPageable pageable);

		void unsupportedMethod(String string);

		void qualifiedMethod(@Qualifier("prefix") CursorPageable pageable);
	}

}
