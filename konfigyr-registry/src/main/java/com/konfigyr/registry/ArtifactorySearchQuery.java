package com.konfigyr.registry;

import com.konfigyr.support.SearchQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Implementation of the {@link SearchQuery} that would be used by the {@link Artifactory}.
 *
 * @param term search term, can't be {@literal null}
 * @param namespace namespace slug, can't be {@literal null}
 * @param pageable paging instructions, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public record ArtifactorySearchQuery(
		@NonNull Optional<String> term,
		@NonNull Optional<String> namespace,
		@NonNull Pageable pageable
) implements SearchQuery {

	/**
	 * Create a new {@link ArtifactorySearchQuery} that only contains the {@link Pageable paging instructions}.
	 *
	 * @param pageable paging instructions, can't be {@literal null}.
	 * @return search query, never {@literal null}
	 */
	@NonNull
	public static ArtifactorySearchQuery of(@NonNull Pageable pageable) {
		return new ArtifactorySearchQuery(Optional.empty(), Optional.empty(), pageable);
	}

	/**
	 * Create a new {@link ArtifactorySearchQuery} with search term, namespace filter criteria and
	 * {@link Pageable paging instructions}.
	 *
	 * @param term search term, can be {@literal null}
	 * @param namespace namespace slug, can be {@literal null}
	 * @param pageable paging instructions, can't be {@literal null}.
	 * @return search query, never {@literal null}
	 */
	public static ArtifactorySearchQuery of(@Nullable String term, @Nullable String namespace, @NonNull Pageable pageable) {
		return new ArtifactorySearchQuery(
				Optional.ofNullable(term).filter(StringUtils::hasText),
				Optional.ofNullable(namespace).filter(StringUtils::hasText),
				pageable
		);
	}

}
