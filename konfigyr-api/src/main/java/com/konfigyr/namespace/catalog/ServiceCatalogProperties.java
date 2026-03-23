package com.konfigyr.namespace.catalog;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration properties controlling the behavior of the service catalog rebuild pipeline.
 * <p>
 * The system uses a queue-based scheduling model with debouncing and controlled concurrency.
 * These properties define how aggressively rebuilds are scheduled and how much parallelism is
 * allowed during execution.
 * <p>
 * Since rebuilds operate at the release level and modify only a subset of rows within a service
 * partition, the system must balance responsiveness with the database load. Proper tuning of these
 * values ensures efficient processing without overwhelming the database.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "konfigyr.namespace.service-catalog")
public class ServiceCatalogProperties {

	/**
	 * The debounce period applied when scheduling catalog rebuilds.
	 * <p>
	 * When a rebuild is scheduled, its execution is delayed by this duration. If additional events
	 * for the same {@code release_id} occur during this window, they update the same queue entry
	 * instead of creating new ones.
	 * <p>
	 * This reduces redundant rebuilds in scenarios where multiple related events are emitted close
	 * together, such as during artifact publication or manifest generation workflows. A shorter
	 * duration improves responsiveness, while a longer duration improves efficiency by batching
	 * more events. The optimal value depends on the expected event rate and the number of artifacts
	 * and services involved in the catalog rebuild.
	 * <p>
	 * The default debounce period is set to 10 seconds.
	 */
	@NotNull
	@DurationMin(seconds = 10)
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration buildDebouncePeriod = Duration.ofSeconds(10);

	/**
	 * The maximum duration for a catalog rebuild operation.
	 * <p>
	 * When a rebuild is scheduled, it is executed within this time limit. If the rebuild exceeds
	 * this duration, it is canceled and rescheduled. This prevents long-running rebuilds from
	 * consuming all available resources and causing service outages.
	 * <p>
	 * The default timeout is set to 1 minute, which should be sufficient time for most use cases.
	 */
	@NotNull
	@DurationMin(seconds = 10)
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration buildTimeout = Duration.ofMinutes(1);

	/**
	 * Maximum number of catalog rebuilds that can be executed concurrently.
	 * <p>
	 * Each rebuild performs a transactional delete and insert within a single service partition.
	 * While partitioning reduces contention, excessive parallelism can still put pressure on the
	 * database due to concurrent writes and index updates.
	 * <p>
	 * This setting limits the number of parallel rebuilds to maintain a predictable load and stable
	 * performance. It should be tuned based on database capacity and workload characteristics. Start
	 * conservatively and scale up under load testing. The default value is set to 10, meaning that
	 * up to 10 catalog rebuilds can be executed at the same time.
	 */
	@Range(min = 1, max = 100)
	private int parallelBuilds = 10;

}
