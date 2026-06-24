package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

/**
 * Resolves a namespace into a lightweight {@link Owner} reference used by ownership workflows.
 *
 * @author Mila Zarkovic
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
public class OwnerResolver {

	private final DSLContext context;

	/**
	 * Resolves the owner for the given namespace slug.
	 * <p>
	 * This is a lookup helper used by the controller layer to translate a namespace path segment into
	 * an {@link Owner} reference for verification operations.
	 *
	 * @param namespace the namespace slug to resolve
	 * @return the matching owner if one exists; otherwise an empty optional
	 */
	@Transactional(readOnly = true, label = "owner-resolver.find-owner-by-slug")
	public Optional<Owner> findOwner(String namespace) {
		log.debug("Resolving owner for namespace slug '{}'", namespace);
		return fetch(NAMESPACES.SLUG.eq(namespace));
	}

	/**
	 * Resolves the owner for the given namespace identifier.
	 *
	 * @param id the namespace entity identifier to resolve
	 * @return the matching owner if one exists; otherwise an empty optional
	 */
	@Transactional(readOnly = true, label = "owner-resolver.find-owner-by-id")
	public Optional<Owner> findOwner(EntityId id) {
		log.debug("Resolving owner for namespace id '{}'", id);
		return fetch(NAMESPACES.ID.eq(id.get()));
	}

	private Optional<Owner> fetch(Condition condition) {
		return context.select(NAMESPACES.ID, NAMESPACES.SLUG)
				.from(NAMESPACES)
				.where(condition)
				.fetchOptional(record -> Owner.of(
						EntityId.from(record.get(NAMESPACES.ID)),
						record.get(NAMESPACES.SLUG)
				));
	}
}
