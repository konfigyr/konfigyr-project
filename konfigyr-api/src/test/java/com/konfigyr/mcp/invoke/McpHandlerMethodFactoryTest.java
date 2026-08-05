package com.konfigyr.mcp.invoke;

import com.konfigyr.mcp.McpToolFixtures;
import com.konfigyr.mcp.McpToolFixtures.Greeting;
import com.konfigyr.mcp.annotation.McpToolParam;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpHandlerMethodFactoryTest {

	@Mock
	McpJsonMapper jsonMapper;

	McpHandlerMethodFactory factory;

	@BeforeEach
	void setup() {
		factory = new McpHandlerMethodFactory(jsonMapper, DefaultConversionService::getSharedInstance);
	}

	@Test
	@DisplayName("should cache the handler method per bean and method")
	void shouldCacheHandlerMethods() {
		final var greet = McpToolFixtures.method("greet");
		final var destroy = McpToolFixtures.method("destroy");

		assertThat(factory.create(McpToolFixtures.BEAN, greet)).isSameAs(factory.create(McpToolFixtures.BEAN, greet));
		assertThat(factory.create(McpToolFixtures.BEAN, greet)).isNotSameAs(factory.create(McpToolFixtures.BEAN, destroy));
	}

	@Test
	@DisplayName("should resolve each parameter's binding name and expose its return type")
	void shouldResolveParameterNamesAndReturnType() {
		final McpHandlerMethod handlerMethod = factory.create(McpToolFixtures.BEAN, McpToolFixtures.method("destroy"));

		assertThat(handlerMethod.returnType())
				.returns(void.class, ResolvableType::toClass);

		assertThat(handlerMethod)
				.hasSize(1)
				.satisfiesExactly(assertParameter("timeout", Integer.class)
						.andThen(assertParameterAnnotation(McpToolParam.class)));
	}

	@Test
	@DisplayName("should invoke the handler, binding a context object by type")
	void shouldInvokeHandlerBindingContextByType() {
		final McpHandlerMethod handlerMethod = factory.create(McpToolFixtures.BEAN, McpToolFixtures.method("whoami"));

		final Object result = handlerMethod.invoke(Map.of(), McpTransportContext.EMPTY);

		assertThat(result).isSameAs(McpTransportContext.EMPTY);
	}

	@Test
	@DisplayName("should invoke the handler, binding a named argument")
	void shouldInvokeHandlerBindingNamedArgument() {
		final McpHandlerMethod handlerMethod = factory.create(McpToolFixtures.BEAN, McpToolFixtures.method("greet"));

		final Object result = handlerMethod.invoke(Map.of("name", "World"), McpTransportContext.EMPTY);

		assertThat(result).isEqualTo(new Greeting("Hello, World"));
	}

	private static ThrowingConsumer<McpHandlerParameter> assertParameter(String name, Class<?> type) {
		return it -> {
			assertThat(it.name())
					.as("Parameter name should be %s", name)
					.isEqualTo(name);

			assertThat(it.type().toClass())
					.as("Parameter type should be %s", type)
					.isEqualTo(type);
		};
	}

	private static ThrowingConsumer<McpHandlerParameter> assertParameterAnnotation(Class<? extends Annotation> annotation) {
		return it -> assertThat(it.annotation(annotation).isPresent())
				.as("Parameter should have %s annotation", annotation.getSimpleName())
				.isTrue();
	}

}
