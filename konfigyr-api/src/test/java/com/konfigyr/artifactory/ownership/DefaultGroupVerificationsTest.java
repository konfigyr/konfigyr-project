package com.konfigyr.artifactory.ownership;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.CommunicationException;
import javax.naming.directory.InitialDirContext;

import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.mockDns;
import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.mockDnsException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

class DefaultGroupVerificationsTest extends AbstractIntegrationTest {

	@Autowired
	GroupVerifications verifications;

	@MockitoSpyBean
	SourceCodeVerificationStrategy sourceCodeVerificationStrategy;

	@Test
	@DisplayName("should find active covering by group id and owner")
	void shouldFundActiveCoveringByIdAndOwner() {
		final var owner = new Owner(EntityId.from(2), "konfigyr");
		final var groupId = "com.konfigyr";
		final var result = verifications.findActiveCovering(owner, groupId);

		assertThat(result)
				.isPresent()
				.get()
				.returns(EntityId.from(1L), GroupVerification::id)
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.ACTIVE, GroupVerification::state)
				.returns(null, GroupVerification::revokedAt)
				.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull());
	}

	@Test
	@DisplayName("should return empty result for the unrelated groupId")
	void shouldReturnEmptyResultForUnrelatedGroupId() {
		final var owner = new Owner(EntityId.from(2), "konfigyr");
		final var groupId = "com.config";
		final var result = verifications.findActiveCovering(owner, groupId);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("should find covering by owner")
	void shouldFindCoveringByOwner() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var result = verifications.findByOwner(owner, SearchQuery.of(Pageable.ofSize(10)));

		assertThat(result.stream())
				.hasSize(2)
				.extracting(GroupVerification::id)
				.contains(EntityId.from(2), EntityId.from(3));
	}

	@Test
	@DisplayName("should find claims by owner filtered by state")
	void shouldFindByOwnerFilteredByState() {
		final var owner = new Owner(EntityId.from(1), "john-doe");

		final var pending = verifications.findByOwner(owner, SearchQuery.builder()
				.pageable(Pageable.ofSize(10))
				.criteria(GroupVerification.STATE_CRITERIA, VerificationState.PENDING)
				.build());

		assertThat(pending.stream())
				.hasSize(1)
				.extracting(GroupVerification::id)
				.containsExactly(EntityId.from(3));

		final var failed = verifications.findByOwner(owner, SearchQuery.builder()
				.pageable(Pageable.ofSize(10))
				.criteria(GroupVerification.STATE_CRITERIA, VerificationState.FAILED)
				.build());

		assertThat(failed.stream())
				.hasSize(1)
				.extracting(GroupVerification::id)
				.containsExactly(EntityId.from(2));
	}

	@Test
	@DisplayName("should find claims by owner filtered by search term")
	void shouldFindByOwnerFilteredByTerm() {
		final var owner = new Owner(EntityId.from(1), "john-doe");

		final var both = verifications.findByOwner(owner, SearchQuery.builder()
				.pageable(Pageable.ofSize(10))
				.term("springframework")
				.build());

		assertThat(both.stream())
				.hasSize(2)
				.extracting(GroupVerification::id)
				.contains(EntityId.from(2), EntityId.from(3));

		final var one = verifications.findByOwner(owner, SearchQuery.builder()
				.pageable(Pageable.ofSize(10))
				.term("boot")
				.build());

		assertThat(one.stream())
				.hasSize(1)
				.extracting(GroupVerification::groupId)
				.containsExactly("org.springframework.boot");
	}

	@Test
	@DisplayName("should return empty page when no claims match state filter")
	void shouldReturnEmptyPageForUnmatchedStateFilter() {
		final var owner = new Owner(EntityId.from(1), "john-doe");

		final var result = verifications.findByOwner(owner, SearchQuery.builder()
				.pageable(Pageable.ofSize(10))
				.criteria(GroupVerification.STATE_CRITERIA, VerificationState.ACTIVE)
				.build());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("should find claims by owner filtered by state and search term")
	void shouldFindByOwnerFilteredByStateAndTerm() {
		final var owner = new Owner(EntityId.from(1), "john-doe");

		final var result = verifications.findByOwner(owner, SearchQuery.builder()
				.pageable(Pageable.ofSize(10))
				.term("springframework")
				.criteria(GroupVerification.STATE_CRITERIA, VerificationState.PENDING)
				.build());

		assertThat(result.stream())
				.hasSize(1)
				.extracting(GroupVerification::groupId)
				.containsExactly("org.springframework.boot");
	}

	@Test
	@DisplayName("should find covering by group id")
	void shouldFindCoveringByGroupId() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var groupId = "org.springframework.boot";
		final var result = verifications.findByGroupId(owner, groupId);

		assertThat(result)
				.isPresent()
				.get()
				.returns(EntityId.from(3L), GroupVerification::id)
				.returns(groupId, GroupVerification::groupId)
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.PENDING, GroupVerification::state);
	}

	@Test
	@DisplayName("should find active challenge")
	void shouldFindActiveChallenge() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var groupId = "org.springframework.boot";
		final var groupVerification = GroupVerification.builder()
				.id(EntityId.from(3))
				.owner(owner)
				.groupId(groupId)
				.state(VerificationState.PENDING)
				.build();

		final var result = verifications.findActiveChallenge(groupVerification);
		assertThat(result)
				.isPresent()
				.get()
				.returns(UUID.fromString("018f4c2a-1b3f-7e5f-9a6b-7c8d9e0f1a2d"), VerificationChallenge::id)
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
				.returns(VerificationMethod.DNS, VerificationChallenge::method);
	}

	@Test
	@Transactional
	@DisplayName("should find overlapping active claims for parent group ids")
	void findAnyOverlapping() {
		final var owner = new Owner(EntityId.from(2), "konfigyr");
		final var groupId = "com.konfigyr";
		final var verification = verifications.findActiveCovering(owner, groupId);

		assertThat(verification).isPresent();

		final var result = verifications.findAnyOverlapping("com.konfigyr.utils");

		assertThat(result)
				.isPresent()
				.get()
				.returns(verification.get().id(), GroupVerification::id)
				.returns(VerificationState.ACTIVE, GroupVerification::state);
	}

	@Test
	@Transactional
	@DisplayName("should reject duplicate active claims for the same groupId")
	void shouldRejectDuplicateActiveClaim() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		assertThatThrownBy(() -> verifications.claim(owner, "com.konfigyr", VerificationMethod.DNS))
				.isInstanceOf(GroupIdAlreadyClaimedException.class);
	}

	@Test
	@Transactional
	@DisplayName("should create a new claim, activate and revoke with DNA activation method")
	void shouldCreateActivateAndRevokeClaimWithDns() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var method = VerificationMethod.DNS;
		final var groupId = "com.mycompany";
		final var domain = "mycompany.com";

		final var pendingResult = verifications.claim(owner, groupId, method);
		assertThat(pendingResult)
				.as("should create correct group verification")
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.PENDING, GroupVerification::state)
				.returns(null, GroupVerification::revokedAt)
				.returns(null, GroupVerification::verifiedAt);

		final var activeVerificationChallenge = verifications.findActiveChallenge(pendingResult);
		final var verificationToken = assertThat(activeVerificationChallenge)
				.as("should create correct verification challenge with token")
				.isPresent()
				.get()
				.satisfies(it -> assertThat(it.token()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
				.returns(method, VerificationChallenge::method)
				.actual()
				.token();

		final String txtRecord = "konfigyr-verification=" + verificationToken;

		try (MockedConstruction<InitialDirContext> ignored = mockDns(domain, "some-other-record", txtRecord)) {
			final var activeResult = verifications.verify(owner, groupId);
			assertThat(activeResult)
					.as("should activate group verification")
					.returns(pendingResult.id(), GroupVerification::id)
					.satisfies(it -> assertThat(it.createdAt()).isNotNull())
					.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
					.returns(null, GroupVerification::revokedAt)
					.returns(owner, GroupVerification::owner)
					.returns(VerificationState.ACTIVE, GroupVerification::state);

			final var revokedResult = verifications.revoke(activeResult);
			assertThat(revokedResult)
					.as("should revoke group verification")
					.returns(activeResult.id(), GroupVerification::id)
					.satisfies(it -> assertThat(it.createdAt()).isNotNull())
					.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
					.satisfies(it -> assertThat(it.revokedAt()).isNotNull())
					.returns(owner, GroupVerification::owner)
					.returns(VerificationState.REVOKED, GroupVerification::state);
		}
	}

	@Test
	@Transactional
	@DisplayName("should revoke claim in pending state")
	void shouldRevokeClaimInPendingState() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var method = VerificationMethod.DNS;
		final var groupId = "com.pending-company";

		final var pendingResult = assertThat(verifications.claim(owner, groupId, method))
				.as("should create correct group verification")
				.returns(VerificationState.PENDING, GroupVerification::state)
				.actual();

		final var revokedResult = verifications.revoke(pendingResult);
		assertThat(revokedResult)
				.as("should revoke pending claim")
				.returns(pendingResult.id(), GroupVerification::id)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.revokedAt()).isNotNull())
				.returns(null, GroupVerification::verifiedAt)
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.REVOKED, GroupVerification::state);
	}

	@Test
	@Transactional
	@DisplayName("should fail to revoke claim in revoked state")
	void shouldFailToRevokeClaimInPendingState() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var method = VerificationMethod.DNS;
		final var groupId = "com.revoked-company";

		final var pendingResult = assertThat(verifications.claim(owner, groupId, method))
				.as("should create correct group verification")
				.returns(VerificationState.PENDING, GroupVerification::state)
				.actual();

		final var revokedResult = assertThat(verifications.revoke(pendingResult))
				.as("should revoke pending claim")
				.returns(VerificationState.REVOKED, GroupVerification::state)
				.actual();

		assertThatThrownBy(() -> verifications.revoke(revokedResult))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Can only revoke an active groupId verification, but it was in a '%s' state", VerificationState.REVOKED);
	}

	@Test
	@Transactional
	@DisplayName("should create a new claim, activate and revoke with SOURCE_CODE activation method")
	void shouldCreateActivateAndRevokeClaimWithSourceCode() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var method = VerificationMethod.SOURCE_CODE;
		final var groupId = "io.gitlab.john-doe";

		final var pendingVerification = verifications.claim(owner, groupId, method);
		assertThat(pendingVerification)
				.as("should create correct group verification")
				.satisfies(it -> assertThat(it.id()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.PENDING, GroupVerification::state)
				.returns(null, GroupVerification::revokedAt)
				.returns(null, GroupVerification::verifiedAt);

		final var activeVerificationChallenge = assertThat(verifications.findActiveChallenge(pendingVerification))
				.as("should create correct verification challenge with token")
				.isPresent()
				.get()
				.satisfies(it -> assertThat(it.token()).isNotNull())
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
				.returns(method, VerificationChallenge::method)
				.actual();

		doReturn(VerificationResult.success(VerificationMethod.SOURCE_CODE))
				.when(sourceCodeVerificationStrategy)
				.verify(pendingVerification, activeVerificationChallenge);

		final var activeResult = verifications.verify(owner, groupId);
		assertThat(activeResult)
				.as("should activate group verification")
				.returns(pendingVerification.id(), GroupVerification::id)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
				.returns(null, GroupVerification::revokedAt)
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.ACTIVE, GroupVerification::state);

		final var revokedResult = verifications.revoke(activeResult);
		assertThat(revokedResult)
				.as("should revoke group verification")
				.returns(activeResult.id(), GroupVerification::id)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull())
				.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
				.satisfies(it -> assertThat(it.revokedAt()).isNotNull())
				.returns(owner, GroupVerification::owner)
				.returns(VerificationState.REVOKED, GroupVerification::state);
	}

	@Test
	@Transactional
	@DisplayName("should fail to execute the DNS verification challenge")
	void shouldFailToVerifyDnsVerificationChallenge() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var groupId = "com.my-third-company";

		final var pendingVerification = assertThat(verifications.claim(owner, groupId, VerificationMethod.DNS))
				.isNotNull()
				.actual();

		assertThat(verifications.findChallenges(pendingVerification))
				.hasSize(1)
				.first()
				.returns(VerificationMethod.DNS, VerificationChallenge::method)
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state);

		try (MockedConstruction<InitialDirContext> ignored = mockDnsException(new CommunicationException("DNS timed out"))) {
			assertThat(verifications.verify(owner, groupId))
					.returns(pendingVerification.id(), GroupVerification::id)
					.returns(null, GroupVerification::verifiedAt)
					.returns(VerificationState.PENDING, GroupVerification::state);

			assertThat(verifications.findChallenges(pendingVerification))
					.hasSize(1)
					.first()
					.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state);
		}
	}

	@Test
	@Transactional
	@DisplayName("should fail to execute the SOURCE_COSE verification challenge")
	void shouldFailToVerifySourceCodeVerificationChallenge() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		final var groupId = "io.gitlab.john-doe";

		final var pendingVerification = assertThat(verifications.claim(owner, groupId, VerificationMethod.SOURCE_CODE))
				.isNotNull()
				.actual();

		final var activeVerificationChallenge = assertThat(verifications.findChallenges(pendingVerification))
				.hasSize(1)
				.first()
				.returns(VerificationMethod.SOURCE_CODE, VerificationChallenge::method)
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
				.actual();

		doReturn(VerificationResult.failure(VerificationResult.FailureReason.SERVICE_UNAVAILABLE))
				.when(sourceCodeVerificationStrategy)
				.verify(pendingVerification, activeVerificationChallenge);

		assertThat(verifications.verify(owner, groupId))
				.returns(pendingVerification.id(), GroupVerification::id)
				.returns(null, GroupVerification::verifiedAt)
				.returns(VerificationState.PENDING, GroupVerification::state);

		assertThat(verifications.findChallenges(pendingVerification))
				.hasSize(1)
				.first()
				.returns(ChallengeState.UNVERIFIED, VerificationChallenge::state);
	}

	@Test
	@Transactional
	@DisplayName("should allow only one verification challenge in unverified state")
	void shouldAllowOnlyOneActiveChallenge() {
		final var owner = new Owner(EntityId.from(1), "john-doe");
		assertThat(verifications.claim(owner, "com.my-new-company", VerificationMethod.DNS))
				.isNotNull();
		assertThatThrownBy(() -> verifications.claim(owner, "com.my-new-company", VerificationMethod.DNS))
				.isInstanceOf(DuplicateKeyException.class);
	}

}
