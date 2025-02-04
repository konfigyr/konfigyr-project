package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Slf4j
@RequiredArgsConstructor
class AccessControlRepository {

	private final DSLContext context;

	@Transactional(readOnly = true, label = "access-control-repository.get")
	AccessControl get(@NonNull ObjectIdentity identity) throws ObjectIdentityNotFound {
		if (log.isDebugEnabled()) {
			log.debug("Looking up access control grants for: {}", identity);
		}

		final Collection<AccessGrant> grants = switch (identity.type()) {
			case ObjectIdentity.NAMESPACE_TYPE -> namespace(identity);
			default -> Collections.emptyList();
		};

		if (CollectionUtils.isEmpty(grants)) {
			throw new ObjectIdentityNotFound(identity);
		}

		return new KonfigyrAccessControl(identity, grants);
	}

	private Collection<AccessGrant> namespace(@NonNull ObjectIdentity identity) {
		final Condition condition = switch (identity.id()) {
			case String slug -> NAMESPACES.SLUG.eq(slug);
			case EntityId id -> NAMESPACES.ID.eq(id.get());
			case Long id -> NAMESPACES.ID.eq(id);
			default -> throw new IllegalArgumentException("Invalid namespace object identifier: " + identity);
		};

		return context.select(NAMESPACE_MEMBERS.ACCOUNT_ID, NAMESPACE_MEMBERS.ROLE)
				.from(NAMESPACE_MEMBERS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.where(condition)
				.fetch(AccessControlRepository::toNamespaceAccessGrant);
	}

	static AccessGrant toNamespaceAccessGrant(@NonNull Record record) {
		return AccessGrant.of(
				record.get(NAMESPACE_MEMBERS.ACCOUNT_ID, EntityId.class),
				record.get(NAMESPACE_MEMBERS.ROLE, NamespaceRole.class)
		);
	}

}
