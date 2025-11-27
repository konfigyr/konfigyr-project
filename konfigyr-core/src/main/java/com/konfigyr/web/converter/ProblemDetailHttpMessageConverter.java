package com.konfigyr.web.converter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * A {@link HttpMessageConverter} for {@link ProblemDetail RFC 9457 problem detail}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProblemDetail
 */
@NullMarked
public class ProblemDetailHttpMessageConverter extends AbstractJacksonHttpMessageConverter<ObjectMapper> {

	public ProblemDetailHttpMessageConverter() {
		this(JsonMapper.builder()
				.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
				.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
				.build());
	}

	public ProblemDetailHttpMessageConverter(ObjectMapper mapper) {
		super(mapper, MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return ClassUtils.isAssignable(ProblemDetail.class, clazz);
	}

	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return supports(clazz) && super.canRead(clazz, mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return supports(clazz) && super.canWrite(clazz, mediaType);
	}
}
