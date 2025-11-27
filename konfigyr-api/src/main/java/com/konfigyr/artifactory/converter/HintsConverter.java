package com.konfigyr.artifactory.converter;

import org.jooq.Converter;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ResolvableType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public final class HintsConverter implements Converter<String, List<String>> {

	private static final ResolvableType HINTS_TYPE = ResolvableType.forClassWithGenerics(List.class, String.class);

	@Override
	public List<String> from(String databaseObject) {
		if (databaseObject == null) {
			return null;
		}

		final String[] hints = StringUtils.commaDelimitedListToStringArray(databaseObject);
		return List.of(hints);
	}

	@Override
	public String to(List<String> userObject) {
		return CollectionUtils.isEmpty(userObject) ? null : StringUtils.collectionToCommaDelimitedString(userObject);
	}

	@NonNull
	@Override
	public Class<String> fromType() {
		return String.class;
	}

	@NonNull
	@Override
	@SuppressWarnings("unchecked")
	public Class<List<String>> toType() {
		return (Class<List<String>>) HINTS_TYPE.resolve(List.class);
	}
}
