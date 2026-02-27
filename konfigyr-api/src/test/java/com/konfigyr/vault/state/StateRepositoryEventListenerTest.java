package com.konfigyr.vault.state;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.ServiceEvent;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileEvent;
import com.konfigyr.vault.ProfilePolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateRepositoryEventListenerTest {

	final Service service = Service.builder()
			.id(9361L)
			.namespace(1L)
			.slug("test-service")
			.name("Test service")
			.build();

	final Profile profile = Profile.builder()
			.id(EntityId.from(10))
			.service(service.id())
			.name("test-profile")
			.policy(ProfilePolicy.PROTECTED)
			.build();

	Path root;

	@Mock
	Services services;

	StateRepository repository;

	StateRepositoryEventListener listener;

	@BeforeEach
	void setup(@TempDir(cleanup = CleanupMode.ALWAYS) Path directory) {
		root = directory;
		listener = new StateRepositoryEventListener(services, directory);
	}

	@AfterEach
	void cleanup() throws Exception {
		if (repository != null) {
			repository.close();
		}
	}

	@Test
	@DisplayName("should initialize the source control repository when service created event is published")
	void provisionRepository() {
		final var event = mock(ServiceEvent.Created.class);
		doReturn(service).when(event).get();

		assertThatNoException()
				.as("Should initialize the source control repository")
				.isThrownBy(() -> listener.provisionServiceSourceControlRepository(event));

		assertThatNoException()
				.as("Should load the created Git repository after initialization")
				.isThrownBy(() -> GitStateRepository.load(service, root).close());
	}

	@Test
	@DisplayName("should create profile branch when profile created event is published")
	void provisionProfile() {
		repository = GitStateRepository.initialize(service, root);

		doReturn(Optional.of(service)).when(services).get(service.id());

		final var event = mock(ProfileEvent.Created.class);
		doReturn(profile).when(event).get();

		assertThatNoException()
				.as("Should create profile branch")
				.isThrownBy(() -> listener.provisionServiceProfile(event));

		assertThatNoException()
				.as("Should read the state of the profile branch")
				.isThrownBy(() -> repository.get(profile));

		verify(services).get(profile.service());
	}

	@Test
	@DisplayName("should fail to create profile branch when service is not found")
	void provisionProfileForUnknownService() {
		final var event = mock(ProfileEvent.Created.class);
		doReturn(profile).when(event).get();

		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> listener.provisionServiceProfile(event))
				.withMessageContaining("Failed to find Service that owns the Profile with identifier: %s", profile.id());

		verify(services).get(profile.service());
	}

	@Test
	@DisplayName("should fail to create profile branch when repository is not found")
	void provisionProfileForUnknownRepository() {
		doReturn(Optional.of(service)).when(services).get(service.id());

		final var event = mock(ProfileEvent.Created.class);
		doReturn(profile).when(event).get();

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> listener.provisionServiceProfile(event))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, RepositoryStateException::getErrorCode);

		verify(services).get(profile.service());
	}

	@Test
	@DisplayName("should delete the source control repository when service deleted event is published")
	void deprovisionRepository() {
		repository = GitStateRepository.initialize(service, root);

		final var event = mock(ServiceEvent.Deleted.class);
		doReturn(service).when(event).get();

		assertThatNoException().isThrownBy(() -> listener.deprovisionServiceSourceControlRepository(event));

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> GitStateRepository.load(service, root).close())
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, RepositoryStateException::getErrorCode);
	}

	@Test
	@DisplayName("should delete profile branch when profile deleted event is published")
	void deprovisionProfile() {
		repository = GitStateRepository.initialize(service, root);
		repository.create(profile);

		doReturn(Optional.of(service)).when(services).get(service.id());

		final var event = mock(ProfileEvent.Deleted.class);
		doReturn(profile).when(event).get();

		assertThatNoException().isThrownBy(() -> listener.deprovisionServiceProfile(event));

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> repository.get(profile))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode);

		verify(services).get(profile.service());
	}

	@Test
	@DisplayName("should fail to delete profile branch when service is not found")
	void deprovisionProfileForUnknownService() {
		final var event = mock(ProfileEvent.Deleted.class);
		doReturn(profile).when(event).get();

		assertThatExceptionOfType(ServiceNotFoundException.class)
				.isThrownBy(() -> listener.deprovisionServiceProfile(event))
				.withMessageContaining("Failed to find Service that owns the Profile with identifier: %s", profile.id());

		verify(services).get(profile.service());
	}

	@Test
	@DisplayName("should fail to delete profile branch when repository is not found")
	void deprovisionProfileForUnknownRepository() {
		doReturn(Optional.of(service)).when(services).get(service.id());

		final var event = mock(ProfileEvent.Deleted.class);
		doReturn(profile).when(event).get();

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> listener.deprovisionServiceProfile(event))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, RepositoryStateException::getErrorCode);

		verify(services).get(profile.service());
	}

	@Test
	@DisplayName("should fail to delete profile branch when repository branch is not found")
	void deprovisionProfileForUnknownBranch() {
		repository = GitStateRepository.initialize(service, root);

		doReturn(Optional.of(service)).when(services).get(service.id());

		final var event = mock(ProfileEvent.Deleted.class);
		doReturn(profile).when(event).get();

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> listener.deprovisionServiceProfile(event))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode);

		verify(services).get(profile.service());
	}

}
