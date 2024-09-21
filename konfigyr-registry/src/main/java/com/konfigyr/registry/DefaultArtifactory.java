package com.konfigyr.registry;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.Repositories.REPOSITORIES;

/**
 * Implementation of the {@link Artifactory} that uses {@link DSLContext jOOQ} to communicate with the
 * persistence layer.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@Service
@RequiredArgsConstructor
class DefaultArtifactory implements Artifactory {

	private final DSLContext context;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "repository-search")
	public Page<Repository> searchRepositories(@NonNull SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();

		query.term().ifPresent(term -> conditions.add(
				REPOSITORIES.NAME.likeIgnoreCase("%" + term + "%")
		));

		if (query instanceof ArtifactorySearchQuery asq) {
			asq.namespace().ifPresent(namespace -> conditions.add(
					NAMESPACES.SLUG.eq(namespace)
			));
		}

		final List<Repository> repositories = createRepositoryQuery(DSL.and(conditions))
				.offset(query.pageable().getOffset())
				.limit(query.pageable().getPageSize())
				.fetch(DefaultArtifactory::toRepository);

		final long count = context.fetchCount(createRepositoryQuery(DSL.and(conditions)));

		return new PageImpl<>(repositories, query.pageable(), count);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "repository-id-lookup")
	public Optional<Repository> findRepositoryById(@NonNull EntityId id) {
		return fetch(REPOSITORIES.ID.eq(id.get()));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-slug-lookup")
	public Optional<Repository> findRepositoryBySlug(@NonNull String namespace, @NonNull String slug) {
		return fetch(DSL.and(
				NAMESPACES.SLUG.eq(namespace),
				REPOSITORIES.SLUG.eq(slug)
		));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-slug-lookup")
	public Optional<Repository> findRepositoryBySlug(@NonNull EntityId namespace, @NonNull String slug) {
		return fetch(DSL.and(
				NAMESPACES.ID.eq(namespace.get()),
				REPOSITORIES.SLUG.eq(slug)
		));
	}

	private Optional<Repository> fetch(Condition predicate) {
		return createRepositoryQuery(predicate)
			.fetchOptional(DefaultArtifactory::toRepository);
	}

	private SelectConditionStep<? extends Record> createRepositoryQuery(@NonNull Condition predicate) {
		return context.select(
						REPOSITORIES.ID,
						NAMESPACES.SLUG,
						REPOSITORIES.SLUG,
						REPOSITORIES.NAME,
						REPOSITORIES.DESCRIPTION,
						REPOSITORIES.IS_PRIVATE,
						REPOSITORIES.CREATED_AT,
						REPOSITORIES.UPDATED_AT
				)
				.from(REPOSITORIES)
				.innerJoin(NAMESPACES)
				.on(REPOSITORIES.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(predicate);
	}

	@NonNull
	private static Repository toRepository(@NonNull Record record) {
		return Repository.builder()
				.id(record.get(REPOSITORIES.ID))
				.namespace(record.get(NAMESPACES.SLUG))
				.slug(record.get(REPOSITORIES.SLUG))
				.name(record.get(REPOSITORIES.NAME))
				.description(record.get(REPOSITORIES.DESCRIPTION))
				.isPrivate(record.get(REPOSITORIES.IS_PRIVATE))
				.createdAt(record.get(REPOSITORIES.CREATED_AT))
				.updatedAt(record.get(REPOSITORIES.UPDATED_AT))
				.build();
	}
}
