package com.konfigyr.batch;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@NullMarked
@RequiredArgsConstructor
final class ListableJobRegistry implements JobRegistry {

	private final ListableBeanFactory listableBeanFactory;

	@Override
	public @Nullable Job getJob(String name) {
		final Object candidate;

		try {
			candidate = listableBeanFactory.getBean(name);
		} catch (NoSuchBeanDefinitionException e) {
			return null;
		}

		if (candidate instanceof Job job) {
			return job;
		}

		return null;
	}

	@Override
	public Collection<String> getJobNames() {
		final String[] names = listableBeanFactory.getBeanNamesForType(Job.class);
		return names.length == 0 ? Collections.emptySet() : List.of(names);
	}

	@Override
	public void register(Job job) {
		throw new UnsupportedOperationException("Cannot register jobs in read-only registry");
	}

	@Override
	public void unregister(String jobName) {
		throw new UnsupportedOperationException("Cannot unregister jobs in read-only registry");
	}
}
