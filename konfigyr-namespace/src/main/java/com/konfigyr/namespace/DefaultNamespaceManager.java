package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.jooq.SettableRecord;
import com.konfigyr.support.FullName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;

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

	private final Marker CREATED = MarkerFactory.getMarker("NAMEPSPACE_CREATED");

	static final String CACHE_NAME = "namespaces";

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Cacheable(CACHE_NAME)
	@Transactional(readOnly = true, label = "namespace-id-lookup")
	public Optional<Namespace> findById(@NonNull EntityId id) {
		return fetch(NAMESPACES.ID.eq(id.get()));
	}

	@NonNull
	@Override
	@Cacheable(CACHE_NAME)
	@Transactional(readOnly = true, label = "namespace-slug-lookup")
	public Optional<Namespace> findBySlug(@NonNull String slug) {
		return fetch(NAMESPACES.SLUG.eq(slug));
	}

	@Override
	@Transactional(readOnly = true, label = "namespace-exists")
	public boolean exists(@NonNull String slug) {
		return context.fetchExists(NAMESPACES, NAMESPACES.SLUG.eq(slug));
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-create")
	public Namespace create(@NonNull NamespaceDefinition definition) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create namespace from: {}", definition);
		}

		final EntityId owner = lookupOwner(definition.owner())
				.orElseThrow(() -> new NamespaceOwnerException(definition));

		final Namespace namespace;

		try {
			namespace = context.insertInto(NAMESPACES)
					.set(
							SettableRecord.of(context, NAMESPACES)
									.set(NAMESPACES.ID, EntityId.generate().map(EntityId::get))
									.set(NAMESPACES.TYPE, definition.type().name())
									.set(NAMESPACES.SLUG, definition.slug().get())
									.set(NAMESPACES.NAME, definition.name())
									.set(NAMESPACES.DESCRIPTION, definition.description())
									.set(NAMESPACES.CREATED_AT, OffsetDateTime.now())
									.set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
									.get()
					)
					.returning(NAMESPACES.fields())
					.fetchOne(DefaultNamespaceManager::toNamespace);
		} catch (DuplicateKeyException e) {
			throw new NamespaceExistsException(definition, e);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while creating a namespace", e);
		}

		Assert.state(namespace != null, () -> "Could not create namespace from: " + definition);

		final Member admin = createMember(namespace.id(), owner, NamespaceRole.ADMIN);

		log.info(CREATED, "Successfully created new namespace {} with administrator member {} from {}",
				namespace.id(), admin.id(), definition);

		publisher.publishEvent(new NamespaceEvent.Created(namespace.id()));

		return namespace;
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-find-members")
	public Page<Member> findMembers(@NonNull EntityId id) {
		return findMembers(NAMESPACES.ID.eq(id.get()));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-find-members")
	public Page<Member> findMembers(@NonNull String slug) {
		return findMembers(NAMESPACES.SLUG.eq(slug));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-find-members")
	public Page<Member> findMembers(@NonNull Namespace namespace) {
		return findMembers(namespace.id());
	}

	@NonNull
	@Override
	public Member updateMember(@NonNull EntityId id, @NonNull NamespaceRole role) {
		context.update(NAMESPACE_MEMBERS)
				.set(NAMESPACE_MEMBERS.ROLE, role.name())
				.where(NAMESPACE_MEMBERS.ID.eq(id.get()))
				.execute();

		final Member member = createMembersQuery(NAMESPACE_MEMBERS.ID.eq(id.get()))
				.fetchOptional(DefaultNamespaceManager::toMember)
				.orElseThrow(() -> new NamespaceException("Failed to update unknown member with: " + id));

		log.info("Successfully updated member role: [id={}, namespace={}, account={}, role={}]",
				member.id(), member.namespace(), member.account(), member.role());

		return member;
	}

	@Override
	@Transactional(label = "namespace-remove-member")
	public void removeMember(@NonNull EntityId member) {
		final Record result = context.deleteFrom(NAMESPACE_MEMBERS)
				.where(NAMESPACE_MEMBERS.ID.eq(member.get()))
				.returning(NAMESPACE_MEMBERS.ACCOUNT_ID, NAMESPACE_MEMBERS.NAMESPACE_ID)
				.fetchAny();

		if (result != null) {
			log.info("Successfully removed member: [id={}, namespace={}, account={}]",
					member.get(), result.get(NAMESPACE_MEMBERS.NAMESPACE_ID), result.get(NAMESPACE_MEMBERS.ACCOUNT_ID));
		}
	}

	@NonNull
	private Optional<Namespace> fetch(@NonNull Condition condition) {
		return context
				.select(NAMESPACES.fields())
				.from(NAMESPACES)
				.where(condition)
				.fetchOptional(DefaultNamespaceManager::toNamespace);
	}

	@NonNull
	private Optional<EntityId> lookupOwner(@NonNull EntityId id) {
		return context
				.select(ACCOUNTS.ID)
				.from(ACCOUNTS)
				.where(ACCOUNTS.ID.eq(id.get()))
				.fetchOptional(ACCOUNTS.ID)
				.map(EntityId::from);
	}

	@NonNull
	private Page<Member> findMembers(@NonNull Condition condition) {
		if (log.isDebugEnabled()) {
			log.debug("Fetching namespace members for namespace for Condition: {}", condition);
		}

		final List<Member> members = createMembersQuery(condition)
				.fetch()
				.map(DefaultNamespaceManager::toMember);

		return new PageImpl<>(members);
	}

	@NonNull
	private SelectConditionStep<? extends Record> createMembersQuery(@NonNull Condition condition) {
		return context.select(
						NAMESPACE_MEMBERS.ID,
						NAMESPACE_MEMBERS.NAMESPACE_ID,
						NAMESPACE_MEMBERS.ACCOUNT_ID,
						NAMESPACE_MEMBERS.ROLE,
						NAMESPACE_MEMBERS.SINCE,
						ACCOUNTS.EMAIL,
						ACCOUNTS.FIRST_NAME,
						ACCOUNTS.LAST_NAME,
						ACCOUNTS.AVATAR
				)
				.from(NAMESPACE_MEMBERS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.innerJoin(ACCOUNTS)
				.on(ACCOUNTS.ID.eq(NAMESPACE_MEMBERS.ACCOUNT_ID))
				.where(condition);
	}

	@NonNull
	private Member createMember(@NonNull EntityId namespace, @NonNull EntityId account, @NonNull NamespaceRole role) {
		if (log.isDebugEnabled()) {
			log.debug("Creating new namespace member with: [namespace={}, account={}, role={}]", namespace, account, role);
		}

		final Long id;

		try {
			id = context.insertInto(NAMESPACE_MEMBERS)
					.set(
							SettableRecord.of(context, NAMESPACE_MEMBERS)
									.set(NAMESPACE_MEMBERS.ID, EntityId.generate().map(EntityId::get))
									.set(NAMESPACE_MEMBERS.NAMESPACE_ID, namespace.get())
									.set(NAMESPACE_MEMBERS.ACCOUNT_ID, account.get())
									.set(NAMESPACE_MEMBERS.ROLE, role.name())
									.get()
					)
					.returning(NAMESPACE_MEMBERS.ID)
					.fetchOne(NAMESPACE_MEMBERS.ID);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while creating the namespace member", e);
		}

		Assert.state(id != null, () -> String.format("Could not create member for: " +
				"[namespace=%s, account=%s, role=%s]", namespace, account, role));

		return createMembersQuery(NAMESPACE_MEMBERS.ID.eq(id))
				.fetchOptional(DefaultNamespaceManager::toMember)
				.orElseThrow(() -> new IllegalStateException("Failed to lookup member with: " + EntityId.from(id)));
	}

	@NonNull
	private static Namespace toNamespace(@NonNull Record record) {
		return Namespace.builder()
				.id(record.get(NAMESPACES.ID))
				.type(record.get(NAMESPACES.TYPE))
				.slug(record.get(NAMESPACES.SLUG))
				.name(record.get(NAMESPACES.NAME))
				.description(record.get(NAMESPACES.DESCRIPTION))
				.avatar(record.get(NAMESPACES.AVATAR))
				.createdAt(record.get(NAMESPACES.CREATED_AT))
				.updatedAt(record.get(NAMESPACES.UPDATED_AT))
				.build();
	}

	@NonNull
	private static Member toMember(@NonNull Record record) {
		final FullName displayName = FullName.of(
				record.get(ACCOUNTS.FIRST_NAME),
				record.get(ACCOUNTS.LAST_NAME)
		);

		return Member.builder()
				.id(record.get(NAMESPACE_MEMBERS.ID))
				.namespace(record.get(NAMESPACE_MEMBERS.NAMESPACE_ID))
				.account(record.get(NAMESPACE_MEMBERS.ACCOUNT_ID))
				.role(record.get(NAMESPACE_MEMBERS.ROLE))
				.email(record.get(ACCOUNTS.EMAIL))
				.displayName(displayName.get())
				.avatar(record.get(ACCOUNTS.AVATAR))
				.since(record.get(NAMESPACE_MEMBERS.SINCE))
				.build();
	}
}
