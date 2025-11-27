package com.konfigyr.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListableJobRegistryTest {

	@Mock
	Job job;

	@Mock
	ListableBeanFactory listableBeanFactory;

	JobRegistry registry;

	@BeforeEach
	void setup() {
		registry = new ListableJobRegistry(listableBeanFactory);
	}

	@Test
	@DisplayName("should list empty job names when no Job bean is registered")
	void listEmptyRegisteredJobs() {
		doReturn(new String[0]).when(listableBeanFactory).getBeanNamesForType(Job.class);

		assertThat(registry.getJobNames())
				.isEmpty();

		verify(listableBeanFactory).getBeanNamesForType(Job.class);
	}

	@Test
	@DisplayName("should list job names registered as Spring beans")
	void listRegisteredJobs() {
		doReturn(new String[] { "job-1", "job-2", "job-3" }).when(listableBeanFactory).getBeanNamesForType(Job.class);

		assertThat(registry.getJobNames())
				.containsExactly("job-1", "job-2", "job-3");

		verify(listableBeanFactory).getBeanNamesForType(Job.class);
	}

	@Test
	@DisplayName("should retrieve job by name from listable bean factory")
	void findRegisteredJobByName() {
		doReturn(job).when(listableBeanFactory).getBean("job-name");

		assertThat(registry.getJob("job-name"))
				.isSameAs(job);

		verify(listableBeanFactory).getBean("job-name");
	}

	@Test
	@DisplayName("should retrieve unknown job by name from listable bean factory")
	void findUnknownJobByName() {
		doThrow(NoSuchBeanDefinitionException.class).when(listableBeanFactory).getBean("job-name");

		assertThat(registry.getJob("job-name"))
				.isNull();

		verify(listableBeanFactory).getBean("job-name");
	}

	@Test
	@DisplayName("should return null when listable bean factory returns a non-job spring bean")
	void findInvalidJobByName() {
		doReturn(new Object()).when(listableBeanFactory).getBean("job-name");

		assertThat(registry.getJob("job-name"))
				.isNull();

		verify(listableBeanFactory).getBean("job-name");
	}

	@Test
	@DisplayName("should not be able to register jobs")
	void registerJobs() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> registry.register(job))
				.withMessageContaining("Cannot register jobs in read-only registry");
	}

	@Test
	@DisplayName("should not be able to unregister jobs")
	void unregisterJobs() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> registry.unregister("job name"))
				.withMessageContaining("Cannot unregister jobs in read-only registry");
	}
}
