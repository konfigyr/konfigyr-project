package com.konfigyr.namespace.catalog;

import com.konfigyr.artifactory.*;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceCatalog;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Tokenizer;
import com.konfigyr.support.Tokens;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;

/**
 * Service that is responsible for providing a default implementation of the {@link ServiceCatalogSource}
 * interface. This implementation uses jOOQ to query the {@code service_configuration_catalog} table
 * that is partitioned by {@code service_id}. This ensures that all database operations remain localized
 * to a single partition, which keeps reads, deletes, or inserts efficient while still allowing multiple
 * releases per service to coexist in the same partition.
 * <p>
 * This service provides listeners for {@link ServiceEvent.Created} and {@link ServiceEvent.Deleted} events
 * that trigger the creation and deletion of service configiuration partitions in the database. The name
 * of the partition is derived from the original table name and service identifier:
 * <code>service_configuration_catalog_{service_id}</code>.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
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

	/**
	 * Splits a search term into words the same way the {@code service_configuration_catalog} search vector
	 * trigger normalizes {@code name} (see {@code namespaces-1.0.0.xml}), any run of non-alphanumeric
	 * characters, so a dotted/hyphenated term like {@code spring.datasource.url} is treated as three
	 * separate words.
	 */
	private static final Tokenizer TERM_TOKENIZER = Tokenizer.alphanumeric();

	static final PageableExecutor serviceCatalogExecutor = PageableExecutor.builder()
			.defaultSortField(SERVICE_CONFIGURATION_CATALOG.NAME.asc())
			.build();

	private final DSLContext context;
	private final ArtifactoryConverters converters;

	@Override
	@Transactional(readOnly = true, label = "retrieve-service-catalog")
	public ServiceCatalog get(Service service) {
		final List<ServiceCatalog.Property> properties = createServiceCatalogQuery()
				.where(SERVICE_CONFIGURATION_CATALOG.SERVICE_ID.eq(service.id().get()))
				.orderBy(SERVICE_CONFIGURATION_CATALOG.NAME)
				.fetch(this::toPropertyDescriptor);

		return new ServiceCatalog(service.id(), service, "latest", properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	@Transactional(readOnly = true, label = "search-service-catalog")
	public Page<PropertyDescriptor> query(Service service, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(SERVICE_CONFIGURATION_CATALOG.SERVICE_ID.eq(service.id().get()));

		final String rankSearchTerm = query.term(TERM_TOKENIZER)
				.map(ConfigurationCatalogService::toPrefixTsQuery)
				.filter(StringUtils::hasText)
				.orElse(null);

		return serviceCatalogExecutor.rankBy(rankSearchTerm, SERVICE_CONFIGURATION_CATALOG.SEARCH_VECTOR).execute(
				this::createServiceCatalogQuery,
				() -> DSL.and(conditions),
				this::toPropertyDescriptor,
				query.pageable()
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

	private SelectJoinStep<Record> createServiceCatalogQuery() {
		return context.select(SERVICE_CONFIGURATION_CATALOG.fields())
				.from(SERVICE_CONFIGURATION_CATALOG);
	}

	private ServiceCatalog.Property toPropertyDescriptor(Record record) {
		final JsonSchema schema = Objects.requireNonNullElseGet(
				record.get(PROPERTY_DEFINITIONS.SCHEMA, converters.schema()),
				NullSchema::instance
		);

		return ServiceCatalog.Property.builder()
				.groupId(record.get(SERVICE_CONFIGURATION_CATALOG.GROUP_ID))
				.artifactId(record.get(SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID))
				.version(record.get(SERVICE_CONFIGURATION_CATALOG.VERSION))
				.name(record.get(SERVICE_CONFIGURATION_CATALOG.NAME))
				.typeName(record.get(SERVICE_CONFIGURATION_CATALOG.TYPE_NAME))
				.schema(schema)
				.description(record.get(SERVICE_CONFIGURATION_CATALOG.DESCRIPTION))
				.defaultValue(record.get(SERVICE_CONFIGURATION_CATALOG.DEFAULT_VALUE))
				.deprecation(record.get(SERVICE_CONFIGURATION_CATALOG.DEPRECATION, converters.deprecation()))
				.build();
	}

	static Name formatPartitionTableName(EntityId id) {
		return DSL.name(SERVICE_CONFIGURATION_CATALOG.getName() + "_" + id.get());
	}

	/**
	 * Builds a {@code tsquery} expression from a search term already split into words by {@link #TERM_TOKENIZER}
	 * (the same normalization the search vector trigger applies to {@code name}), requiring every word to be
	 * present as a prefix ({@code word:*}), so a partial term like {@code spring.appl} matches a property
	 * named {@code spring.application.name}, and a full dotted name matches by requiring each of its segments
	 * as its own word.
	 *
	 * @param words the search term, already split into words, can't be {@literal null}
	 * @return the {@code tsquery} expression text, or {@literal empty} if {@code words} has no alphanumeric words
	 */
	static String toPrefixTsQuery(Tokens words) {
		return words.filter(StringUtils::hasText)
				.map(word -> word + ":*")
				.join(" & ");
	}
}
