package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.ArtifactoryConverters;
import com.konfigyr.artifactory.PropertyDescriptor;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.support.SearchQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class ConfigurationCatalogService implements ServiceCatalogSource {

	private static final Marker PARTITION_MARKER = MarkerFactory.getMarker("SERVICE_CONFIGURATION_CATALOG_PARTITION");

	/**
	 * SQL command to be executed when Namespace service is created. This would create a new partition on
	 * the `service_configuration_catalog` table using the Service identifier. The final SQL should look
	 * something like this:
	 * <pre>
	 * CREATE TABLE service_configuration_catalog_42
	 * PARTITION OF service_configuration_catalog
	 * FOR VALUES IN (42);
	 * </pre>
	 */
	private static final String CREATE_PARTITION_COMMAND = "CREATE TABLE {0} PARTITION OF {1} FOR VALUES IN ({2});";

	static final PageableExecutor serviceCatalogExecutor = PageableExecutor.builder()
			.defaultSortField(SERVICE_CONFIGURATION_CATALOG.NAME.asc())
			.build();

	private final DSLContext context;
	private final ArtifactoryConverters converters;

	@Override
	@Transactional(readOnly = true, label = "retrieve-service-catalog")
	public ServiceCatalog get(Service service) {
		final List<ServiceCatalog.Property> properties =
				createServiceCatalogQuery(SERVICE_CONFIGURATION_CATALOG.SERVICE_ID.eq(service.id().get()))
				.orderBy(SERVICE_CONFIGURATION_CATALOG.NAME)
				.fetch(this::toPropertyDescriptor);

		return new ServiceCatalog(service.id(), service, "latest", properties);
	}

	@Override
	@Transactional(readOnly = true, label = "search-service-catalog")
	public Page<PropertyDescriptor> query(Service service, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(SERVICE_CONFIGURATION_CATALOG.SERVICE_ID.eq(service.id().get()));

		query.term().ifPresent(term -> conditions.add(DSL.or(
				SERVICE_CONFIGURATION_CATALOG.NAME.likeIgnoreCase(term),
				DSL.condition("search_vector @@ plainto_tsquery('simple', ?)", term)
		)));

		return serviceCatalogExecutor.execute(
				createServiceCatalogQuery(DSL.and(conditions)),
				this::toPropertyDescriptor,
				query.pageable(),
				() -> context.fetchCount(createServiceCatalogQuery(DSL.and(conditions)))
		);
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(id = "namespace.catalog.create-partition", classes = ServiceEvent.Created.class)
	void createConfigurationCatalogPartition(ServiceEvent.Created event) {
		log.debug("Attempting to create configuration catalog partition for service: {}", event.id());

		context.execute(
				CREATE_PARTITION_COMMAND,
				formatPartitionTableName(event.id()),
				DSL.name(SERVICE_CONFIGURATION_CATALOG.getName()), event.id().get()
		);

		log.info(PARTITION_MARKER, "Successfully created configuration catalog partition for service: {}", event.id());
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(id = "namespace.catalog.drop-partition", classes = ServiceEvent.Deleted.class)
	void dropConfigurationCatalogPartition(ServiceEvent.Deleted event) {
		log.debug("Attempting to drop configuration catalog partition for service: {}", event.id());

		context.dropTable(formatPartitionTableName(event.id())).execute();

		log.info(PARTITION_MARKER, "Successfully dropped configuration catalog partition for service: {}", event.id());
	}

	private SelectConditionStep<Record> createServiceCatalogQuery(Condition condition) {
		return context.select(SERVICE_CONFIGURATION_CATALOG.fields())
				.from(SERVICE_CONFIGURATION_CATALOG)
				.where(condition);
	}

	private ServiceCatalog.Property toPropertyDescriptor(Record record) {
		return ServiceCatalog.Property.builder()
				.groupId(record.get(SERVICE_CONFIGURATION_CATALOG.GROUP_ID))
				.artifactId(record.get(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID))
				.version(record.get(SERVICE_CONFIGURATION_CATALOG.VERSION))
				.name(record.get(SERVICE_CONFIGURATION_CATALOG.NAME))
				.typeName(record.get(SERVICE_CONFIGURATION_CATALOG.TYPE_NAME))
				.schema(record.get(SERVICE_CONFIGURATION_CATALOG.SCHEMA, converters.schema()))
				.description(record.get(SERVICE_CONFIGURATION_CATALOG.DESCRIPTION))
				.defaultValue(record.get(SERVICE_CONFIGURATION_CATALOG.DEFAULT_VALUE))
				.deprecation(record.get(SERVICE_CONFIGURATION_CATALOG.DEPRECATION, converters.deprecation()))
				.build();
	}

	static Name formatPartitionTableName(EntityId id) {
		return DSL.name(SERVICE_CONFIGURATION_CATALOG.getName() + "_" + id.get());
	}
}
