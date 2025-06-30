package com.konfigyr.web.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;

/**
 * A {@link HttpMessageConverter} for {@link ProblemDetail RFC 9457 problem detail}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ProblemDetail
 */
public class ProblemDetailHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	public ProblemDetailHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	public ProblemDetailHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Override
	protected boolean supports(@NonNull Class<?> clazz) {
		return ClassUtils.isAssignable(ProblemDetail.class, clazz);
	}

	@Override
	public boolean canRead(@NonNull Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && super.canRead(clazz, mediaType);
	}

	@Override
	public boolean canWrite(@NonNull Class<?> clazz, MediaType mediaType) {
		return supports(clazz) && super.canWrite(clazz, mediaType);
	}
}
