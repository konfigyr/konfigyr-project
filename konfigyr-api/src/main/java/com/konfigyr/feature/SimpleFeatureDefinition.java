package com.konfigyr.feature;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.Comparator;
import java.util.Objects;

record SimpleFeatureDefinition<T extends FeatureValue>(String name, Class<T> type) implements FeatureDefinition<T> {

	@Serial
	private static final long serialVersionUID = 294304163437354662L;

	SimpleFeatureDefinition {
		Assert.hasText(name, "Feature definition name cannot be empty");
		Assert.notNull(type, "Feature definition value type cannot be null");
	}

	@Override
	public int compareTo(@NonNull FeatureDefinition<?> o) {
		return Objects.compare(name(), o.name(), Comparator.naturalOrder());
	}

}
