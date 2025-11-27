package com.konfigyr.batch;


import org.jspecify.annotations.NullMarked;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchJdbcAutoConfiguration;
import org.springframework.context.annotation.Bean;

@NullMarked
@AutoConfiguration(before = BatchJdbcAutoConfiguration.class)
public class BatchAutoConfiguration {

	@Bean
	JobRegistry listableJobRegistry(ListableBeanFactory listableBeanFactory) {
		return new ListableJobRegistry(listableBeanFactory);
	}

}
