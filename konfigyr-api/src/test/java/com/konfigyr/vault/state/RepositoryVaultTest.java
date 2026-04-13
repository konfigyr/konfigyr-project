package com.konfigyr.vault.state;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.TestKeysetOperations;
import com.konfigyr.vault.*;
import com.konfigyr.vault.changes.ChangeRequestCreateCommand;
import com.konfigyr.vault.changes.ChangeRequestManager;
import com.konfigyr.vault.changes.ChangeRequestRevision;
import com.konfigyr.vault.changes.ChangeRequestUpdateCommand;
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
	ChangeRequest changeRequest;

	@Mock
	Service service;

	@Mock
	Profile profile;

	@Mock
	AuthenticatedPrincipal author;

	@Mock
	StateRepository stateRepository;

	@Mock
	ChangeRequestManager changeRequestManager;

	Vault vault;

	@BeforeEach
	void setup() {
		vault = RepositoryVault.builder()
				.author(author)
				.service(service)
				.profile(profile)
				.stateRepository(stateRepository)
				.keysetOperations(keysetOperations)
				.changeRequestManager(changeRequestManager)
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

		verify(stateRepository).close();
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

		doReturn(state).when(stateRepository).get(profile);

		final var properties = vault.state();

		assertThat(properties)
				.isNotNull();

		assertThat(vault.state())
				.as("should return the cached configuration properties state")
				.isSameAs(properties);

		verify(stateRepository).get(profile);
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

		doReturn(state).when(stateRepository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), "updated-revision");
		doReturn(updateOutcome).when(stateRepository).update(eq(profile), any());

		final var mergeOutcome = MergeOutcome.applied("profile-branch", state.author(), "merged-revision");
		doReturn(mergeOutcome).when(stateRepository).merge(profile, updateOutcome.branch());

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

		verify(stateRepository, never()).discard(profile, updateOutcome.branch());
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

		doReturn(state).when(stateRepository).get(profile);

		final var updateOutcome = MergeOutcome.conflicting("changeset-branch", state.author(), "conflict");
		doReturn(updateOutcome).when(stateRepository).update(eq(profile), any());

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.apply(changes))
				.withMessageContaining(
						"Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s",
						profile.slug(), service.id(), service.slug(), updateOutcome
				)
				.withNoCause();

		verify(stateRepository).discard(profile, updateOutcome.branch());
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

		doReturn(state).when(stateRepository).get(profile);

		final var updateOutcome = MergeOutcome.unknown("changeset-branch", state.author());
		doReturn(updateOutcome).when(stateRepository).update(eq(profile), any());

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.apply(changes))
				.withMessageContaining(
						"Failed to prepare changeset for profile '%s' of Service(%s, %s) due to: %s",
						profile.slug(), service.id(), service.slug(), updateOutcome
				)
				.withNoCause();

		verify(stateRepository).discard(profile, updateOutcome.branch());
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

		doReturn(state).when(stateRepository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), state.revision());
		doReturn(updateOutcome).when(stateRepository).update(eq(profile), any());

		final var mergeOutcome = MergeOutcome.conflicting("profile-branch", state.author(), "conflicts");
		doReturn(mergeOutcome).when(stateRepository).merge(profile, updateOutcome.branch());

		assertThatExceptionOfType(ConflictingProfileStateException.class)
				.isThrownBy(() -> vault.apply(changes))
				.returns(profile, ConflictingProfileStateException::getProfile)
				.returns(mergeOutcome.conflicts(), ConflictingProfileStateException::getConflicts)
				.withMessageContaining("Configuration state conflicts occurred while applying changes to profile: %s", profile.name())
				.withNoCause();

		verify(stateRepository).discard(profile, updateOutcome.branch());
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

		doReturn(state).when(stateRepository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), state.revision());
		doReturn(updateOutcome).when(stateRepository).update(eq(profile), any());

		final var mergeOutcome = MergeOutcome.unknown("profile-branch", state.author());
		doReturn(mergeOutcome).when(stateRepository).merge(profile, updateOutcome.branch());

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.apply(changes))
				.withMessageContaining(
						"Failed to apply changes to profile '%s' of Service(%s, %s) due to: %s",
						profile.slug(), service.id(), service.slug(), mergeOutcome
				)
				.withNoCause();

		verify(stateRepository).discard(profile, updateOutcome.branch());
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

	@Test
	@DisplayName("should successfully submit configuration changes to protected profile and create change request")
	void submitChangesToProtectedProfile() {
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

		doReturn(state).when(stateRepository).get(profile);

		final var updateOutcome = MergeOutcome.applied("changeset-branch", state.author(), "updated-revision");
		doReturn(updateOutcome).when(stateRepository).update(eq(profile), any());

		doReturn(changeRequest).when(changeRequestManager).create(assertArg(cmd -> assertThat(cmd)
				.returns(service, ChangeRequestCreateCommand::service)
				.returns(profile, ChangeRequestCreateCommand::profile)
				.returns("changeset-branch", ChangeRequestCreateCommand::branch)
				.extracting(ChangeRequestCreateCommand::result)
				.returns("updated-revision", ApplyResult::revision)
				.returns(state.revision(), ApplyResult::previousRevision)
				.returns(changes.subject(), ApplyResult::subject)
				.returns(changes.description(), ApplyResult::description)
				.returns(author, ApplyResult::author)
				.returns(updateOutcome.timestamp(), ApplyResult::timestamp)
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
				)
		));

		assertThatObject(vault.submit(changes))
				.isNotNull()
				.isEqualTo(changeRequest);

		verify(stateRepository, never()).discard(profile, updateOutcome.branch());
		verify(stateRepository, never()).merge(profile, updateOutcome.branch());
	}

	@Test
	@DisplayName("should fail to merge change request when not in open state")
	void failToMergeNonOpenChangeRequest() {
		doReturn(ChangeRequestState.MERGED).when(changeRequest).state();

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.merge(changeRequest))
				.withMessageContaining("must be in open state to be merged");

		verify(stateRepository, never()).merge(eq(profile), anyString());
		verifyNoInteractions(changeRequestManager);
	}

	@Test
	@DisplayName("should fail to merge change request when change request revision is not found")
	void failToMergeChangeRequestWithInvalidRevision() {
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		doThrow(ChangeRequestNotFoundException.class).when(changeRequestManager).revision(changeRequest);

		assertThatExceptionOfType(ChangeRequestNotFoundException.class)
				.isThrownBy(() -> vault.merge(changeRequest));

		verify(stateRepository, never()).merge(eq(profile), anyString());
		verify(changeRequestManager, never()).update(any());
	}

	@Test
	@DisplayName("should fail to merge change request due to conflicts between two states")
	void failToMergeConflictingChangeRequest() {
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		final var revision = mock(ChangeRequestRevision.class);
		doReturn("refs/heads/changeset/test/6").when(revision).branch();
		doReturn(revision).when(changeRequestManager).revision(changeRequest);

		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(stateRepository).get(profile);

		final var outcome = MergeOutcome.conflicting("git-revision", state.author(), "conflict");
		doReturn(outcome).when(stateRepository).merge(profile, "refs/heads/changeset/test/6");

		assertThatExceptionOfType(ConflictingProfileStateException.class)
				.isThrownBy(() -> vault.merge(changeRequest))
				.withMessageContaining("Configuration state conflicts occurred while applying changes to profile")
				.returns("conflict", ConflictingProfileStateException::getConflicts)
				.withNoCause();

		verify(changeRequestManager, never()).update(any());
	}

	@Test
	@DisplayName("should fail to merge change request due to unknown merge outcome")
	void failToMergeChangeRequestDueToUnkownOutcome() {
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		final var revision = mock(ChangeRequestRevision.class);
		doReturn("refs/heads/changeset/test/100").when(revision).branch();
		doReturn(revision).when(changeRequestManager).revision(changeRequest);

		final var state = RepositoryState.builder()
				.revision("git-revision")
				.summary("Latest configuration state")
				.author("john.doe@konfigyr.com")
				.timestamp(OffsetDateTime.now())
				.contents(() -> new ByteArrayInputStream(new byte[0]))
				.build();

		doReturn(state).when(stateRepository).get(profile);

		final var outcome = MergeOutcome.unknown("git-revision", state.author());
		doReturn(outcome).when(stateRepository).merge(profile, "refs/heads/changeset/test/100");

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.merge(changeRequest))
				.withMessageContaining("Failed to apply changes to profile")
				.withNoCause();

		verify(changeRequestManager, never()).update(any());
	}

	@Test
	@DisplayName("should successfully discard change request")
	void discardChangeRequest() {
		doReturn(service).when(changeRequest).service();
		doReturn(7L).when(changeRequest).number();
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		final var revision = mock(ChangeRequestRevision.class);
		doReturn("refs/heads/changeset/test/7").when(revision).branch();
		doReturn(revision).when(changeRequestManager).revision(changeRequest);

		final var discarded = mock(ChangeRequest.class);
		doReturn(discarded).when(changeRequestManager).update(assertArg(cmd -> assertThat(cmd)
				.returns(service, ChangeRequestUpdateCommand::service)
				.returns(7L, ChangeRequestUpdateCommand::number)
				.returns(author, ChangeRequestUpdateCommand::principal)
				.returns(ChangeRequestState.DISCARDED, ChangeRequestUpdateCommand::state)
				.returns(null, ChangeRequestUpdateCommand::subject)
				.returns(null, ChangeRequestUpdateCommand::description)
		));

		assertThat(vault.discard(changeRequest))
				.isEqualTo(discarded);

		verify(stateRepository).discard(profile, "refs/heads/changeset/test/7");
	}

	@Test
	@DisplayName("should successfully discard change request when changeset branch is not found")
	void discardChangeRequestWhenBranchIsMissing() {
		doReturn(service).when(changeRequest).service();
		doReturn(1376L).when(changeRequest).number();
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		final var revision = mock(ChangeRequestRevision.class);
		doReturn("refs/heads/changeset/test/1376").when(revision).branch();
		doReturn(revision).when(changeRequestManager).revision(changeRequest);

		final var cause = new RepositoryStateException(RepositoryStateException.ErrorCode.UNKNOWN_CHANGESET, "Missing branch");
		doThrow(cause).when(stateRepository).discard(profile, "refs/heads/changeset/test/1376");

		final var discarded = mock(ChangeRequest.class);
		doReturn(discarded).when(changeRequestManager).update(any());

		assertThat(vault.discard(changeRequest))
				.isEqualTo(discarded);

		verify(stateRepository).discard(profile, "refs/heads/changeset/test/1376");
	}

	@Test
	@DisplayName("should fail to discard change request when not in open state")
	void failToDiscardNonOpenChangeRequest() {
		doReturn(ChangeRequestState.MERGED).when(changeRequest).state();

		assertThatIllegalStateException()
				.isThrownBy(() -> vault.discard(changeRequest))
				.withMessageContaining("must be in open state to be discarded");

		verify(stateRepository, never()).discard(eq(profile), anyString());
		verifyNoInteractions(changeRequestManager);
	}

	@Test
	@DisplayName("should fail to discard change request when change request revision is not found")
	void failToDiscardChangeRequestWithInvalidRevision() {
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		doThrow(ChangeRequestNotFoundException.class).when(changeRequestManager).revision(changeRequest);

		assertThatExceptionOfType(ChangeRequestNotFoundException.class)
				.isThrownBy(() -> vault.discard(changeRequest));

		verify(stateRepository, never()).discard(eq(profile), anyString());
		verify(changeRequestManager, never()).update(any());
	}

	@Test
	@DisplayName("should fail to discard change request due repository state exception")
	void failToDiscardChangeRequestDueToRepositoryStateException() {
		doReturn(ChangeRequestState.OPEN).when(changeRequest).state();

		final var revision = mock(ChangeRequestRevision.class);
		doReturn("refs/heads/changeset/test/1").when(revision).branch();
		doReturn(revision).when(changeRequestManager).revision(changeRequest);

		final var cause = new RepositoryStateException(RepositoryStateException.ErrorCode.UNAVAILABLE, "Unavailable");
		doThrow(cause).when(stateRepository).discard(profile, "refs/heads/changeset/test/1");

		assertThatException()
				.isThrownBy(() -> vault.discard(changeRequest))
				.isEqualTo(cause);

		verify(stateRepository).discard(profile, "refs/heads/changeset/test/1");
		verify(changeRequestManager, never()).update(any());
	}

}
