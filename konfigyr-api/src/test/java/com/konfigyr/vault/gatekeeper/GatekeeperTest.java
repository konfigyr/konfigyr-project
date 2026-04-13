package com.konfigyr.vault.gatekeeper;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.vault.*;
import com.konfigyr.vault.state.GitStateRepository;
import com.konfigyr.vault.state.RepositoryStateException;
import com.konfigyr.vault.state.StateRepository;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;
import static com.konfigyr.data.tables.VaultChangeRequestEvents.VAULT_CHANGE_REQUEST_EVENTS;

import static org.assertj.core.api.Assertions.*;

class GatekeeperTest extends AbstractIntegrationTest {

	static final EntityId CHANGE_REQUEST_ID = EntityId.from(123456789);

	@Autowired
	VaultProperties properties;

	@Autowired
	ChangeRequestGatekeeper gatekeeper;

	@Autowired
	ProfileManager profiles;

	@Autowired
	Services services;

	@Autowired
	DSLContext context;

	Service service;
	Profile profile;
	StateRepository repository;

	@BeforeEach
	void setup() {
		service = services.get(EntityId.from(2)).orElseThrow();
		profile = profiles.get(service, "development").orElseThrow();

		// set up the repository and the profile branch
		repository = GitStateRepository.initialize(service, properties.getRepositoryDirectory());
		repository.create(profile);
	}

	@AfterEach
	void cleanup() throws Exception {
		repository.destroy();
	}

	@Test
	@DisplayName("should fail to evaluate merge status for unknown change request")
	void evaluateForUnknownChangeRequest() {
		assertThatExceptionOfType(ChangeRequestNotFoundException.class)
				.isThrownBy(() -> gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.withNoCause();
	}

	@Test
	@Transactional
	@DisplayName("should fail to evaluate merge status when repository does not exist")
	void evaluateForUnknownRepository() {
		assertThatNoException()
				.as("Should destroy the repository without exceptions")
				.isThrownBy(repository::destroy);

		setupChangeRequest(ChangeRequestState.OPEN);

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_REPOSITORY, RepositoryStateException::getErrorCode);
	}

	@Test
	@Transactional
	@DisplayName("should fail to evaluate merge status when profile branch does not exist")
	void evaluateForUnknownProfileBranch() {
		assertThatNoException()
				.as("Should delete the profile branch without exceptions")
				.isThrownBy(() -> repository.delete(profile));

		setupChangeRequest(ChangeRequestState.OPEN);

		assertThatExceptionOfType(RepositoryStateException.class)
				.isThrownBy(() -> gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.returns(RepositoryStateException.ErrorCode.UNKNOWN_PROFILE, RepositoryStateException::getErrorCode);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate merge status for merged change request")
	void evaluateForMergedChangeRequest() {
		setupChangeRequest(ChangeRequestState.MERGED);

		assertThat(gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.isEqualTo(ChangeRequestMergeStatus.NOT_OPEN);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate merge status for discarded change request")
	void evaluateForDiscardedChangeRequest() {
		setupChangeRequest(ChangeRequestState.DISCARDED);

		assertThat(gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.isEqualTo(ChangeRequestMergeStatus.NOT_OPEN);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate merge status for outdated change request")
	void evaluateForOutdatedChangeRequest() {
		setupChangeRequest(ChangeRequestState.OPEN);

		assertThat(gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.isEqualTo(ChangeRequestMergeStatus.OUTDATED);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate merge status for not yet approved change request")
	void evaluateForUnapprovedChangeRequest() {
		final var state = repository.get(profile);

		setupChangeRequest(ChangeRequestState.OPEN, state.revision());

		assertThat(gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.isEqualTo(ChangeRequestMergeStatus.MERGEABLE);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate merge status for approved change request")
	void evaluateForApprovedChangeRequest() {
		final var state = repository.get(profile);

		setupChangeRequest(ChangeRequestState.OPEN, state.revision());
		submitReview(ChangeRequestHistory.Type.APPROVED);

		assertThat(gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.isEqualTo(ChangeRequestMergeStatus.MERGEABLE);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate merge status for change request that requires further changes or updates")
	void evaluateForChangeRequestedChangeRequest() {
		final var state = repository.get(profile);

		setupChangeRequest(ChangeRequestState.OPEN, state.revision());
		submitReview(ChangeRequestHistory.Type.CHANGES_REQUESTED);

		assertThat(gatekeeper.evaluate(CHANGE_REQUEST_ID))
				.isEqualTo(ChangeRequestMergeStatus.CHANGES_REQUESTED);
	}

	private void setupChangeRequest(ChangeRequestState state) {
		setupChangeRequest(state, "head-revision");
	}

	private void setupChangeRequest(ChangeRequestState state, String base) {
		context.insertInto(VAULT_CHANGE_REQUESTS)
				.set(VAULT_CHANGE_REQUESTS.ID, CHANGE_REQUEST_ID.get())
				.set(VAULT_CHANGE_REQUESTS.SERVICE_ID, profile.service().get())
				.set(VAULT_CHANGE_REQUESTS.PROFILE_ID, profile.id().get())
				.set(VAULT_CHANGE_REQUESTS.NUMBER, CHANGE_REQUEST_ID.get())
				.set(VAULT_CHANGE_REQUESTS.STATE, state.name())
				.set(VAULT_CHANGE_REQUESTS.MERGE_STATUS, ChangeRequestMergeStatus.CHECKING.name())
				.set(VAULT_CHANGE_REQUESTS.CHANGE_COUNT, 0)
				.set(VAULT_CHANGE_REQUESTS.BRANCH_NAME, "change-request-branch")
				.set(VAULT_CHANGE_REQUESTS.BASE_REVISION, base)
				.set(VAULT_CHANGE_REQUESTS.HEAD_REVISION, "head-revision")
				.set(VAULT_CHANGE_REQUESTS.SUBJECT, "Test change request")
				.set(VAULT_CHANGE_REQUESTS.CREATED_BY, "John Doe")
				.execute();
	}

	private void submitReview(ChangeRequestHistory.Type type) {
		context.insertInto(VAULT_CHANGE_REQUEST_EVENTS)
				.set(VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID, CHANGE_REQUEST_ID.get())
				.set(VAULT_CHANGE_REQUEST_EVENTS.TYPE, type.name())
				.set(VAULT_CHANGE_REQUEST_EVENTS.INITIATOR, "John Doe")
				.execute();
	}

}
