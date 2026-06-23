package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.CommunicationException;
import javax.naming.directory.InitialDirContext;

import static com.konfigyr.artifactory.ownership.VerificationResult.FailureReason.SERVICE_UNAVAILABLE;
import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.mockDns;
import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.mockDnsException;
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
        assertThatThrownBy(() -> verifications.claim(owner, "com.konfigyr", VerificationMethod.DNS))
                .isInstanceOf(GroupIdAlreadyClaimedException.class);
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
    @DisplayName("should successfully execute the DNS verification challenge")
    void shouldSuccessfullyVerifyDnsVerificationChallenge() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupId = "com.my-second-company";
        final var domain = "my-second-company.com";
        final var pendingVerification = assertThat(verifications.claim(owner, groupId, VerificationMethod.DNS))
                .isNotNull()
                .actual();

        final var unverifiedChallenge = assertThat(verifications.findChallenges(pendingVerification.id(), owner))
                .hasSize(1)
                .first()
                .returns(VerificationMethod.DNS, VerificationChallenge::method)
                .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state)
                .actual();

        final String txtRecord = "konfigyr-verification=" + unverifiedChallenge.token();
        try (MockedConstruction<InitialDirContext> ignored = mockDns(domain, "some-other-record", txtRecord)) {
            final var activeVerification = assertThat(verifications.verify(owner, groupId))
                    .returns(pendingVerification.id(), GroupVerification::id)
                    .satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
                    .returns(VerificationState.ACTIVE, GroupVerification::state)
                    .actual();

            assertThat(verifications.findChallenges(activeVerification.id(), owner))
                    .hasSize(1)
                    .first()
                    .returns(unverifiedChallenge.id(), VerificationChallenge::id)
                    .returns(activeVerification.id(), VerificationChallenge::verificationId)
                    .returns(ChallengeState.VERIFIED, VerificationChallenge::state)
                    .satisfies(it -> assertThat(it.verifiedAt()).isNotNull());
        }
    }

    @Test
    @Transactional
    @DisplayName("should fail to execute the DNS verification challenge.")
    void shouldFailToVerifyDnsVerificationChallenge() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        final var groupId = "com.my-third-company";

        final var pendingVerification = assertThat(verifications.claim(owner, groupId, VerificationMethod.DNS))
                .isNotNull()
                .actual();

        assertThat(verifications.findChallenges(pendingVerification.id(), owner))
                .hasSize(1)
                .first()
                .returns(VerificationMethod.DNS, VerificationChallenge::method)
                .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state);

        try (MockedConstruction<InitialDirContext> ignored = mockDnsException(new CommunicationException("DNS timed out"))) {
            assertThat(verifications.verify(owner, groupId))
                    .returns(pendingVerification.id(), GroupVerification::id)
                    .returns(null, GroupVerification::verifiedAt)
                    .returns(VerificationState.PENDING, GroupVerification::state);

            assertThat(verifications.findChallenges(pendingVerification.id(), owner))
                    .hasSize(1)
                    .first()
                    .returns(ChallengeState.UNVERIFIED, VerificationChallenge::state);
        }
    }

    @Test
    @Transactional
    @DisplayName("should allow only one verification challenge in unverified state")
    void shouldAllowOnlyOneActiveChallenge() {
        final var owner = Owner.of(EntityId.from(1), "john-doe");
        assertThat(verifications.claim(owner, "com.my-new-company", VerificationMethod.DNS))
                .isNotNull();
        assertThatThrownBy(() -> verifications.claim(owner, "com.my-new-company", VerificationMethod.DNS))
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
