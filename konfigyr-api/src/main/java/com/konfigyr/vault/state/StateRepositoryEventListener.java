package com.konfigyr.vault.state;

import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.vault.ProfileEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
public class StateRepositoryEventListener {

	static Marker INITIALIZED = MarkerFactory.getMarker("STATE_REPOSITORY_INITIALIZED");
	static Marker DESTROYED = MarkerFactory.getMarker("STATE_REPOSITORY_DESTROYED");
	static Marker PROFILE_CREATED = MarkerFactory.getMarker("STATE_REPOSITORY_PROFILE_CREATED");
	static Marker PROFILE_DELETED = MarkerFactory.getMarker("STATE_REPOSITORY_PROFILE_DELETED");

	private final Services services;
	private final Path path;

	@Async
	@Retryable
	@TransactionalEventListener(id = "source-control.repository-provisioner", classes = ServiceEvent.Created.class)
	void provisionServiceSourceControlRepository(ServiceEvent.Created event) throws Exception {
		try (StateRepository repository = GitStateRepository.initialize(event.get(), path)) {
			log.info(INITIALIZED, "Successfully created Git repository for {}", repository.owner());
		}
	}

	@Async
	@Retryable(noRetryFor = ServiceNotFoundException.class)
	@TransactionalEventListener(id = "source-control.profile-provisioner", classes = ProfileEvent.Created.class)
	void provisionServiceProfile(ProfileEvent.Created event) throws Exception {
		final Service service = services.get(event.get().service()).orElseThrow(() -> new ServiceNotFoundException(
				"Failed to find Service that owns the Profile with identifier: " + event.get().id()
		));

		try (StateRepository repository = GitStateRepository.load(service, path)) {
			final String branch = repository.create(event.get());

			log.info(PROFILE_CREATED, "Successfully created Git profile branch with name '{}' for: {}",
					branch, event.get());
		}
	}

	@Async
	@Retryable
	@TransactionalEventListener(id = "source-control.repository-deprovisioner", classes = ServiceEvent.Deleted.class)
	void deprovisionServiceSourceControlRepository(ServiceEvent.Deleted event) throws Exception {
		try (StateRepository repository = GitStateRepository.load(event.get(), path)) {
			repository.destroy();

			log.info(DESTROYED, "Successfully destroyed Git repository for: {}", repository.owner());
		}
	}

	@Async
	@Retryable(noRetryFor = ServiceNotFoundException.class)
	@TransactionalEventListener(id = "source-control.profile-deprovisioner", classes = ProfileEvent.Deleted.class)
	void deprovisionServiceProfile(ProfileEvent.Deleted event) throws Exception {
		final Service service = services.get(event.get().service()).orElseThrow(() -> new ServiceNotFoundException(
				"Failed to find Service that owns the Profile with identifier: " + event.get().id()
		));

		try (StateRepository repository = GitStateRepository.load(service, path)) {
			repository.delete(event.get());

			log.info(PROFILE_DELETED, "Successfully deleted Git profile branch for: {}", event.get());
		}
	}

}
