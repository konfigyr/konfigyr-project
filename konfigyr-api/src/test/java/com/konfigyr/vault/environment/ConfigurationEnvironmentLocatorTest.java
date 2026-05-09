package com.konfigyr.vault.environment;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import com.konfigyr.vault.state.RepositoryStateException;
import com.konfigyr.vault.state.StateRepository;
import com.konfigyr.vault.state.StateRepositoryFactory;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationEnvironmentLocatorTest extends AbstractIntegrationTest {

	Service service;
	StateRepository repository;

	@Mock
	AuthenticatedPrincipal principal;

	@Autowired
	ProfileManager profiles;

	@Autowired
	Services services;

	@Autowired
	VaultAccessor accessor;

	@Autowired
	StateRepositoryFactory factory;

	@Autowired
	ConfigurationEnvironmentLocator locator;

	@Autowired
	TestObservationRegistry observationRegistry;

	@BeforeEach
	void setup() {
		service = services.get(EntityId.from(2)).orElseThrow();
		repository = factory.create(service);
	}

	@AfterEach
	void cleanup() throws Exception {
		repository.destroy();
		repository.close();
	}

	@Test
	@DisplayName("should not locate any configuration environment when no profile is specified")
	void emptyProfiles() {
		final var environment = locator.locate(principal, service);

		assertThat(environment)
				.returns("konfigyr-id", ConfigurationEnvironment::name)
				.returns(Collections.emptyList(), ConfigurationEnvironment::profiles)
				.returns(Collections.emptyList(), ConfigurationEnvironment::propertySources);
	}

	@Test
	@DisplayName("should not locate configuration environment for an unknown profile name")
	void locateForUnknownProfile() {
		final var environment = locator.locate(principal, service, "unknown-profile");

		assertThat(environment)
				.returns("konfigyr-id", ConfigurationEnvironment::name)
				.returns(List.of("unknown-profile"), ConfigurationEnvironment::profiles)
				.returns(Collections.emptyList(), ConfigurationEnvironment::propertySources);

		assertObservation()
				.hasEvent("konfigyr.vault.environment.missing", "unknown-profile");
	}

	@Test
	@DisplayName("should not locate configuration environment for an unknown source repository")
	void locateForUnknownRepository() {
		assertThatNoException().isThrownBy(repository::destroy);

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> locator.locate(principal, service, "staging"))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, RepositoryStateException::getErrorCode);

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo(ConfigurationEnvironmentObservation.OBSERVATION_NAME)
				.that()
				.hasBeenStarted()
				.hasBeenStopped()
				.hasError()
				.assertThatError()
				.isInstanceOf(RepositoryStateException.class);
	}

	@Test
	@DisplayName("should not locate configuration environment for an unknown profile in source repository")
	void locateForUnknownBranch() {
		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> locator.locate(principal, service, "production"))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode);

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo(ConfigurationEnvironmentObservation.OBSERVATION_NAME)
				.that()
				.hasBeenStarted()
				.hasBeenStopped()
				.hasError()
				.assertThatError()
				.isInstanceOf(RepositoryStateException.class);
	}

	@Test
	@DisplayName("should locate configuration environment for a single profile name")
	void locateForSingleProfile() throws Exception {
		final var profile = setupStateForProfile("development");
		final var environment = locator.locate(principal, service, "development");

		assertThat(environment)
				.returns("konfigyr-id", ConfigurationEnvironment::name)
				.returns(List.of("development"), ConfigurationEnvironment::profiles);

		assertThat(environment.propertySources())
				.hasSize(1)
				.satisfiesExactly(
						assertPropertySource(service, profile, Map.of("spring.profiles.active", "development"))
				);

		assertObservation()
				.hasEvent("konfigyr.vault.environment.located", "development");
	}

	@Test
	@DisplayName("should locate configuration environment for multiple profile names")
	void locateForDifferentProfile() throws Exception {
		final var staging = setupBranchForProfile("staging");
		final var development = setupStateForProfile("development");

		final var environment = locator.locate(principal, service, "staging", "development");

		assertThat(environment)
				.returns("konfigyr-id", ConfigurationEnvironment::name)
				.returns(List.of("staging", "development"), ConfigurationEnvironment::profiles);

		assertThat(environment.propertySources())
				.hasSize(2)
				.satisfiesExactly(
						assertPropertySource(service, staging, Map.of()),
						assertPropertySource(service, development, Map.of("spring.profiles.active", "development"))
				);

		assertObservation()
				.hasEvent("konfigyr.vault.environment.located", "development")
				.hasEvent("konfigyr.vault.environment.located", "staging");
	}

	Profile setupBranchForProfile(String profileName) {
		final var profile = profiles.get(service, profileName).orElseThrow(() -> new IllegalStateException(
				"Attempted to setup repository state for profile that does not exist: " + profileName
		));

		repository.create(profile);

		return profile;
	}

	Profile setupStateForProfile(String profileName) throws Exception {
		final var profile = setupBranchForProfile(profileName);
		final var author = (AuthenticatedPrincipal) TestPrincipals.john().getPrincipal();

		try (var vault = accessor.open(Objects.requireNonNull(author), service, profile)) {
			vault.apply(
					PropertyChanges.builder()
							.profile(profile)
							.subject("Test changes")
							.createProperty("spring.profiles.active", profileName)
							.build()
			);
		}
		return profile;
	}

	TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertObservation() {
		return assertThat(observationRegistry)
				.hasObservationWithNameEqualTo(ConfigurationEnvironmentObservation.OBSERVATION_NAME)
				.that()
				.hasBeenStarted()
				.hasBeenStopped()
				.doesNotHaveError()
				.hasContextualNameEqualTo("locating configuration environment for '%s' service".formatted(service.slug()))
				.hasHighCardinalityKeyValue("konfigyr.namespace.service", service.id().serialize());
	}

	static Consumer<PropertySource> assertPropertySource(Service service, Profile profile, Map<String, String> source) {
		return it -> {
			assertThat(it.name())
					.as("Property source name should be '%s-%s'", service.slug(), profile.slug())
					.isEqualTo("%s-%s".formatted(service.slug(), profile.slug()));

			assertThat(it.source())
					.as("Property source should contain following configuration state: %s", source)
					.isEqualTo(source);
		};
	}
}
