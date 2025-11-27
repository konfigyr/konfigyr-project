package com.konfigyr.support;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the {@link SearchQuery}, {@link Criteria} and {@link Builder}
 * interfaces.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class CriteriaSearchQuery implements SearchQuery {

	private final @NonNull Map<Criteria<?>, Object> criteria;
	private final @NonNull Pageable pageable;

	@NonNull
	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> criteria(@NonNull Criteria<T> criteria) {
		if (CollectionUtils.isEmpty(this.criteria)) {
			return Optional.empty();
		}

		return (Optional<T>) Optional.ofNullable(this.criteria.get(criteria));
	}

	@NonNull
	@Override
	public Pageable pageable() {
		return pageable;
	}

	@NonNull
	@Override
	public SearchQuery next() {
		return new CriteriaSearchQuery(criteria, pageable.next());
	}

	record SearchQueryCriteria<T>(String name, Class<T> type) implements Criteria<T> {
		@Serial
		private static final long serialVersionUID = -2088301892487492935L;
	}

	static final class SearchQueryBuilder implements Builder {

		private Pageable pageable = Pageable.ofSize(20);
		private final Map<Criteria<?>, Object> criteria = new LinkedHashMap<>();

		SearchQueryBuilder() {
		}

		@NonNull
		@Override
		public Builder pageable(Pageable pageable) {
			this.pageable = pageable;
			return this;
		}

		@NonNull
		@Override
		public <T> Builder criteria(Criteria<T> criteria, T value) {
			if (criteria != null) {
				this.criteria.put(criteria, value);
			}
			return this;
		}

		@NonNull
		public CriteriaSearchQuery build() {
			Assert.notNull(pageable, "Search query paging instructions must not be null");

			return new CriteriaSearchQuery(Collections.unmodifiableMap(criteria), pageable);
		}
	}
}
