package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.directory.InitialDirContext;

import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.mockDns;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGroupVerificationsTest extends AbstractIntegrationTest {

    @Autowired
    GroupVerifications verifications;

    @Test
    @DisplayName("should find active covering by group id and owner")
    void shouldFundActiveCoveringByIdAndOwner() {
        final var owner = Owner.of(EntityId.from(2), "konfigyr");
        final var groupId = "com.konfigyr";
        final var result = verifications.findActiveCovering(groupId, owner);

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
        final var owner = Owner.of(EntityId.from(2), "konfigyr");
        final var groupId = "com.config";
        final var result = verifications.findActiveCovering(groupId, owner);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find covering by owner")
    void shouldFindCoveringByOwner() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var result = verifications.findByOwner(owner);

        assertThat(result.stream())
                .hasSize(2)
                .extracting(GroupVerification::id)
                .contains(EntityId.from(2), EntityId.from(3));
    }

    @Test
    @DisplayName("should find covering by group id")
    void shouldFindCoveringByGroupId() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupId = "org.springframework.boot";
        final var result = verifications.findByGroupId(groupId, owner);

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
        final var owner = Owner.of(EntityId.from(1), "john-doe");
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
                .returns(EntityId.from(3L), VerificationChallenge::id)
                .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
                .returns(VerificationMethod.DNS, VerificationChallenge::method);
    }

    @Test
    @Transactional
    @DisplayName("should find overlapping active claims for parent group ids")
    void findAnyOverlapping() {
        final var owner = Owner.of(EntityId.from(2), "konfigyr");
        final var groupId = "com.konfigyr";
        final var verification = verifications.findActiveCovering(groupId, owner);

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
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupVerification = GroupVerification.claim(owner, "com.konfigyr").activate();
        assertThatThrownBy(() -> verifications.save(groupVerification)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    @DisplayName("should create a new claim, activate and revoke it")
    void shouldCreateActivateAndRevokeClaim() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
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

            final var revokedResult = verifications.save(activeResult.revoke());
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
    @DisplayName("should successfully execute the DNS verification challenge")
    void shouldSuccessfullyVerifyDnsVerificationChallenge() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupVerification = GroupVerification.claim(owner, "com.my-second-company");

        final var pendingResult = verifications.save(groupVerification);
        assertThat(pendingResult).isNotNull();

        final var challenge = VerificationChallenge.issue(VerificationMethod.DNS)
                .toBuilder()
                .verificationId(pendingResult.id())
                .build();

        final var unverifiedChallenge = verifications.saveChallenge(challenge);
        assertThat(unverifiedChallenge)
                .satisfies(it -> assertThat(it.id()).isNotNull())
                .satisfies(it -> assertThat(it.createdAt()).isNotNull())
                .satisfies(it -> assertThat(it.token()).isNotNull())
                .returns(pendingResult.id(), VerificationChallenge::verificationId)
                .returns(VerificationMethod.DNS, VerificationChallenge::method)
                .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
                .returns(null, VerificationChallenge::verifiedAt)
                .returns(null, VerificationChallenge::expiresAt);

        final var verifiedChallenge = verifications.saveChallenge(
                unverifiedChallenge.applyResult(VerificationResult.success(VerificationMethod.DNS))
        );

        assertThat(verifiedChallenge)
                .returns(unverifiedChallenge.id(), VerificationChallenge::id)
                .satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
                .returns(VerificationMethod.DNS, VerificationChallenge::method)
                .returns(ChallengeState.VERIFIED, VerificationChallenge::state)
                .returns(null, VerificationChallenge::expiresAt);
    }

    @Test
    @Transactional
    @DisplayName("should fail to execute the DNS verification challenge.")
    void shouldFailToVerifyDnsVerificationChallenge() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupVerification = GroupVerification.claim(owner, "com.my-third-company");

        final var pendingResult = verifications.save(groupVerification);
        assertThat(pendingResult).isNotNull();

        final var challenge = VerificationChallenge.issue(VerificationMethod.DNS)
                .toBuilder()
                .verificationId(pendingResult.id())
                .build();

        final var unverifiedChallenge = verifications.saveChallenge(challenge);
        assertThat(unverifiedChallenge)
                .satisfies(it -> assertThat(it.id()).isNotNull())
                .satisfies(it -> assertThat(it.createdAt()).isNotNull())
                .satisfies(it -> assertThat(it.token()).isNotNull())
                .returns(pendingResult.id(), VerificationChallenge::verificationId)
                .returns(VerificationMethod.DNS, VerificationChallenge::method)
                .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
                .returns(null, VerificationChallenge::verifiedAt)
                .returns(null, VerificationChallenge::expiresAt);

        final var failedChallenge = verifications.saveChallenge(
                unverifiedChallenge.applyResult(VerificationResult.failure("Cannot validate record"))
        );

        assertThat(failedChallenge)
                .returns(unverifiedChallenge.id(), VerificationChallenge::id)
                .returns(null, VerificationChallenge::verifiedAt)
                .returns(VerificationMethod.DNS, VerificationChallenge::method)
                .returns(ChallengeState.EXPIRED, VerificationChallenge::state)
                .returns(null, VerificationChallenge::expiresAt);
    }

    @Test
    @Transactional
    @DisplayName("should allow only one verification challenge in unverified state")
    void shouldAllowOnlyOneActiveChallenge() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupVerification = GroupVerification.claim(owner, "com.my-new-company");

        final var pendingResult = verifications.save(groupVerification);
        assertThat(pendingResult).isNotNull();

        final var challenge = VerificationChallenge.issue(VerificationMethod.DNS)
                .toBuilder()
                .verificationId(pendingResult.id())
                .build();

        final var unverifiedChallenge = verifications.saveChallenge(challenge);
        assertThat(unverifiedChallenge)
                .satisfies(it -> assertThat(it.id()).isNotNull())
                .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state);

        assertThatThrownBy(() -> verifications.saveChallenge(challenge))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("should find group owner for the given namespace slug")
    void shouldFindGroupOwner() {
        final var result = verifications.findOwner("konfigyr");

        assertThat(result)
                .isPresent()
                .get()
                .returns(EntityId.from(2L), Owner::id)
                .returns("konfigyr", Owner::slug);
    }
}
