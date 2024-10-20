package com.konfigyr.support;

import com.konfigyr.entity.EntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SearchQueryTest {

	@Test
	@DisplayName("should create an empty query with pageable")
	void shouldCreateEmptyQueryWithPageable() {
		final var query = SearchQuery.of(Pageable.ofSize(8));

		assertThat(query.pageable())
				.isEqualTo(Pageable.ofSize(8));

		assertThat(query.term())
				.isEmpty();
	}

	@Test
	@DisplayName("should create query with criteria")
	void shouldCreateQueryWithCriteria() {
		final var query = SearchQuery.builder()
				.term("search term")
				.criteria(SearchQuery.ACCOUNT, EntityId.from(1))
				.criteria(SearchQuery.NAMESPACE, "konfigyr")
				.criteria(null, null)
				.pageable(Pageable.unpaged())
				.build();

		assertThat(query.pageable())
				.isEqualTo(Pageable.unpaged());

		assertThat(query.term())
				.hasValue("search term");

		assertThat(query.criteria(SearchQuery.ACCOUNT))
				.hasValue(EntityId.from(1));

		assertThat(query.criteria(SearchQuery.NAMESPACE))
				.hasValue("konfigyr");
	}

	@Test
	@DisplayName("should create search query with the next page")
	void shouldCreateQueryNextPage() {
		final var query = SearchQuery.builder()
				.term("search term")
				.pageable(Pageable.ofSize(1))
				.build();

		assertThat(query)
				.returns(PageRequest.of(0, 1), SearchQuery::pageable)
				.returns(Optional.of("search term"), SearchQuery::term);

		final var next = query.next();

		assertThat(next)
				.returns(PageRequest.of(1, 1), SearchQuery::pageable)
				.returns(Optional.of("search term"), SearchQuery::term)
				.isNotEqualTo(query)
				.doesNotHaveSameHashCodeAs(query);
	}

	@Test
	@DisplayName("should create criteria definition")
	void shouldCreateCriteria() {
		assertThat(SearchQuery.criteria("size", Long.class))
				.isEqualTo(SearchQuery.criteria("size", Long.class))
				.isNotEqualTo(SearchQuery.criteria("size", String.class))
				.hasSameHashCodeAs(SearchQuery.criteria("size", Long.class))
				.doesNotHaveSameHashCodeAs(SearchQuery.criteria("size", String.class));
	}

}
