package com.konfigyr.batch;


import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import org.jspecify.annotations.NullMarked;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchJdbcAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Objects;

@NullMarked
@AutoConfiguration(before = BatchJdbcAutoConfiguration.class)
public class BatchAutoConfiguration {

	@Bean
	JobRegistry listableJobRegistry(ListableBeanFactory listableBeanFactory) {
		return new ListableJobRegistry(listableBeanFactory);
	}

	@Bean
	ObservationFilter batchItemProcessStatusFilter() {
		return context -> {
			if (Objects.equals(BatchMetrics.METRICS_PREFIX + "item.process", context.getName())) {
				// Ensure the status key exists to maintain a consistent key set. Metrics backends
				// like Prometheus or Stackdriver reject this because they require every metric to
				// have the exact same tag keys to allow for proper aggregation.
				return context.addLowCardinalityKeyValue(KeyValue.of(
						BatchMetrics.METRICS_PREFIX + "item.process.status", "STARTED"
				));
			}
			return context;
		};
	}

}
