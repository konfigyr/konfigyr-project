package com.konfigyr.support;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

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
public interface SearchQuery {

	/**
	 * Returns an {@link Optional} that may contain the search query string that is used to
	 * filter results.
	 *
	 * @return search term or an empty {@link Optional}, never {@literal null}.
	 */
	@NonNull
	Optional<String> term();

	/**
	 * Paging instructions for the {@link SearchQuery}. Can not be {@literal null} or
	 * {@link Pageable#unpaged() unpaged}.
	 *
	 * @return Paging instructions, never {@literal null}.
	 */
	@NonNull
	Pageable pageable();

}
