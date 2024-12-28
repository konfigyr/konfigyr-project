package com.konfigyr.data;

import com.konfigyr.entity.EntityId;
import org.jooq.ConverterProvider;
import org.jooq.exception.DataTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.GenericConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SpringConverterProviderTest {

	GenericConversionService conversionService;
	ConverterProvider converterProvider;

	@BeforeEach
	void setup() {
		conversionService = spy(new GenericConversionService());
		converterProvider = new SpringConverterProvider(() -> conversionService);
	}

	@Test
	@DisplayName("should use default conversion provider")
	void shouldUseDefaultConversionProvider() {
		assertThat(converterProvider.provide(String.class, Long.class))
				.isNotNull()
				.returns("1", it -> it.to(1L))
				.returns(1L, it -> it.from("1"));

		verifyNoInteractions(conversionService);
	}

	@Test
	@DisplayName("should use spring conversion service to create read/write converter")
	void shouldCreateReadWriteConverter() {
		conversionService.addConverter(Long.class, EntityId.class, EntityId::from);
		conversionService.addConverter(EntityId.class, Long.class, EntityId::get);

		final var id = EntityId.from(123568125L);

		assertThat(converterProvider.provide(Long.class, EntityId.class))
				.isNotNull()
				.returns(id.get(), it -> it.to(id))
				.returns(id, it -> it.from(id.get()))
				.returns(null, it -> it.to(null))
				.returns(null, it -> it.from(null));

		verify(conversionService).canConvert(Long.class, EntityId.class);
		verify(conversionService).canConvert(EntityId.class, Long.class);
		verify(conversionService).convert(id, Long.class);
		verify(conversionService).convert(id.get(), EntityId.class);
	}

	@Test
	@DisplayName("should use spring conversion service to create read only converter")
	void shouldCreateReadOnlyConverter() {
		conversionService.addConverter(String.class, EntityId.class, EntityId::from);

		final var id = EntityId.from(98126512354L);

		assertThat(converterProvider.provide(String.class, EntityId.class))
				.isNotNull()
				.returns(id, it -> it.from(id.serialize()))
				.returns(null, it -> it.from(null))
				.satisfies(it -> assertThatThrownBy(() -> it.to(id))
						.isInstanceOf(DataTypeException.class)
						.hasMessageContaining("not implemented")
				);

		verify(conversionService).canConvert(String.class, EntityId.class);
		verify(conversionService).canConvert(EntityId.class, String.class);
		verify(conversionService).convert(id.serialize(), EntityId.class);
		verify(conversionService, times(0)).convert(id, String.class);
	}

	@Test
	@DisplayName("should fail to find converter")
	void shouldNotFindConverter() {
		assertThat(converterProvider.provide(String.class, EntityId.class)).isNull();

		verify(conversionService).canConvert(String.class, EntityId.class);
		verify(conversionService, times(0)).canConvert(EntityId.class, String.class);
	}

}
