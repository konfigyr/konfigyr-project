package com.konfigyr.security.access;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

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

		final Set<AccessGrant> grants = new HashSet<>();
		grants.addAll(namespaceMembershipGrants(condition));
		grants.addAll(namespaceApplicationGrants(condition));

		return Collections.unmodifiableSet(grants);
	}

	private Collection<AccessGrant> namespaceMembershipGrants(@NonNull Condition condition) {
		return context.select(NAMESPACE_MEMBERS.ACCOUNT_ID, NAMESPACE_MEMBERS.ROLE)
				.from(NAMESPACE_MEMBERS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.where(condition)
				.fetch(record -> AccessGrant.forNamespaceMember(
						record.get(NAMESPACE_MEMBERS.ACCOUNT_ID, EntityId.class),
						record.get(NAMESPACE_MEMBERS.ROLE, NamespaceRole.class)
				));
	}

	private Collection<AccessGrant> namespaceApplicationGrants(@NonNull Condition condition) {
		return context.select(OAUTH_APPLICATIONS.CLIENT_ID)
				.from(OAUTH_APPLICATIONS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(OAUTH_APPLICATIONS.NAMESPACE_ID))
				.where(condition.and(DSL.or(
						OAUTH_APPLICATIONS.EXPIRES_AT.isNull(),
						OAUTH_APPLICATIONS.EXPIRES_AT.gt(OffsetDateTime.now())
				)))
				.fetch(record -> AccessGrant.forNamespaceApplication(
						record.get(OAUTH_APPLICATIONS.CLIENT_ID)
				));
	}

}
