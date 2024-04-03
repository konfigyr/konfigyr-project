package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/**
 * Implementation of the {@link NamespaceManager} that uses {@link DSLContext jOOQ} to communicate with the
 * persistence layer.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Slf4j
@RequiredArgsConstructor
class DefaultNamespaceManager implements NamespaceManager {

	static final String CACHE_NAME = "namespaces";

	private final DSLContext context;

	@NonNull
	@Override
	@Cacheable(CACHE_NAME)
	@Transactional(readOnly = true)
	public Optional<Namespace> findById(@NonNull EntityId id) {
		return fetch(NAMESPACES.ID.eq(id.get()));
	}

	@NonNull
	@Override
	@Cacheable(CACHE_NAME)
	@Transactional(readOnly = true)
	public Optional<Namespace> findBySlug(@NonNull String slug) {
		return fetch(NAMESPACES.SLUG.eq(slug));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean exists(@NonNull String slug) {
		return context.fetchExists(NAMESPACES, NAMESPACES.SLUG.eq(slug));
	}

	private Optional<Namespace> fetch(@NonNull Condition condition) {
		return context
				.select(NAMESPACES.fields())
				.from(NAMESPACES)
				.where(condition)
				.fetchOptional(DefaultNamespaceManager::map);
	}

	@NonNull
	private static Namespace map(@NonNull Record record) {
		return Namespace.builder()
				.id(record.get(NAMESPACES.ID))
				.type(record.get(NAMESPACES.TYPE))
				.slug(record.get(NAMESPACES.SLUG))
				.name(record.get(NAMESPACES.NAME))
				.description(record.get(NAMESPACES.DESCRIPTION))
				.createdAt(record.get(NAMESPACES.CREATED_AT))
				.updatedAt(record.get(NAMESPACES.UPDATED_AT))
				.build();
	}
}
