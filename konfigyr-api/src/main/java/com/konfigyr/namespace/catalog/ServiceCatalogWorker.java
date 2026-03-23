package com.konfigyr.namespace.catalog;

import com.konfigyr.entity.EntityId;
import io.micrometer.observation.annotation.ObservationKeyValue;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.ArtifactVersionProperties.ARTIFACT_VERSION_PROPERTIES;
import static com.konfigyr.data.tables.ArtifactVersions.ARTIFACT_VERSIONS;
import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.PropertyDefinitions.PROPERTY_DEFINITIONS;
import static com.konfigyr.data.tables.ServiceArtifacts.SERVICE_ARTIFACTS;
import static com.konfigyr.data.tables.ServiceConfigurationCatalog.SERVICE_CONFIGURATION_CATALOG;
import static com.konfigyr.data.tables.ServiceReleases.SERVICE_RELEASES;

/**
 * Performs the actual rebuild of the service configuration catalog for a given release that
 * is triggered by the {@link ServiceCatalogScheduler}. The service configuration catalog re-build
 * operation is designed to be:
 * <ul>
 *     <li><b>Idempotent</b> – repeated executions produce the same result for the same database state</li>
 *     <li><b>Deterministic</b> – the catalog is a pure function of release artifacts and metadata</li>
 *     <li><b>Concurrency-safe</b> – invoked only after the release has been claimed by the scheduler</li>
 * </ul>
 * <p>
 * This component encapsulates the core projection logic of the system, transforming a service
 * release, or a {@link com.konfigyr.artifactory.Manifest} and its associated artifact metadata
 * into a materialized configuration metadata catalog stored in the database.
 * <p>
 * The rebuild process operates strictly at the <b>release level</b>. Each invocation recomputes the
 * catalog for a single {@code release_id}, ensuring that multiple versions of a service can coexist
 * independently without interfering with each other. Because of this, <i>partition-level</i> truncation
 * is not possible, and rebuilds rely on targeted {@code DELETE} operations followed by {@code INSERT}
 * statements.
 * <p>
 * The rebuild is executed as a single transactional operation. First, the existing catalog rows for
 * the associated release are removed using a targeted {@code DELETE} statement. Then the new rows are
 * inserted using a set-based SQL projection (JOIN between release artifacts and artifact property
 * definitions).
 * <p>
 * Because rebuilds operate on a subset of rows within a service partition, partition-level truncation is
 * not possible. Instead, the system relies on efficient partition pruning (via {@code service_id}) and
 * indexed deletes to keep operations performant.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ServiceCatalogScheduler
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class ServiceCatalogWorker {

	private final DSLContext context;

	/**
	 * Builds the service configuration catalog for a specific release. It performs the following steps:
	 *
	 * <ul>
	 *     <li>Resolves the {@code release_id} and associated {@code service_id}</li>
	 *     <li>Deletes existing catalog entries for the release</li>
	 *     <li>Executes a bulk insert using a SQL projection that joins artifacts with descriptors</li>
	 * </ul>
	 *
	 * <p>
	 * If some artifacts do not yet have metadata available, they are naturally excluded from the
	 * projection. This results in a partial catalog, which will be completed in later rebuilds
	 * when metadata becomes available. This behavior ensures eventual consistency without introducing
	 * failures or blocking execution.
	 * <p>
	 * Each rebuild should be executed as a single transactional operation. The catalog rows belonging
	 * to the given {@code release_id} are removed and recomputed using a set-based SQL projection.
	 * The use of partitioning ensures that all operations remain scoped to a single service
	 * partition, minimizing contention and improving performance.
	 * <p>
	 * The method must remain fast and set-based, avoiding per-row processing to scale efficiently when
	 * handling large numbers of releases.
	 *
	 * @param release the release entity identifier, can't be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "service-catalog-worker.build")
	@Observed(name = "konfigyr.namespace.service-catalog-worker")
	void build(@ObservationKeyValue(key = "release") EntityId release) {
		assertReleaseExists(release);

		final long dropped = context.deleteFrom(SERVICE_CONFIGURATION_CATALOG)
				.where(SERVICE_CONFIGURATION_CATALOG.RELEASE_ID.eq(release.get()))
				.execute();

		final long inserted = createCatalogForRelease(release);

		log.info("Service configuration catalog successfully built: [release={}, propertes_dropped={}, propertes_inserted={}]",
				release, dropped, inserted);
	}

	private long createCatalogForRelease(EntityId release) {
		return context.insertInto(
						SERVICE_CONFIGURATION_CATALOG,
						SERVICE_CONFIGURATION_CATALOG.SERVICE_ID,
						SERVICE_CONFIGURATION_CATALOG.RELEASE_ID,
						SERVICE_CONFIGURATION_CATALOG.GROUP_ID,
						SERVICE_CONFIGURATION_CATALOG.ARTIFACT_ID,
						SERVICE_CONFIGURATION_CATALOG.VERSION,
						SERVICE_CONFIGURATION_CATALOG.NAME,
						SERVICE_CONFIGURATION_CATALOG.TYPE_NAME,
						SERVICE_CONFIGURATION_CATALOG.SCHEMA,
						SERVICE_CONFIGURATION_CATALOG.DEFAULT_VALUE,
						SERVICE_CONFIGURATION_CATALOG.DESCRIPTION,
						SERVICE_CONFIGURATION_CATALOG.DEPRECATION
				)
				.select(
						DSL.select(
										SERVICE_RELEASES.SERVICE_ID,
										SERVICE_RELEASES.ID,
										SERVICE_ARTIFACTS.GROUP_ID,
										SERVICE_ARTIFACTS.ARTIFACT_ID,
										SERVICE_ARTIFACTS.VERSION,
										PROPERTY_DEFINITIONS.NAME,
										PROPERTY_DEFINITIONS.TYPE_NAME,
										PROPERTY_DEFINITIONS.SCHEMA,
										PROPERTY_DEFINITIONS.DEFAULT_VALUE,
										PROPERTY_DEFINITIONS.DESCRIPTION,
										PROPERTY_DEFINITIONS.DEPRECATION
								)
								.from(SERVICE_ARTIFACTS)
								.innerJoin(SERVICE_RELEASES)
								.on(SERVICE_RELEASES.ID.eq(SERVICE_ARTIFACTS.RELEASE_ID))
								.innerJoin(ARTIFACTS)
								.on(DSL.and(
										ARTIFACTS.GROUP_ID.eq(SERVICE_ARTIFACTS.GROUP_ID),
										ARTIFACTS.ARTIFACT_ID.eq(SERVICE_ARTIFACTS.ARTIFACT_ID)
								))
								.innerJoin(ARTIFACT_VERSIONS)
								.on(DSL.and(
										ARTIFACT_VERSIONS.ARTIFACT_ID.eq(ARTIFACTS.ID),
										ARTIFACT_VERSIONS.VERSION.eq(SERVICE_ARTIFACTS.VERSION)
								))
								.innerJoin(ARTIFACT_VERSION_PROPERTIES)
								.on(ARTIFACT_VERSION_PROPERTIES.ARTIFACT_VERSION_ID.eq(ARTIFACT_VERSIONS.ID))
								.innerJoin(PROPERTY_DEFINITIONS)
								.on(PROPERTY_DEFINITIONS.ID.eq(ARTIFACT_VERSION_PROPERTIES.PROPERTY_DEFINITION_ID))
								.where(SERVICE_RELEASES.ID.eq(release.get()))
				).execute();
	}

	private void assertReleaseExists(EntityId release) {
		final Long id = context.selectDistinct(SERVICE_RELEASES.SERVICE_ID)
				.from(SERVICE_RELEASES)
				.where(SERVICE_RELEASES.ID.eq(release.get()))
				.fetchOne(SERVICE_RELEASES.SERVICE_ID);

		if (id == null) {
			throw new IllegalStateException("Failed to resolve service release with identifier: " + release);
		}
	}

}
