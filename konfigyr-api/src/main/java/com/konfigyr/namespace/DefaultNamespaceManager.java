package com.konfigyr.namespace;

import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Accounts.ACCOUNTS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

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

	private final Marker CREATED = MarkerFactory.getMarker("NAMESPACE_CREATED");

	static final String CACHE_NAME = "namespaces";

	static final PageableExecutor namespaceExecutor = PageableExecutor.builder()
			.defaultSortField(NAMESPACES.UPDATED_AT.desc())
			.sortField("name", NAMESPACES.NAME)
			.sortField("date", NAMESPACES.UPDATED_AT)
			.build();

	static final PageableExecutor applicationsExecutor = PageableExecutor.builder()
			.defaultSortField(OAUTH_APPLICATIONS.UPDATED_AT.desc())
			.sortField("name", OAUTH_APPLICATIONS.NAME)
			.sortField("date", OAUTH_APPLICATIONS.UPDATED_AT)
			.build();

	private final DSLContext context;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "namespace-search")
	public Page<@NonNull Namespace> search(@NonNull SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();

		query.term().map(term -> "%" + term + "%").ifPresent(term -> conditions.add(DSL.or(
				NAMESPACES.NAME.likeIgnoreCase(term),
				NAMESPACES.DESCRIPTION.likeIgnoreCase(term)
		)));

		query.criteria(SearchQuery.NAMESPACE).ifPresent(namespace -> conditions.add(
				NAMESPACES.SLUG.eq(namespace)
		));

		query.criteria(SearchQuery.ACCOUNT).ifPresent(account -> conditions.add(
				DSL.exists(
						DSL.select(NAMESPACE_MEMBERS.ID)
								.from(NAMESPACE_MEMBERS)
								.where(DSL.and(
										NAMESPACE_MEMBERS.ACCOUNT_ID.eq(account.get()),
										NAMESPACE_MEMBERS.NAMESPACE_ID.eq(NAMESPACES.ID)
								))
				)
		));

		if (log.isDebugEnabled()) {
			log.debug("Fetching namespace for conditions: {}", conditions);
		}

		return namespaceExecutor.execute(
				createNamespaceQuery(DSL.and(conditions)),
				DefaultNamespaceManager::toNamespace,
				query.pageable(),
				() -> context.fetchCount(createNamespaceQuery(DSL.and(conditions)))
		);
	}

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
	@Transactional(readOnly = true, label = "namespace-id-exists")
	public boolean exists(@NonNull EntityId id) {
		return context.fetchExists(NAMESPACES, NAMESPACES.ID.eq(id.get()));
	}

	@Override
	@Transactional(readOnly = true, label = "namespace-slug-exists")
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

		final EntityId adminId = createNamespaceAdministratorMember(namespace.id(), owner);

		log.info(CREATED, "Successfully created new namespace {} with administrator member {} from {}",
				namespace.id(), adminId, definition);

		publisher.publishEvent(new NamespaceEvent.Created(namespace));

		return namespace;
	}

	@NonNull
	@Override
	@CacheEvict(CACHE_NAME)
	@Transactional(label = "namespace-update")
	public Namespace update(@NonNull String slug, @NonNull NamespaceDefinition definition) {
		final Record record = context.select(NAMESPACES.SLUG, NAMESPACES.NAME, NAMESPACES.DESCRIPTION)
				.from(NAMESPACES)
				.where(NAMESPACES.SLUG.eq(slug))
				.fetchOptional()
				.orElseThrow(() -> new NamespaceNotFoundException(slug))
				.with(NAMESPACES.SLUG, definition.slug().get())
				.with(NAMESPACES.NAME, definition.name())
				.with(NAMESPACES.DESCRIPTION, definition.description());

		final EntityId id;

		try {
			id = context.update(NAMESPACES)
					.set(record)
					.set(NAMESPACES.UPDATED_AT, OffsetDateTime.now())
					.where(NAMESPACES.SLUG.eq(slug))
					.returning(NAMESPACES.ID)
					.fetchOptional(NAMESPACES.ID, EntityId.class)
					.orElseThrow(() -> new NamespaceNotFoundException(slug));
		} catch (DuplicateKeyException e) {
			throw new NamespaceExistsException(definition, e);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while updating namespace", e);
		}
		final Namespace namespace = fetch(NAMESPACES.ID.eq(id.get()))
				.orElseThrow(() -> new NamespaceNotFoundException(slug));

		if (!record.original(NAMESPACES.SLUG).equals(definition.slug().get())) {
			publisher.publishEvent(new NamespaceEvent.Renamed(namespace, Slug.slugify(slug), definition.slug()));
		}

		return namespace;
	}

	@Override
	@CacheEvict(CACHE_NAME)
	@Transactional(label = "namespace-delete")
	public void delete(@NonNull String slug) {
		final Namespace namespace = fetch(NAMESPACES.SLUG.eq(slug))
				.orElseThrow(() -> new NamespaceNotFoundException(slug));

		context.delete(NAMESPACES)
				.where(NAMESPACES.ID.eq(namespace.id().get()))
				.execute();

		publisher.publishEvent(new NamespaceEvent.Deleted(namespace));
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-find-applications", readOnly = true)
	public Page<@NonNull NamespaceApplication> findApplications(@NonNull SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();

		query.criteria(SearchQuery.NAMESPACE).ifPresent(slug -> conditions.add(NAMESPACES.SLUG.eq(slug)));

		query.criteria(NamespaceApplication.ID_CRITERIA).ifPresent(id -> conditions.add(
				OAUTH_APPLICATIONS.ID.eq(id.get())
		));

		query.criteria(NamespaceApplication.ACTIVE_CRITERIA).ifPresent(active -> {
			if (active) {
				conditions.add(DSL.or(
						OAUTH_APPLICATIONS.EXPIRES_AT.isNull(),
						OAUTH_APPLICATIONS.EXPIRES_AT.gt(OffsetDateTime.now())
				));
			} else {
				conditions.add(OAUTH_APPLICATIONS.EXPIRES_AT.lt(OffsetDateTime.now()));
			}
		});

		query.term().map(term -> "%" + term + "%").ifPresent(term -> conditions.add(DSL.or(
				OAUTH_APPLICATIONS.NAME.likeIgnoreCase(term),
				OAUTH_APPLICATIONS.CLIENT_ID.likeIgnoreCase(term)
		)));

		if (log.isDebugEnabled()) {
			log.debug("Fetching OAuth Applications for namespace for conditions: {}", conditions);
		}

		return applicationsExecutor.execute(
				createApplicationsQuery(DSL.and(conditions)),
				DefaultNamespaceManager::toApplication,
				query.pageable(),
				() -> context.fetchCount(createApplicationsQuery(DSL.and(conditions)))
		);

	}

	@NonNull
	@Override
	@Transactional(label = "namespace-find-by-client-id", readOnly = true)
	public Optional<Namespace> findNamespaceByClientId(@NonNull String clientId) {
		return context.select(NAMESPACES.fields())
				.from(NAMESPACES)
				.innerJoin(OAUTH_APPLICATIONS)
				.on(OAUTH_APPLICATIONS.NAMESPACE_ID.eq(NAMESPACES.ID))
				.where(OAUTH_APPLICATIONS.CLIENT_ID.eq(clientId))
				.fetchOptional(DefaultNamespaceManager::toNamespace);
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-get-application", readOnly = true)
	public Optional<NamespaceApplication> getApplication(@NonNull EntityId application) {
		return createApplicationsQuery(OAUTH_APPLICATIONS.ID.eq(application.get()))
				.fetchOptional(DefaultNamespaceManager::toApplication);
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-create-application")
	public NamespaceApplication createApplication(
			@NonNull Namespace namespace,
			@NonNull NamespaceApplicationDefinition definition
	) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create namespace OAuth application from: {}", definition);
		}

		final String clientId = NamespaceApplicationDefinition.generateClientId(definition);
		final String clientSecret = NamespaceApplicationDefinition.generateClientSecret(clientId);

		final Record record = SettableRecord.of(context, OAUTH_APPLICATIONS)
				.set(OAUTH_APPLICATIONS.ID, EntityId.generate().map(EntityId::get))
				.set(OAUTH_APPLICATIONS.NAMESPACE_ID, definition.namespace().get())
				.set(OAUTH_APPLICATIONS.NAME, definition.name())
				.set(OAUTH_APPLICATIONS.CLIENT_ID, clientId)
				.set(OAUTH_APPLICATIONS.CLIENT_SECRET, passwordEncoder.encode(clientSecret))
				.set(OAUTH_APPLICATIONS.SCOPES, definition.scopes().toString())
				.set(OAUTH_APPLICATIONS.EXPIRES_AT, definition.expiration())
				.set(OAUTH_APPLICATIONS.CREATED_AT, OffsetDateTime.now())
				.set(OAUTH_APPLICATIONS.UPDATED_AT, OffsetDateTime.now())
				.get();

		final NamespaceApplication application;

		try {
			application = context.insertInto(OAUTH_APPLICATIONS)
					.set(record)
					.returning(OAUTH_APPLICATIONS.fields())
					.fetchOne(it -> toApplication(it, clientSecret));
		} catch (DataIntegrityViolationException ex) {
			throw new NamespaceNotFoundException(definition.namespace());
		}

		if (application == null) {
			throw new IllegalStateException("Failed to create application");
		}

		publisher.publishEvent(new NamespaceEvent.ApplicationCreated(namespace, application));

		return application;
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-update-application")
	public NamespaceApplication updateApplication(
			@NonNull Namespace namespace,
			@NonNull EntityId id,
			@NonNull NamespaceApplicationDefinition definition
	) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to update namespace OAuth application with: [id={}, definition={}]", id, definition);
		}

		final NamespaceApplication application = context.update(OAUTH_APPLICATIONS)
				.set(OAUTH_APPLICATIONS.NAME, definition.name())
				.set(OAUTH_APPLICATIONS.SCOPES, definition.scopes().toString())
				.set(OAUTH_APPLICATIONS.EXPIRES_AT, definition.expiration())
				.set(OAUTH_APPLICATIONS.UPDATED_AT, OffsetDateTime.now())
				.where(OAUTH_APPLICATIONS.ID.eq(id.get()))
				.returning(OAUTH_APPLICATIONS.fields())
				.fetchOne(DefaultNamespaceManager::toApplication);

		if (application == null) {
			throw new NamespaceApplicationNotFoundException(id);
		}

		log.info("Successfully updated namespace OAuth application: [id={}, namespace={}, name={}, scopes={}, expiry={}]",
				application.id(), application.namespace(), application.name(), application.scopes(), application.expiresAt());

		publisher.publishEvent(new NamespaceEvent.ApplicationUpdated(namespace, application));

		return application;
	}

	@NonNull
	@Override
	@Transactional(label = "namespace-reset-application")
	public NamespaceApplication resetApplication(@NonNull Namespace namespace, @NonNull EntityId id) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to reset namespace OAuth application with identifier: {}", id);
		}

		final Record record = lookupApplication(namespace.id(), id)
				.orElseThrow(() -> new NamespaceApplicationNotFoundException(id));

		final String clientId = record.get(OAUTH_APPLICATIONS.CLIENT_ID);
		final String clientSecret = NamespaceApplicationDefinition.generateClientSecret(clientId);

		// create a new record so we can reuse it for the update query and mapper
		final var updates = record.into(OAUTH_APPLICATIONS);
		updates.set(OAUTH_APPLICATIONS.CLIENT_SECRET, passwordEncoder.encode(clientSecret));
		updates.set(OAUTH_APPLICATIONS.UPDATED_AT, OffsetDateTime.now());

		context.update(OAUTH_APPLICATIONS)
				.set(updates)
				.where(OAUTH_APPLICATIONS.ID.eq(id.get()))
				.execute();

		final NamespaceApplication application = toApplication(updates, clientSecret);

		log.info("Successfully reset namespace OAuth application: [id={}, namespace={}, name={}, scopes={}, expiry={}]",
				application.id(), application.namespace(), application.name(), application.scopes(), application.expiresAt());

		publisher.publishEvent(new NamespaceEvent.ApplicationReset(namespace, application));

		return application;
	}

	@Override
	@Transactional(label = "namespace-remove-application")
	public void removeApplication(@NonNull Namespace namespace, @NonNull EntityId id) {
		final NamespaceApplication application = lookupApplication(namespace.id(), id)
				.map(DefaultNamespaceManager::toApplication)
				.orElseThrow(() -> new NamespaceApplicationNotFoundException(id));

		context.deleteFrom(OAUTH_APPLICATIONS)
				.where(OAUTH_APPLICATIONS.ID.eq(application.id().get()))
				.execute();

		publisher.publishEvent(new NamespaceEvent.ApplicationRemoved(namespace, application));
	}

	@NonNull
	private SelectConditionStep<Record> createNamespaceQuery(@NonNull Condition condition) {
		return context
				.select(NAMESPACES.fields())
				.from(NAMESPACES)
				.where(condition);
	}

	@NonNull
	private Optional<Namespace> fetch(@NonNull Condition condition) {
		return createNamespaceQuery(condition).fetchOptional(DefaultNamespaceManager::toNamespace);
	}

	@NonNull
	private Optional<EntityId> lookupOwner(@NonNull EntityId id) {
		return context.select(ACCOUNTS.ID)
				.from(ACCOUNTS)
				.where(ACCOUNTS.ID.eq(id.get()))
				.fetchOptional(ACCOUNTS.ID)
				.map(EntityId::from);
	}

	@NonNull
	private EntityId createNamespaceAdministratorMember(@NonNull EntityId namespace, @NonNull EntityId account) {
		if (log.isDebugEnabled()) {
			log.debug("Creating namespace owner member with: [namespace={}, account={}]", namespace, account);
		}

		final Long id;

		try {
			id = context.insertInto(NAMESPACE_MEMBERS)
					.set(
							SettableRecord.of(context, NAMESPACE_MEMBERS)
									.set(NAMESPACE_MEMBERS.ID, EntityId.generate().map(EntityId::get))
									.set(NAMESPACE_MEMBERS.NAMESPACE_ID, namespace.get())
									.set(NAMESPACE_MEMBERS.ACCOUNT_ID, account.get())
									.set(NAMESPACE_MEMBERS.ROLE, NamespaceRole.ADMIN.name())
									.get()
					)
					.returning(NAMESPACE_MEMBERS.ID)
					.fetchOne(NAMESPACE_MEMBERS.ID);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while creating the namespace owner", e);
		}

		Assert.state(id != null, () -> String.format("Could not create namespace owner member for: " +
				"[namespace=%s, account=%s]", namespace, account));

		return EntityId.from(id);
	}

	@NonNull
	private SelectConditionStep<? extends Record> createApplicationsQuery(@NonNull Condition condition) {
		return context.select(
						OAUTH_APPLICATIONS.ID,
						OAUTH_APPLICATIONS.NAMESPACE_ID,
						OAUTH_APPLICATIONS.NAME,
						OAUTH_APPLICATIONS.CLIENT_ID,
						OAUTH_APPLICATIONS.SCOPES,
						OAUTH_APPLICATIONS.EXPIRES_AT,
						OAUTH_APPLICATIONS.CREATED_AT,
						OAUTH_APPLICATIONS.UPDATED_AT
				)
				.from(OAUTH_APPLICATIONS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(OAUTH_APPLICATIONS.NAMESPACE_ID))
				.where(condition);
	}

	@NonNull
	private Optional<? extends Record> lookupApplication(@NonNull EntityId namespace, @NonNull EntityId application) {
		return createApplicationsQuery(DSL.and(
				OAUTH_APPLICATIONS.ID.eq(application.get()),
				OAUTH_APPLICATIONS.NAMESPACE_ID.eq(namespace.get())
		)).fetchOptional();
	}

	@NonNull
	private static Namespace toNamespace(@NonNull Record record) {
		return Namespace.builder()
				.id(record.get(NAMESPACES.ID))
				.slug(record.get(NAMESPACES.SLUG))
				.name(record.get(NAMESPACES.NAME))
				.description(record.get(NAMESPACES.DESCRIPTION))
				.avatar(record.get(NAMESPACES.AVATAR))
				.createdAt(record.get(NAMESPACES.CREATED_AT))
				.updatedAt(record.get(NAMESPACES.UPDATED_AT))
				.build();
	}

	@NonNull
	private static NamespaceApplication toApplication(@NonNull Record record) {
		return toApplication(record, null);
	}

	@NonNull
	private static NamespaceApplication toApplication(@NonNull Record record, @Nullable String secret) {
		return NamespaceApplication.builder()
				.id(record.get(OAUTH_APPLICATIONS.ID))
				.namespace(record.get(OAUTH_APPLICATIONS.NAMESPACE_ID))
				.name(record.get(OAUTH_APPLICATIONS.NAME))
				.clientId(record.get(OAUTH_APPLICATIONS.CLIENT_ID))
				.clientSecret(secret)
				.scopes(record.get(OAUTH_APPLICATIONS.SCOPES))
				.expiresAt(record.get(OAUTH_APPLICATIONS.EXPIRES_AT))
				.createdAt(record.get(OAUTH_APPLICATIONS.CREATED_AT))
				.updatedAt(record.get(OAUTH_APPLICATIONS.UPDATED_AT))
				.build();
	}
}
