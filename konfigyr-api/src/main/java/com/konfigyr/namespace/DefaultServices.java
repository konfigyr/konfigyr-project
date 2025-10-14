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
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Services.SERVICES;

@Slf4j
@RequiredArgsConstructor
public class DefaultServices implements Services {

	private final Marker CREATED = MarkerFactory.getMarker("SERVICE_CREATED");

	static final PageableExecutor servicesExecutor = PageableExecutor.builder()
			.defaultSortField(SERVICES.NAME.desc())
			.sortField("name", SERVICES.NAME)
			.sortField("date", SERVICES.UPDATED_AT)
			.build();

	private final DSLContext context;
	private final NamespaceManager namespaces;
	private final ApplicationEventPublisher publisher;

	@NonNull
	@Override
	@Transactional(readOnly = true, label = "service-search")
	public Page<Service> find(@NonNull Namespace namespace, @NonNull SearchQuery query) {
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

		publisher.publishEvent(new ServiceEvent.Created(service.id()));

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
			throw new NamespaceException("Unexpected exception occurred while updating namespace", e);
		}

		if (!slug.equals(definition.slug())) {
			publisher.publishEvent(new ServiceEvent.Renamed(id, slug, definition.slug()));
		}

		return fetch(SERVICES.ID.eq(id.get())).orElseThrow(() -> new ServiceNotFoundException(id));
	}

	@Override
	@Transactional(label = "service-delete")
	public void delete(@NonNull EntityId id) {
		final long count = context.delete(SERVICES)
				.where(SERVICES.ID.eq(id.get()))
				.execute();

		if (count == 0) {
			throw new ServiceNotFoundException(id);
		}

		publisher.publishEvent(new ServiceEvent.Deleted(id));
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
	private Optional<Service> fetch(@NonNull Condition condition) {
		return createServicesQuery(condition).fetchOptional(DefaultServices::toService);
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

}
