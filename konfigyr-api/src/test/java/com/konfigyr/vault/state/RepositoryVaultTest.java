package com.konfigyr.vault.state;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.TestKeysetOperations;
import com.konfigyr.vault.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;

import static com.konfigyr.vault.ProfilePolicyViolationException.ViolationReason.IMMUTABLE_PROFILE;
import static com.konfigyr.vault.ProfilePolicyViolationException.ViolationReason.PROTECTED_PROFILE;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryVaultTest {

	final KeysetOperations keysetOperations = TestKeysetOperations.create();

	@Mock
	Service service;

	@Mock
	Profile profile;

	@Mock
	AuthenticatedPrincipal author;

	@Mock
	StateRepository repository;

	Vault vault;

	@BeforeEach
	void setup() {
		vault = RepositoryVault.builder()
				.author(author)
				.service(service)
				.profile(profile)
				.keysetOperations(keysetOperations)
				.repository(repository)
				.build();
	}

	@Test
	@DisplayName("should return the vault service and profile owners")
	void returnVaultOwners() {
		assertThat(vault.service())
				.isSameAs(service);

		assertThat(vault.profile())
				.isSameAs(profile);
	}

	@Test
	@DisplayName("should close the repository when vault is closed")
	void closeRepositoryOnClose() throws Exception {
		assertThatNoException()
				.isThrownBy(vault::close);

		verify(repository).close();
	}

	@Test
	@DisplayName("should resolve and cache the current configuration properties state from the underlying repository")
	void resolveRepositoryState() {
		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(repository).get(profile);

		final var properties = vault.state();

		assertThat(properties)
				.isNotNull();

		assertThat(vault.state())
				.as("should return the cached configuration properties state")
				.isSameAs(properties);

		verify(repository).get(profile);
	}

	@Test
	@DisplayName("should successfully apply configuration changes to unprotected profile")
	void applyChangesToUnprotectedProfile() {
		doReturn(EntityId.from(9361)).when(profile).id();
		doReturn(ProfilePolicy.UNPROTECTED).when(profile).policy();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Incoming changes")
				.createProperty("server.port", "8080")
				.modifyProperty("server.ssl.enabled", "true")
				.removeProperty("server.ssl.key-store")
				.build();

		final var properties = Properties.builder()
				.add("server.ssl.enabled", PropertyValue.create(
						profile.id(), "server.ssl.enabled", "false").seal(keysetOperations))
				.add("server.ssl.key-store", PropertyValue.create(
						profile.id(), "server.ssl.key-store", "ssl-store.jsk").seal(keysetOperations))
				.build();

		final var state = RepositoryState.builder()
				.revision("previous-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(properties)
				.build();

		doReturn(state).when(repository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), "updated-revision");
		doReturn(updateOutcome).when(repository).update(eq(profile), any());

		final var mergeOutcome = MergeOutcome.applied("profile-branch", state.author(), "merged-revision");
		doReturn(mergeOutcome).when(repository).merge(profile, updateOutcome.branch());

		assertThatObject(vault.apply(changes))
				.returns(mergeOutcome.revision(), ApplyResult::revision)
				.returns(state.revision(), ApplyResult::previousRevision)
				.returns(changes.subject(), ApplyResult::subject)
				.returns(changes.description(), ApplyResult::description)
				.returns(author, ApplyResult::author)
				.returns(mergeOutcome.timestamp(), ApplyResult::timestamp)
				.extracting(ApplyResult::changes, InstanceOfAssertFactories.iterable(PropertyTransition.class))
				.hasSize(changes.size())
				.satisfiesExactly(
						it -> assertThat(it)
								.returns("server.port", PropertyTransition::name)
								.returns(PropertyTransitionType.ADDED, PropertyTransition::type)
								.returns(null, PropertyTransition::from)
								.returns(PropertyValue.create(profile.id(), "server.port", "8080")
										.seal(keysetOperations), PropertyTransition::to),
						it -> assertThat(it)
								.returns("server.ssl.enabled", PropertyTransition::name)
								.returns(PropertyTransitionType.UPDATED, PropertyTransition::type)
								.returns(PropertyValue.create(profile.id(), "server.ssl.enabled", "false")
											.seal(keysetOperations), PropertyTransition::from)
								.returns(PropertyValue.create(profile.id(), "server.ssl.enabled", "true")
										.seal(keysetOperations), PropertyTransition::to),
						it -> assertThat(it)
								.returns("server.ssl.key-store", PropertyTransition::name)
								.returns(PropertyTransitionType.REMOVED, PropertyTransition::type)
								.returns(PropertyValue.create(profile.id(), "server.ssl.key-store", "ssl-store.jsk")
										.seal(keysetOperations), PropertyTransition::from)
								.returns(null, PropertyTransition::to)
				);

		verify(repository, never()).discard(profile, updateOutcome.branch());
	}

	@Test
	@DisplayName("should fail to apply configuration changes when changeset branch returns a conflicting outcome")
	void conflictingChangesetBranchOutcomeWhenApplying() {
		doReturn(EntityId.from(9361)).when(profile).id();
		doReturn(ProfilePolicy.UNPROTECTED).when(profile).policy();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Incoming changes")
				.createProperty("server.port", "8080")
				.build();

		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(repository).get(profile);

		final var updateOutcome = MergeOutcome.conflicting("changeset-branch", state.author(), "conflict");
		doReturn(updateOutcome).when(repository).update(eq(profile), any());

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.apply(changes))
				.withMessageContaining(
						"Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s",
						profile.slug(), service.id(), service.slug(), updateOutcome
				)
				.withNoCause();

		verify(repository).discard(profile, updateOutcome.branch());
	}

	@Test
	@DisplayName("should fail to apply configuration changes when changeset branch returns an unknown outcome")
	void unknownChangesetBranchOutcomeWhenApplying() {
		doReturn(EntityId.from(9361)).when(profile).id();
		doReturn(ProfilePolicy.UNPROTECTED).when(profile).policy();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Incoming changes")
				.createProperty("server.port", "8080")
				.build();

		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(repository).get(profile);

		final var updateOutcome = MergeOutcome.unknown("changeset-branch", state.author());
		doReturn(updateOutcome).when(repository).update(eq(profile), any());

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.apply(changes))
				.withMessageContaining(
						"Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s",
						profile.slug(), service.id(), service.slug(), updateOutcome
				)
				.withNoCause();

		verify(repository).discard(profile, updateOutcome.branch());
	}

	@Test
	@DisplayName("should fail to apply configuration changes when profile branch returns a conflicting outcome")
	void conflictingProfileBranchOutcomeWhenApplying() {
		doReturn(EntityId.from(9361)).when(profile).id();
		doReturn(ProfilePolicy.UNPROTECTED).when(profile).policy();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Incoming changes")
				.createProperty("server.port", "8080")
				.build();

		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(repository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), state.revision());
		doReturn(updateOutcome).when(repository).update(eq(profile), any());

		final var mergeOutcome = MergeOutcome.conflicting("profile-branch", state.author(), "conflicts");
		doReturn(mergeOutcome).when(repository).merge(profile, updateOutcome.branch());

		assertThatExceptionOfType(ConflictingProfileStateException.class)
				.isThrownBy(() -> vault.apply(changes))
				.returns(profile, ConflictingProfileStateException::getProfile)
				.returns(mergeOutcome.conflicts(), ConflictingProfileStateException::getConflicts)
				.withMessageContaining("Configuration state conflicts occurred while applying changes to profile: %s", profile.name())
				.withNoCause();

		verify(repository).discard(profile, updateOutcome.branch());
	}

	@Test
	@DisplayName("should fail to apply configuration changes when profile branch returns an unknown outcome")
	void unknownProfileBranchOutcomeWhenApplying() {
		doReturn(EntityId.from(9361)).when(profile).id();
		doReturn(ProfilePolicy.UNPROTECTED).when(profile).policy();

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Incoming changes")
				.createProperty("server.port", "8080")
				.build();

		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(repository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), state.revision());
		doReturn(updateOutcome).when(repository).update(eq(profile), any());

		final var mergeOutcome = MergeOutcome.unknown("profile-branch", state.author());
		doReturn(mergeOutcome).when(repository).merge(profile, updateOutcome.branch());

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.apply(changes))
				.withMessageContaining(
						"Failed to apply changes to profile '%s' of Service(%s, %s) due to: %s",
						profile.slug(), service.id(), service.slug(), mergeOutcome
				)
				.withNoCause();

		verify(repository).discard(profile, updateOutcome.branch());
	}

	@Test
	@DisplayName("should fail to apply configuration changes to immutable profile")
	void applyChangesToImmutableProfile() {
		final var changes = mock(PropertyChanges.class);
		doReturn(ProfilePolicy.IMMUTABLE).when(profile).policy();

		assertThatExceptionOfType(ProfilePolicyViolationException.class)
				.isThrownBy(() -> vault.apply(changes))
				.returns(IMMUTABLE_PROFILE, ProfilePolicyViolationException::getReason)
				.returns(HttpStatus.CONFLICT, ProfilePolicyViolationException::getStatusCode)
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to apply configuration changes to protected profile")
	void applyChangesToProtectedProfile() {
		final var changes = mock(PropertyChanges.class);
		doReturn(ProfilePolicy.PROTECTED).when(profile).policy();

		assertThatExceptionOfType(ProfilePolicyViolationException.class)
				.isThrownBy(() -> vault.apply(changes))
				.returns(PROTECTED_PROFILE, ProfilePolicyViolationException::getReason)
				.returns(HttpStatus.CONFLICT, ProfilePolicyViolationException::getStatusCode)
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to submit proposed configuration changes to immutable profile")
	void submitChangesToImmutableProfile() {
		final var changes = mock(PropertyChanges.class);
		doReturn(ProfilePolicy.IMMUTABLE).when(profile).policy();

		assertThatExceptionOfType(ProfilePolicyViolationException.class)
				.isThrownBy(() -> vault.submit(changes))
				.returns(IMMUTABLE_PROFILE, ProfilePolicyViolationException::getReason)
				.returns(HttpStatus.CONFLICT, ProfilePolicyViolationException::getStatusCode)
				.withNoCause();
	}

}
