package com.konfigyr.namespace;

import com.konfigyr.artifactory.*;
import com.konfigyr.data.Keys;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.catalog.ServiceCatalogSource;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.Services.SERVICES;
import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;

@Slf4j
@RequiredArgsConstructor
public class DefaultServices implements Services {

	private static final Name SERVICE_ARTIFACTS_ALIAS = DSL.name("artifacts");

	private final Marker CREATED = MarkerFactory.getMarker("SERVICE_CREATED");
	private final Marker PUBLISHED = MarkerFactory.getMarker("MANIFEST_PUBLISHED");

	static final PageableExecutor servicesExecutor = PageableExecutor.builder()
			.defaultSortField(SERVICES.NAME.desc())
			.sortField("name", SERVICES.NAME)
			.sortField("date", SERVICES.UPDATED_AT)
			.build();

	private final DSLContext context;
	private final NamespaceManager namespaces;
	private final ServiceCatalogSource catalogSource;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "service-search")
	public Page<@NonNull Service> find(@NonNull Namespace namespace, @NonNull SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(SERVICES.NAMESPACE_ID.eq(namespace.id().get()));

		query.term().map(term -> "%" + term + "%").ifPresent(term -> conditions.add(DSL.or(
				SERVICES.NAME.likeIgnoreCase(term),
				SERVICES.DESCRIPTION.likeIgnoreCase(term)
		)));

		if (log.isDebugEnabled()) {
			log.debug("Fetching services for conditions: {}", conditions);
		}

		return servicesExecutor.execute(
				createServicesQuery(DSL.and(conditions)),
				DefaultServices::toService,
				query.pageable(),
				() -> context.fetchCount(createServicesQuery(DSL.and(conditions)))
		);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "service-id-lookup")
	public Optional<Service> get(@NonNull EntityId id) {
		return fetch(SERVICES.ID.eq(id.get()));
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "service-slug-lookup")
	public Optional<Service> get(@NonNull Namespace namespace, @NonNull String slug) {
		return fetch(DSL.and(
				SERVICES.NAMESPACE_ID.eq(namespace.id().get()),
				SERVICES.SLUG.eq(slug)
		));
	}

	@Override
	@Transactional(readOnly = true, label = "service-exists")
	public boolean exists(@NonNull Namespace namespace, @NonNull String slug) {
		return context.fetchExists(SERVICES, DSL.and(
				SERVICES.NAMESPACE_ID.eq(namespace.id().get()),
				SERVICES.SLUG.eq(slug)
		));
	}

	@NonNull
	@Override
	@Transactional(label = "service-create")
	public Service create(@NonNull ServiceDefinition definition) {
		if (log.isDebugEnabled()) {
			log.debug("Attempting to create service from: {}", definition);
		}

		if (!namespaces.exists(definition.namespace())) {
			throw new NamespaceNotFoundException(definition.namespace());
		}

		final Service service;

		try {
			service = context.insertInto(SERVICES)
					.set(
							SettableRecord.of(context, SERVICES)
									.set(SERVICES.ID, EntityId.generate().map(EntityId::get))
									.set(SERVICES.NAMESPACE_ID, definition.namespace().get())
									.set(SERVICES.SLUG, definition.slug().get())
									.set(SERVICES.NAME, definition.name())
									.set(SERVICES.DESCRIPTION, definition.description())
									.set(SERVICES.CREATED_AT, OffsetDateTime.now())
									.set(SERVICES.UPDATED_AT, OffsetDateTime.now())
									.get()
					)
					.returning(SERVICES.fields())
					.fetchOne(DefaultServices::toService);
		} catch (DuplicateKeyException e) {
			throw new ServiceExistsException(definition, e);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while creating a service", e);
		}

		Assert.state(service != null, () -> "Could not create service from: " + definition);

		log.info(CREATED, "Successfully created new service {} in namespace {} from {}",
				service.id(), service.namespace(), definition);

		publisher.publishEvent(new ServiceEvent.Created(service));

		return service;
	}

	@NonNull
	@Override
	@Transactional(label = "service-update")
	public Service update(@NonNull EntityId id, @NonNull ServiceDefinition definition) {
		final Record record = context.select(SERVICES.SLUG, SERVICES.NAME, SERVICES.DESCRIPTION)
				.from(SERVICES)
				.where(SERVICES.ID.eq(id.get()))
				.fetchOptional()
				.orElseThrow(() -> new ServiceNotFoundException(id))
				.with(SERVICES.NAME, definition.name())
				.with(SERVICES.DESCRIPTION, definition.description());

		final Slug slug = Slug.slugify(record.get(SERVICES.SLUG));

		try {
			context.update(SERVICES)
					.set(record)
					.set(SERVICES.SLUG, definition.slug().get())
					.set(SERVICES.UPDATED_AT, OffsetDateTime.now())
					.where(SERVICES.ID.eq(id.get()))
					.execute();
		} catch (DuplicateKeyException e) {
			throw new ServiceExistsException(definition, e);
		} catch (Exception e) {
			throw new NamespaceException("Unexpected exception occurred while updating service", e);
		}

		final Service service = fetch(SERVICES.ID.eq(id.get()))
				.orElseThrow(() -> new ServiceNotFoundException(id));

		if (!slug.equals(definition.slug())) {
			publisher.publishEvent(new ServiceEvent.Renamed(service, slug, definition.slug()));
		}

		return service;
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "retrieve-service-manifest")
	public Manifest manifest(@NonNull Service service) {
		return createManifestQuery(SERVICE_RELEASES.SERVICE_ID.eq(service.id().get()))
			.fetchOptional(DefaultServices::toManifest)
			.orElseGet(() -> Manifest.builder()
					.id(service.id().serialize())
					.name(service.name())
					.build()
			);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "retrieve-service-catalog")
	public ServiceCatalog catalog(@NonNull EntityId id) {
		final Service service = get(id).orElseThrow(() -> new ServiceNotFoundException(id));
		return catalogSource.get(service);
	}

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "retrieve-service-catalog")
	public ServiceCatalog catalog(@NonNull Namespace namespace, @NonNull String slug) {
		final Service service = get(namespace, slug).orElseThrow(
				() -> new ServiceNotFoundException(namespace.slug(), slug)
		);
		return catalogSource.get(service);
	}

	@NonNull
	@Override
	public Page<PropertyDescriptor> search(@NonNull Service service, @NonNull SearchQuery query) {
		return catalogSource.query(service, query);
	}

	@NonNull
	@Override
	@Transactional(label = "service-release")
	public Manifest publish(@NonNull Service service, @NonNull Collection<? extends ArtifactCoordinates> artifacts) {
		final Long releaseId = context.insertInto(SERVICE_RELEASES)
				.set(
						SettableRecord.of(context, SERVICE_RELEASES)
								.set(SERVICE_RELEASES.ID, EntityId.generate().map(EntityId::get))
								.set(SERVICE_RELEASES.SERVICE_ID, service.id().get())
								.set(SERVICE_RELEASES.VERSION, "latest")
								.set(SERVICE_RELEASES.STATE, "PENDING")
								.set(SERVICE_RELEASES.CREATED_AT, OffsetDateTime.now())
								.get()
				)
				.onConflictOnConstraint(Keys.UNIQUE_NAMESPACE_SERVICE_VERSION)
				.doUpdate()
				.set(SERVICE_RELEASES.STATE, "PENDING")
				.set(SERVICE_RELEASES.CREATED_AT, OffsetDateTime.now())
				.returning(SERVICE_RELEASES.ID)
				.fetchOne(SERVICE_RELEASES.ID);

		Assert.state(releaseId != null, "Failed to resolve the release identifier for: " + service);

		// clear the previous artifact manifest state...
		context.deleteFrom(SERVICE_ARTIFACTS)
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(releaseId))
				.execute();

		// insert the defined artifact coordinates now that we have a clear state...
		final long count = context.insertInto(SERVICE_ARTIFACTS)
				.set(artifacts.stream().map(coordinate -> SettableRecord.of(context, SERVICE_ARTIFACTS)
						.set(SERVICE_ARTIFACTS.RELEASE_ID, releaseId)
						.set(SERVICE_ARTIFACTS.GROUP_ID, coordinate.groupId())
						.set(SERVICE_ARTIFACTS.ARTIFACT_ID, coordinate.artifactId())
						.set(SERVICE_ARTIFACTS.VERSION, coordinate.version().get())
						.get()
				).toList())
				.execute();

		Assert.state(count == artifacts.size(), "Failed to insert all artifacts for: " + service);

		final Manifest manifest = manifest(service);

		log.info(PUBLISHED, "Successfully published manifest for service {} in namespace {}: {}",
				service.id(), service.namespace(), manifest);

		publisher.publishEvent(new ServiceEvent.Published(service, manifest));

		return manifest;
	}

	@Override
	@Transactional(label = "service-delete")
	public void delete(@NonNull EntityId id) {
		final Service service = get(id).orElseThrow(() -> new ServiceNotFoundException(id));

		final long count = context.delete(SERVICES)
				.where(SERVICES.ID.eq(id.get()))
				.execute();

		Assert.state(count != 0, "Failed to delete Service with identifier: " + id);

		publisher.publishEvent(new ServiceEvent.Deleted(service));
	}

	@Override
	@Transactional(label = "service-delete")
	public void delete(@NonNull Namespace namespace, @NonNull String slug) {
		context.select(SERVICES.ID)
				.from(SERVICES)
				.where(DSL.and(
						SERVICES.NAMESPACE_ID.eq(namespace.id().get()),
						SERVICES.SLUG.eq(slug)
				))
				.fetchOptional(record -> EntityId.from(record.get(SERVICES.ID)))
				.ifPresentOrElse(this::delete, () -> {
					throw new ServiceNotFoundException(namespace.slug(), slug);
				});
	}

	@NonNull
	private SelectConditionStep<Record> createServicesQuery(@NonNull Condition condition) {
		return context
				.select(SERVICES.fields())
				.from(SERVICES)
				.where(condition);
	}

	@NonNull
	private SelectConditionStep<? extends Record> createManifestQuery(@NonNull Condition condition) {
		return context.select(SERVICE_RELEASES.ID, SERVICES.NAME, SERVICE_RELEASES.CREATED_AT, createServiceArtifactMultiselectField())
				.from(SERVICE_RELEASES)
				.innerJoin(SERVICES)
				.on(SERVICES.ID.eq(SERVICE_RELEASES.SERVICE_ID))
				.where(condition);
	}

	@NonNull
	private Optional<Service> fetch(@NonNull Condition condition) {
		return createServicesQuery(condition).fetchOptional(DefaultServices::toService);
	}

	private Field<List<Artifact>> createServiceArtifactMultiselectField() {
		return DSL.multiset(
				DSL.select(
						SERVICE_ARTIFACTS.GROUP_ID,
						SERVICE_ARTIFACTS.ARTIFACT_ID,
						SERVICE_ARTIFACTS.VERSION,
						ARTIFACTS.NAME,
						ARTIFACTS.DESCRIPTION,
						ARTIFACTS.WEBSITE,
						ARTIFACTS.REPOSITORY
				)
				.from(SERVICE_ARTIFACTS)
				.leftJoin(ARTIFACTS)
				.on(DSL.and(
						ARTIFACTS.GROUP_ID.eq(SERVICE_ARTIFACTS.GROUP_ID),
						ARTIFACTS.ARTIFACT_ID.eq(SERVICE_ARTIFACTS.ARTIFACT_ID)
				))
				.where(SERVICE_ARTIFACTS.RELEASE_ID.eq(SERVICE_RELEASES.ID))
		).as(SERVICE_ARTIFACTS_ALIAS).convertFrom(results -> results.map(DefaultServices::toArtifact));
	}

	@NonNull
	private static Service toService(@NonNull Record record) {
		return Service.builder()
				.id(record.get(SERVICES.ID))
				.namespace(record.get(SERVICES.NAMESPACE_ID))
				.slug(record.get(SERVICES.SLUG))
				.name(record.get(SERVICES.NAME))
				.description(record.get(SERVICES.DESCRIPTION))
				.createdAt(record.get(SERVICES.CREATED_AT))
				.updatedAt(record.get(SERVICES.UPDATED_AT))
				.build();
	}

	@NonNull
	@SuppressWarnings("unchecked")
	private static Manifest toManifest(@NonNull Record record) {
		return Manifest.builder()
				.id(record.get(SERVICE_RELEASES.ID, EntityId.class).serialize())
				.name(record.get(SERVICES.NAME))
				.artifacts((Iterable<? extends Artifact>) record.get(SERVICE_ARTIFACTS_ALIAS))
				.createdAt(record.get(SERVICE_RELEASES.CREATED_AT, Instant.class))
				.build();
	}

	@NonNull
	private static Artifact toArtifact(@NonNull Record record) {
		return Artifact.builder()
				.groupId(record.get(SERVICE_ARTIFACTS.GROUP_ID))
				.artifactId(record.get(SERVICE_ARTIFACTS.ARTIFACT_ID))
				.version(record.get(SERVICE_ARTIFACTS.VERSION))
				.name(record.get(ARTIFACTS.NAME))
				.description(record.get(ARTIFACTS.DESCRIPTION))
				.website(record.get(ARTIFACTS.WEBSITE))
				.repository(record.get(ARTIFACTS.REPOSITORY))
				.build();
	}

}
