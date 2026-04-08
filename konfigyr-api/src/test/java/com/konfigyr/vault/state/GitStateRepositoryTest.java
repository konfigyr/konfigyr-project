package com.konfigyr.vault.state;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.support.Slug;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfilePolicy;
import com.konfigyr.vault.Properties;
import com.konfigyr.vault.PropertyChanges;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitStateRepositoryTest {

	Path root;
	GitStateRepository repository;

	final Service service = Service.builder()
			.id(123567L)
			.namespace(1L)
			.slug("test-service")
			.name("test service")
			.build();

	@BeforeEach
	void setup(@TempDir(cleanup = CleanupMode.ALWAYS) Path parent) {
		root = parent;
		repository = GitStateRepository.initialize(service, parent);
	}

	@AfterEach
	void cleanup() {
		repository.close();
	}

	@Test
	@DisplayName("should initialize bare Git repository for service")
	void assertRepositoryInitialized() {
		assertThat(root.resolve("service-repository-%s".formatted(service.id().serialize())))
				.exists()
				.isDirectory()
				.isNotEmptyDirectory();
	}

	@Test
	@DisplayName("should load existing Git repository for a service")
	void loadExistingRepository() throws IOException {
		assertThatNoException().isThrownBy(() -> GitStateRepository.load(service, root).close());

		assertThat(Files.list(root))
				.hasSize(1)
				.contains(root.resolve("service-repository-%s".formatted(service.id().serialize())));
	}

	@Test
	@DisplayName("should fail to create a Git repository for a service that already has one")
	void ensureUniqueServiceRepository() {
		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to initialize existing repository")
				.isThrownBy(() -> GitStateRepository.initialize(service, root).close())
				.returns(RepositoryStateException.ErrorCode.REPOSITORY_ALREADY_EXISTS, RepositoryStateException::getErrorCode)
				.withMessageContaining("Repository already exists for Service(%s, %s)", service.id(), service.slug());
	}

	@Test
	@DisplayName("should create a new Git branch for a `test` profile in the service Git repository")
	void createTestProfile() {
		final var profile = createProfile(123456L, "test", ProfilePolicy.UNPROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		assertThat(repository.get(profile))
				.as("Profile should not have any configuration state yet")
				.isNotNull()
				.satisfies(it -> assertThat(it.author())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.revision())
						.isNotBlank()
				)
				.satisfies(it -> assertThat(it.getInputStream())
						.isEmpty()
				)
				.satisfies(it -> assertThat(it.summary())
						.isEqualTo("Repository initialized for Service(%s, %s)", service.id(), service.slug())
				)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to create profile with same name twice")
				.isThrownBy(() -> repository.create(profile))
				.returns(RepositoryStateException.ErrorCode.PROFILE_ALREADY_EXISTS, RepositoryStateException::getErrorCode)
				.withMessageContaining("Profile '%s' already exists", "test");

		assertThatNoException()
				.as("Should delete profile without exceptions")
				.isThrownBy(() -> repository.delete(profile));

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to retrieve state from deleted profile")
				.isThrownBy(() -> repository.get(profile))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to retrieve state from profile '%s' as it does not exist", "test");
	}

	@Test
	@DisplayName("should create a changeset branch for the `test` profile and assert changeset branch lifecycle")
	void assertChangesetBranchLifecycle() throws Exception {
		final var profile = createProfile(89L, "test", ProfilePolicy.PROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		final var changeset = changesetFor("First changes", null, "server.address=localhost\nserver.port=8080\n");
		final var result = repository.update(profile, changeset);

		assertThat(result.isApplied())
				.as("Changeset should be successfully applied")
				.isTrue();

		assertThat(result.branch())
				.as("Changeset branch should be successfully created")
				.isNotBlank()
				.matches("refs/heads/changeset/test/[a-f0-9\\\\-]*");

		assertThat(result.revision())
				.as("The commit identifier should be set")
				.isNotBlank()
				.matches("[a-f0-9]{40}");

		assertThat(repository.get(profile, result.branch()))
				.as("Should retrieve changeset state with the changes that were committed")
				.returns(result.revision(), RepositoryState::revision)
				.returns("Test Author <author@test.com>", RepositoryState::author)
				.returns("First changes", RepositoryState::summary)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.getInputStream())
						.hasContent("server.address=localhost\nserver.port=8080\n")
				);

		assertThat(repository.merge(profile, result.branch()))
				.as("Should apply changeset changes to the target profile without exceptions")
				.returns(true, MergeOutcome::isApplied)
				.returns(false, MergeOutcome::isConflicting);

		assertThat(repository.get(profile))
				.as("Profile branch should now have the changes that were squashed and committed")
				.returns(result.revision(), RepositoryState::revision)
				.returns("Test Author <author@test.com>", RepositoryState::author)
				.returns("First changes", RepositoryState::summary)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.satisfies(it -> assertThat(it.getInputStream())
						.hasContent("server.address=localhost\nserver.port=8080\n")
				);

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("The applied changeset branch should be discarded and removed from the Git repository")
				.isThrownBy(() -> repository.get(profile, result.branch()))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to retrieve state from profile '%s' and changeset '%s' as it does not exist",
						profile.slug(), result.branch());

		assertThat(repository.history(profile, Pageable.ofSize(5)))
				.as("Git repository history should two expected commits in the correct order")
				.hasSize(2)
				.extracting(RepositoryVersion::summary)
				.containsExactly("First changes", "Repository initialized for Service(EntityId(123567, 0000000003RNF), test-service)");
	}

	@Test
	@DisplayName("should fail to merge changeset due to conflicts on the target profile branch")
	void shouldFailToMergeDueToConflicts() throws Exception {
		final var profile = createProfile(120L, "test", ProfilePolicy.PROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		final var first = changesetFor("First change", null, "server.port=8080\n");
		final var second = changesetFor("Second change", "Should initiate a merge conflict", "server.port=8888\n");

		final var firstResult = repository.update(profile, first);
		final var secondResult = repository.update(profile, second);

		assertThat(repository.merge(profile, firstResult.branch()))
				.as("Should apply changes from the first changeset")
				.returns(true, MergeOutcome::isApplied);

		assertThat(repository.merge(profile, secondResult.branch()))
				.returns(false, MergeOutcome::isApplied)
				.returns(true, MergeOutcome::isConflicting)
				.extracting(MergeOutcome::conflicts, InstanceOfAssertFactories.STRING)
				.isEqualTo("<<<<<<< %s\nserver.port=8080\n=======\nserver.port=8888\n>>>>>>> %s\n",
						secondResult.branch(), profile.slug());
	}

	@Test
	@DisplayName("should discard changeset by removing the branch from the service Git repository")
	void discardChangeset() throws Exception {
		final var profile = createProfile(120L, "test", ProfilePolicy.PROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		final var changeset = changesetFor("Subject", null, "server.address=localhost\n");
		final var result = repository.update(profile, changeset);

		assertThat(result.isApplied())
				.as("Changeset should be successfully applied")
				.isTrue();

		assertThatNoException()
				.as("Should successfully discard changeset without exceptions")
				.isThrownBy(() -> repository.discard(profile, result.branch()));

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("The changeset branch should be discarded and removed from the Git repository")
				.isThrownBy(() -> repository.get(profile, result.branch()))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to retrieve state from profile '%s' and changeset '%s' as it does not exist",
						profile.slug(), result.branch());
	}

	@Test
	@DisplayName("should fail to discard changeset that has no branch in the service Git repository")
	void discardUnknownChangeset() {
		final var profile = createProfile(1L, "unknown-profile", ProfilePolicy.IMMUTABLE);

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to discard unknown changeset branch")
				.isThrownBy(() -> repository.discard(profile, "unknown"))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to discard changes from changeset 'unknown'");
	}

	@Test
	@DisplayName("should fail to create a changeset branch for an unknown profile")
	void createChangesetForUnknownProfile() throws Exception {
		final var profile = createProfile(1L, "unknown", ProfilePolicy.IMMUTABLE);
		final var changeset = changesetFor("Failing", null, "server.address=localhost\n");

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should not be able to create changeset for an profile that is not yet created")
				.isThrownBy(() -> repository.update(profile, changeset))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode)
				.withMessageContaining("Could not create changeset branch for an unknown profile with name '%s'",
						profile.slug());
	}

	@Test
	@DisplayName("should fail to apply changes from an unknown changeset branch to a profile")
	void applyUnknownChangesetToProfile() {
		final var profile = createProfile(1L, "test", ProfilePolicy.UNPROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should not apply any changes from an unknown changeset branch")
				.isThrownBy(() -> repository.merge(profile, "unknown"))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to apply changes to profile '%s' as changeset '%s' does not exist", "test", "unknown");
	}

	@Test
	@DisplayName("should fail to apply changes from a changeset branch to an unknown profile")
	void applyChangesetToUnknownProfile() {
		final var profile = createProfile(1L, "unknown profile", ProfilePolicy.IMMUTABLE);

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should not apply changes from a changeset to an unknown profile")
				.isThrownBy(() -> repository.merge(profile, "unknown"))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode)
				.withMessageContaining("Could not apply changeset '%s' to an unknown profile with name '%s'",
						"unknown", profile.slug());
	}

	@Test
	@DisplayName("should fail to create a profile branch that already exists")
	void assertUniqueProfileBranches() {
		final var profile = createProfile(1L, "test", ProfilePolicy.PROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should not apply changes from a changeset to an unknown profile")
				.isThrownBy(() -> repository.create(profile))
				.returns(RepositoryStateException.ErrorCode.PROFILE_ALREADY_EXISTS, RepositoryStateException::getErrorCode)
				.withMessageContaining("Profile '%s' already exists", profile.slug());
	}

	@Test
	@DisplayName("should successfully remove the `test` profile from the service Git repository")
	void shouldDeleteProfile() {
		final var profile = createProfile(1L, "test", ProfilePolicy.UNPROTECTED);

		assertThatNoException()
				.as("Should create a new branch for profile without exceptions")
				.isThrownBy(() -> repository.create(profile));

		assertThatNoException()
				.as("Should remove the profile without exceptions")
				.isThrownBy(() -> repository.delete(profile));

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to retrieve state for deleted profile")
				.isThrownBy(() -> repository.get(profile))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to retrieve state from profile '%s' as it does not exist", profile.slug());
	}

	@Test
	@DisplayName("should fail to retrieve configuration state for unknown profile branch in a service Git repository")
	void retrieveStateForUnknownProfile() {
		final var profile = createProfile(99L, "unknown profile", ProfilePolicy.UNPROTECTED);

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to retrieve configuration state for unknown profile branch")
				.isThrownBy(() -> repository.get(profile))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode)
				.withMessageContaining("Failed to retrieve state from profile '%s' as it does not exist", profile.slug());
	}

	@Test
	@DisplayName("should successfully delete the service Git repository")
	void shouldDestroyRepository() {
		assertThatNoException()
				.as("Should destroy the repository without exceptions")
				.isThrownBy(() -> repository.destroy());

		assertThat(root.resolve("service-repository-%s".formatted(service.id().serialize())))
				.doesNotExist();

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should fail to load delete repository")
				.isThrownBy(() -> GitStateRepository.load(service, root).close())
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, RepositoryStateException::getErrorCode)
				.withMessageContaining("Could not find repository for Service(%s, %s)", service.id(), service.slug());
	}

	@Test
	@DisplayName("should successfully close Git repository and prevent further operations")
	void shouldCloseRepository() {
		final var profile = createProfile(1L, "test", ProfilePolicy.IMMUTABLE);

		assertThatNoException()
				.as("Should close the repository without exceptions")
				.isThrownBy(() -> repository.close());

		assertThatIllegalStateException()
				.as("Should fail to retrieve configuration state for closed repository")
				.isThrownBy(() -> repository.get(profile))
				.withMessageContaining("Can not perform operations on a closed repository");
	}

	static Changeset changesetFor(String subject, String description, String contents) throws IOException {
		final var author = mock(AuthenticatedPrincipal.class);
		doReturn("test-author").when(author).get();
		doReturn(Optional.of("author@test.com")).when(author).getEmail();
		doReturn(Optional.of("Test Author")).when(author).getDisplayName();

		final var changes = mock(PropertyChanges.class);
		doReturn(subject).when(changes).subject();
		doReturn(description).when(changes).description();

		final var properties = mock(Properties.class);
		doReturn(new ByteArrayInputStream(contents.getBytes())).when(properties).getInputStream();

		return new Changeset(author, properties, changes);
	}

	static Profile createProfile(long id, String name, ProfilePolicy policy) {
		return Profile.builder()
				.id(EntityId.from(id))
				.slug(Slug.slugify(name).get())
				.name(name)
				.policy(policy)
				.build();
	}

}
