package com.konfigyr.registry;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Repository repositories}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public interface Artifactory {

	/**
	 * Search for {@link Repository Repositories} that match the given {@link SearchQuery}.
	 *
	 * @param query search query, can't be {@literal null}
	 * @return paged {@link Repository} search results, never {@literal null}
	 */
	@NonNull
	Page<Repository> searchRepositories(@NonNull SearchQuery query);

	/**
	 * Returns a {@link Repository} with the given {@link EntityId identifier}. If the {@link Repository}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param id repository entity identifier, can't be {@literal null}
	 * @return matching repository or empty, never {@literal null}
	 */
	@NonNull
	Optional<Repository> findRepositoryById(@NonNull EntityId id);

	/**
	 * Returns a {@link Repository} with the matching slug for a {@link com.konfigyr.namespace.Namespace}.
	 * If the {@link Repository} does not exist an empty {@link Optional} would be returned.
	 *
	 * @param namespace namespace slug, can't be {@literal null}
	 * @param slug repository slug, can't be {@literal null}
	 * @return matching repository or empty, never {@literal null}
	 */
	@NonNull
	Optional<Repository> findRepositoryBySlug(@NonNull String namespace, @NonNull String slug);

	/**
	 * Returns a {@link Repository} with the matching slug for a {@link com.konfigyr.namespace.Namespace}.
	 * If the {@link Repository} does not exist an empty {@link Optional} would be returned.
	 *
	 * @param namespace namespace entity identifier, can't be {@literal null}
	 * @param slug repository slug, can't be {@literal null}
	 * @return matching repository or empty, never {@literal null}
	 */
	@NonNull
	Optional<Repository> findRepositoryBySlug(@NonNull EntityId namespace, @NonNull String slug);

}
