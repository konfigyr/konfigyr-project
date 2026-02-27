package com.konfigyr.vault.state;

import com.google.crypto.tink.subtle.Base64;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.vault.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.nio.charset.MalformedInputException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class RepositoryVaultManagerTest extends AbstractIntegrationTest {

	static byte[] INVALID_STATE = Base64.decode("/8Cw4py/goA=");

	@Autowired
	VaultProperties properties;

	@Autowired
	ProfileManager profiles;

	@Autowired
	Services services;

	@Autowired
	VaultAccessor accessor;

	Service service;
	GitStateRepository repository;
	AuthenticatedPrincipal principal;

	@BeforeEach
	void setup() {
		principal = authenticatedPrincipal();
		service = services.get(EntityId.from(2)).orElseThrow();
		repository = GitStateRepository.initialize(service, properties.getRepositoryDirectory());
	}

	@AfterEach
	void cleanup() {
		repository.destroy();
		repository.close();
	}

	@Test
	@DisplayName("should open the access to the specific vault for principal")
	void shouldOpenVault() throws Exception {
		final var profile = prepareServiceProfile("development");

		try (var vault = accessor.open(principal, service, profile)) {
			assertThat(vault)
					.returns(service, Vault::service)
					.returns(profile, Vault::profile);

			assertThat(vault.state())
					.as("Should return an empty sealed property state")
					.isEmpty();
		}
	}

	@Test
	@DisplayName("should apply changes to unprotected service profile")
	void shouldApplyChangesToUnprotectedProfile() {
		final var profile = prepareServiceProfile("development");
		final var vault = accessor.open(principal, service, profile);

		ApplyResult result = vault.apply(
				PropertyChanges.builder()
						.profile(profile)
						.subject("Test changes")
						.description("Additional description about the changes")
						.createProperty("server.port", "8080")
						.createProperty("server.address", "localhost")
						.build()
		);

		assertThat(result.changes())
				.hasSize(2)
				.hasEntrySatisfying("server.port", it -> assertThat(it)
						.returns(PropertyHistory.Action.ADDED, PropertyHistory::action)
						.returns("8080", PropertyHistory::newValue)
				)
				.hasEntrySatisfying("server.address", it -> assertThat(it)
						.returns(PropertyHistory.Action.ADDED, PropertyHistory::action)
						.returns("localhost", PropertyHistory::newValue)
				);

		assertThat(vault.state())
				.as("Should return sealed property state with 3 properties")
				.hasSize(2);

		assertThat(vault.unseal())
				.as("Should unseal vault state and return decrypted property state")
				.hasSize(2)
				.containsEntry("server.port", "8080")
				.containsEntry("server.address", "localhost");

		result = vault.apply(
				PropertyChanges.builder()
						.profile(profile)
						.subject("Second changes")
						.createProperty("spring.application.name", "test-application")
						.modifyProperty("server.port", "8888")
						.removeProperty("server.address")
						.build()
		);

		assertThat(result.changes())
				.hasSize(3)
				.hasEntrySatisfying("spring.application.name", it -> assertThat(it)
						.returns(PropertyHistory.Action.ADDED, PropertyHistory::action)
						.returns("test-application", PropertyHistory::newValue)
				)
				.hasEntrySatisfying("server.port", it -> assertThat(it)
						.returns(PropertyHistory.Action.UPDATED, PropertyHistory::action)
						.returns("8888", PropertyHistory::newValue)
						.returns("8080", PropertyHistory::oldValue)
				)
				.hasEntrySatisfying("server.address", it -> assertThat(it)
						.returns(PropertyHistory.Action.REMOVED, PropertyHistory::action)
						.returns("localhost", PropertyHistory::oldValue)
				);

		assertThat(vault.state())
				.as("Should return sealed property state with 2 properties")
				.hasSize(2);

		assertThat(vault.unseal())
				.as("Should unseal vault state and return decrypted property state")
				.hasSize(2)
				.containsEntry("spring.application.name", "test-application")
				.containsEntry("server.port", "8888");

		assertThatNoException()
				.as("Should close vault without exceptions")
				.isThrownBy(vault::close);
	}

	@Test
	@DisplayName("should fail to submit changes as it is not implemented")
	void shouldNotSupportSubmitOperation() {
		final var profile = prepareServiceProfile("staging");
		final var vault = accessor.open(principal, service, profile);

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("server.port", "8080")
				.build();

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Should not allow to submit changes as it is not yet implemented")
				.isThrownBy(() -> vault.submit(changes))
				.withMessageContaining("Submitting changes is not yet supported")
				.withNoCause();

		assertThat(vault.state())
				.as("No changes should be applied to the vault state")
				.isEmpty();

		assertThatNoException()
				.as("Should close vault without exceptions")
				.isThrownBy(vault::close);
	}

	@Test
	@DisplayName("should fail to apply changes directly to a protected service profile")
	void shouldNotApplyChangesToProtectedProfile() {
		final var profile = prepareServiceProfile("production");
		final var vault = accessor.open(principal, service, profile);

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("server.port", "8080")
				.build();

		assertThatExceptionOfType(ProfilePolicyViolationException.class)
				.as("Should not allow to apply changes to a protected profile")
				.isThrownBy(() -> vault.apply(changes))
				.returns(profile, ProfilePolicyViolationException::getProfile)
				.returns(ProfilePolicy.PROTECTED, ProfilePolicyViolationException::getPolicy)
				.returns(ProfilePolicyViolationException.ViolationReason.PROTECTED_PROFILE, ProfilePolicyViolationException::getReason)
				.withMessageContaining("Profile '%s' is protected. Changes must be submitted for approval.", profile.slug())
				.withNoCause();

		assertThat(vault.state())
				.as("No changes should be applied to the vault state")
				.isEmpty();

		assertThatNoException()
				.as("Should close vault without exceptions")
				.isThrownBy(vault::close);
	}

	@Test
	@DisplayName("should fail to apply changes directly to an immutable service profile")
	void shouldNotApplyChangesToImmutableProfile() {
		final var profile = prepareServiceProfile("locked");
		final var vault = accessor.open(principal, service, profile);

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("server.port", "8080")
				.build();

		assertThatExceptionOfType(ProfilePolicyViolationException.class)
				.as("Should not allow to apply changes to an immutable profile")
				.isThrownBy(() -> vault.apply(changes))
				.returns(profile, ProfilePolicyViolationException::getProfile)
				.returns(ProfilePolicy.IMMUTABLE, ProfilePolicyViolationException::getPolicy)
				.returns(ProfilePolicyViolationException.ViolationReason.IMMUTABLE_PROFILE, ProfilePolicyViolationException::getReason)
				.withMessageContaining("Profile '%s' is read-only and does not allow state modifications.", profile.slug())
				.withNoCause();

		assertThat(vault.state())
				.as("No changes should be applied to the vault state")
				.isEmpty();

		assertThatNoException()
				.as("Should close vault without exceptions")
				.isThrownBy(vault::close);
	}

	@Test
	@DisplayName("should fail to submit changes directly to an immutable service profile")
	void shouldNotSubmitChangesToImmutableProfile() {
		final var profile = prepareServiceProfile("locked");
		final var vault = accessor.open(principal, service, profile);

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("server.port", "8080")
				.build();

		assertThatExceptionOfType(ProfilePolicyViolationException.class)
				.as("Should not allow to submit changes to an immutable profile")
				.isThrownBy(() -> vault.submit(changes))
				.returns(profile, ProfilePolicyViolationException::getProfile)
				.returns(ProfilePolicy.IMMUTABLE, ProfilePolicyViolationException::getPolicy)
				.returns(ProfilePolicyViolationException.ViolationReason.IMMUTABLE_PROFILE, ProfilePolicyViolationException::getReason)
				.withMessageContaining("Profile '%s' is read-only and does not allow state modifications.", profile.slug())
				.withNoCause();

		assertThat(vault.state())
				.as("No changes should be applied to the vault state")
				.isEmpty();

		assertThatNoException()
				.as("Should close vault without exceptions")
				.isThrownBy(vault::close);
	}

	@Test
	@DisplayName("should fail to read state from an unknown profile branch")
	void readStateFromMissingProfileBranch() {
		final var profile = lookupProfile("locked");
		final var vault = accessor.open(principal, service, profile);

		assertThatExceptionOfType(RepositoryStateException.class)
				.as("Should throw an unknown profile state exception for missing profile branch")
				.isThrownBy(vault::state)
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode);

		assertThatNoException()
				.as("Should close vault without exceptions")
				.isThrownBy(vault::close);
	}

	@Test
	@DisplayName("should fail to read state from a corrupted profile branch state")
	void readCorruptedStateFromProfileBranch() throws Exception {
		final var profile = prepareServiceProfile("staging");

		final var changes = PropertyChanges.builder()
				.profile(profile)
				.subject("Test changes")
				.createProperty("server.port", "8080")
				.build();

		// build the corrupted changeset to be stored...
		final var changeset = mock(Changeset.class);
		doReturn(changes).when(changeset).changes();
		doReturn(principal).when(changeset).author();
		doReturn(new ByteArrayInputStream(INVALID_STATE)).when(changeset).getInputStream();

		// merge the corrupted state to the profile branch...
		repository.merge(profile, repository.update(profile, changeset).branch());

		try (var vault = accessor.open(principal, service, profile)) {
			assertThatExceptionOfType(RepositoryStateException.class)
					.as("Should throw a corrupted profile state exception when reading state")
					.isThrownBy(vault::state)
					.returns(RepositoryStateException.ErrorCode.CORRUPTED_STATE, RepositoryStateException::getErrorCode)
					.withMessageContaining(
							"Failed read repository configuration state for '%s' profile of Service(%s, %s)",
							profile.slug(), service.id(), service.slug()
					)
					.withCauseInstanceOf(MalformedInputException.class);
		}
	}

	Profile lookupProfile(String name) {
		return assertThat(profiles.get(service, name))
				.as("Should find profile '%s' for service '%s'", name, service.id())
				.isPresent()
				.get()
				.actual();
	}

	Profile prepareServiceProfile(String name) {
		final var profile = lookupProfile(name);

		assertThatNoException()
				.as("Should create a new branch for profile '%s' without exceptions", profile.slug())
				.isThrownBy(() -> repository.create(profile));

		return profile;
	}

	static AuthenticatedPrincipal authenticatedPrincipal() {
		final var account = TestAccounts.john().build();
		final var principal = mock(AuthenticatedPrincipal.class);
		doReturn(account.id().serialize()).when(principal).get();
		doReturn(Optional.of(account.email())).when(principal).getEmail();
		doReturn(Optional.of(account.displayName())).when(principal).getDisplayName();
		return principal;
	}

}
