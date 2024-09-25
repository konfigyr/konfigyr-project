package com.konfigyr.support;

import com.konfigyr.entity.EntityId;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Optional;

/**
 * Interface that defines a basic query API that is used to search and retrieve matching entities.
 * <p>
 * Search queries may contain a search term that specifies the expression that would be evaluated
 * and executed to retrieve matching results.
 * <p>
 * The search query must contain a {@link Pageable} in order to paginate and sort matching results.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public sealed interface SearchQuery permits CriteriaSearchQuery {

	/**
	 * The {@link Criteria} descriptor for search terms.
	 */
	@NonNull
	Criteria<String> TERM = criteria("q", String.class);

	/**
	 * The {@link Criteria} descriptor for the {@link com.konfigyr.account.Account} search by
	 * its {@link EntityId entity identifier}.
	 */
	@NonNull
	Criteria<EntityId> ACCOUNT = criteria("account", EntityId.class);

	/**
	 * The {@link Criteria} descriptor for the {@link com.konfigyr.namespace.Namespace} slug search.
	 */
	@NonNull
	Criteria<String> NAMESPACE = criteria("namespace", String.class);

	/**
	 * Create a new search query {@link Criteria} by specifying their name and value type.
	 *
	 * @param name criteria name
	 * @param type criteria value type
	 * @param <T> generic search criteria value type.
	 * @return the search criteria, never {@literal null}
	 * @throws IllegalArgumentException when name or type are invalid.
	 */
	static <T> Criteria<T> criteria(String name, Class<T> type) {
		Assert.hasText(name, "Search criteria name must not be empty");
		Assert.notNull(type, "Search criteria type must not be null");

		return new CriteriaSearchQuery.SearchQueryCriteria<>(name, type);
	}

	/**
	 * Create a new {@link SearchQuery.Builder} to create new {@link SearchQuery search queries}.
	 *
	 * @return search query builder, never {@literal null}
	 */
	@NonNull
	static Builder builder() {
		return new CriteriaSearchQuery.SearchQueryBuilder();
	}

	/**
	 * Create a new {@link SearchQuery} that only contains the {@link Pageable paging instructions}.
	 *
	 * @param pageable paging instructions, can't be {@literal null}.
	 * @return search query, never {@literal null}
	 */
	static SearchQuery of(@NonNull Pageable pageable) {
		return builder().pageable(pageable).build();
	}

	/**
	 * Returns an {@link Optional} that may contain the search query string that is used to
	 * filter results.
	 *
	 * @return search term or an empty {@link Optional}, never {@literal null}.
	 */
	@NonNull
	default Optional<String> term() {
		return criteria(TERM);
	}

	/**
	 * Returns an {@link Optional} that may contain the {@link Criteria search query critera}
	 * value that is used to filter results.
	 *
	 * @param criteria the criteria for which the value is resolved, can't be {@literal null}.
	 * @param <T> generic type of the criteria value
	 * @return criteria value or an empty {@link Optional}, never {@literal null}.
	 */
	@NonNull
	<T> Optional<T> criteria(@NonNull Criteria<T> criteria);

	/**
	 * Paging instructions for the {@link SearchQuery}. Can not be {@literal null} or
	 * {@link Pageable#unpaged() unpaged}.
	 *
	 * @return Paging instructions, never {@literal null}.
	 */
	@NonNull
	Pageable pageable();

	/**
	 * Returns the {@link SearchQuery} with the {@link Pageable} requesting
	 * next {@link org.springframework.data.domain.Page}.
	 *
	 * @return search query for the next page, never {@literal null}.
	 */
	@NonNull
	SearchQuery next();

	/**
	 * Fluent builder interface used to create new instances of the {@link SearchQuery}.
	 *
	 * @author Vladimir Spasic
	 * @since 1.0.0
	 */
	sealed interface Builder permits CriteriaSearchQuery.SearchQueryBuilder {

		/**
		 * Specify the search term {@link Criteria}.
		 *
		 * @param term search term
		 * @return the search query builder, never {@link null}
		 */
		@NonNull
		default Builder term(String term) {
			return criteria(TERM, term);
		}

		/**
		 * Specify the value for the search {@link Criteria}.
		 *
		 * @param criteria search criteria type descriptor
		 * @param value search criteria value
		 * @param <T> generic search criteria value type
		 * @return the search query builder, never {@link null}
		 */
		@NonNull
		<T> Builder criteria(Criteria<T> criteria, T value);

		/**
		 * Specify the {@link Pageable paging instructions} for the {@link SearchQuery}.
		 *
		 * @param pageable paging instructions
		 * @return the search query builder, never {@link null}
		 */
		@NonNull
		Builder pageable(Pageable pageable);

		/**
		 * Creates the {@link SearchQuery} based in the {@link Criteria} that is added to the builder.
		 * @return the search query, never {@link null}
		 */
		@NonNull
		SearchQuery build();

	}

	/**
	 * Interface that defines the search query criteria that can be used to filter results.
	 * <p>
	 * Criteria are identified by their name and the type of the value that can be stored in the search query.
	 *
	 * @param <T> generic search criteria value type.
	 * @author Vladimir Spasic
	 * @since 1.0.0
	 */
	sealed interface Criteria<T> extends Serializable permits CriteriaSearchQuery.SearchQueryCriteria {

		/**
		 * Name of the search query {@link Criteria}.
		 *
		 * @return criteria name, never {@literal null}
		 */
		@NonNull
		String name();

		/**
		 * Underlying type of the search query {@link Criteria} value.
		 *
		 * @return criteria value type, never {@literal null}
		 */
		@NonNull
		Class<T> type();

	}

}
