package com.konfigyr.mcp.invoke;

import com.konfigyr.entity.EntityId;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThatObject;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpParameterTypeConverterTest {

	@Mock
	McpHandlerParameter parameter;

	@Mock
	McpJsonMapper jsonMapper;

	ConversionService conversionService;

	McpParameterTypeConverter converter;

	@BeforeEach
	void setup() {
		conversionService = spy(DefaultConversionService.getSharedInstance());
		converter = new McpParameterTypeConverter(() -> conversionService, jsonMapper);
	}

	@Test
	@DisplayName("should return null without converting when the raw value is null")
	void shouldReturnNullForNullValue() {
		assertThatObject(converter.convert(parameter, null))
				.isNull();

		verifyNoInteractions(conversionService);
		verifyNoInteractions(jsonMapper);
		verifyNoInteractions(parameter);
	}

	@Test
	@DisplayName("should convert a value using the conversion service when it can")
	void shouldConvertUsingConversionService() {
		final var type = ResolvableType.forClass(Integer.class);
		doReturn(type).when(parameter).type();

		assertThatObject(converter.convert(parameter, "42"))
				.isEqualTo(42);

		verifyNoInteractions(jsonMapper);
	}

	@Test
	@DisplayName("should fall back to the JSON mapper when the conversion service can't convert the value")
	void shouldFallBackToJsonMapperForStructuredValues() {
		final var value = EntityId.from(42);

		doReturn(ResolvableType.forInstance(value)).when(parameter).type();
		doReturn(value).when(jsonMapper).convertValue(value.serialize(), value.getClass());

		assertThatObject(converter.convert(parameter, value.serialize()))
				.isEqualTo(value);

		verify(conversionService, never()).convert(any(), any(), any());
	}

}
