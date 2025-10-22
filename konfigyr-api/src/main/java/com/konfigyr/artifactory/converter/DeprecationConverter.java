package com.konfigyr.artifactory.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konfigyr.artifactory.Deprecation;
import com.konfigyr.data.converter.JsonConverter;
import org.jooq.Converter;
import org.springframework.lang.NonNull;

public final class DeprecationConverter implements Converter<String, Deprecation> {

	private final Converter<String, Deprecation> delegate;

	public DeprecationConverter() {
		this.delegate = JsonConverter.create(Deprecation.class);
	}

	public DeprecationConverter(ObjectMapper mapper) {
		this.delegate = JsonConverter.create(mapper, Deprecation.class);
	}

	@NonNull
	@Override
	public Class<String> fromType() {
		return delegate.fromType();
	}

	@NonNull
	@Override
	public Class<Deprecation> toType() {
		return delegate.toType();
	}

	@Override
	public Deprecation from(String databaseObject) {
		return delegate.from(databaseObject);
	}

	@Override
	public String to(Deprecation userObject) {
		return delegate.to(userObject);
	}
}

