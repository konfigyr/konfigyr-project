package com.konfigyr.artifactory.ownership.controller;

import com.konfigyr.artifactory.ownership.ChallengeState;
import com.konfigyr.artifactory.ownership.GroupIdAlreadyClaimedException;
import com.konfigyr.artifactory.ownership.GroupVerification;
import com.konfigyr.artifactory.ownership.OwnerNotFoundException;
import com.konfigyr.artifactory.ownership.VerificationChallenge;
import com.konfigyr.artifactory.ownership.VerificationMethod;
import com.konfigyr.artifactory.ownership.VerificationState;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.directory.InitialDirContext;

import static com.konfigyr.artifactory.ownership.VerificationStrategyTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class GroupVerificationsControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should fail to list claims for an unknown namespace")
	@Transactional
	void shouldFailFindOwner() {
		mvc.get().uri("/namespaces/{namespace}/group-verifications", "fake-namespace")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.NOT_FOUND)
				.satisfies(hasFailedWithException(OwnerNotFoundException.class, ex -> ex
						.hasMessageContaining("Could not find an owner with the following name: fake-namespace")));
	}

	@Test
	@DisplayName("should list claims for a namespace")
	@Transactional
	void shouldListClaimsForNamespace() {
		mvc.get().uri("/namespaces/{namespace}/group-verifications", "john-doe")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(collectionModel(GroupVerification.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(GroupVerification::groupId, GroupVerification::state)
						.containsExactlyInAnyOrder(
								tuple("org.springframework.ai", VerificationState.FAILED),
								tuple("org.springframework.boot", VerificationState.PENDING)
						));
	}

	@Test
	@DisplayName("should submit a new verification claim")
	@Transactional
	void shouldSubmitNewVerificationClaim() {
		submit("konfigyr", "com.company.claim");
	}

	@Test
	@DisplayName("should reject duplicate active claims for the same groupId")
	void shouldRejectDuplicateActiveClaimsForTheSameGroupId() {
		mvc.post().uri("/namespaces/{namespace}/group-verifications", "john-doe")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupId\":\"com.konfigyr\",\"verificationMethod\":\"DNS\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(hasFailedWithException(GroupIdAlreadyClaimedException.class));
	}

	@Test
	@DisplayName("should fetch claim by group id")
	@Transactional
	void shouldFetchClaimByGroupId() {
		mvc.get().uri("/namespaces/{namespace}/group-verifications/{groupId}", "john-doe", "org.springframework.ai")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(GroupVerification.class)
				.returns("org.springframework.ai", GroupVerification::groupId)
				.returns(VerificationState.FAILED, GroupVerification::state);
	}

	@Test
	@DisplayName("should retrieve verification challenge history for the given claim")
	@Transactional
	void shouldRetrieveVerificationChallengeHistory() {
		final EntityId verificationId = EntityId.from(2);
		mvc.get().uri("/namespaces/{namespace}/group-verifications/{verificationId}/verification-challenges", "john-doe", verificationId.serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(collectionModel(VerificationChallenge.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(1)
						.extracting(VerificationChallenge::verificationId, VerificationChallenge::method, VerificationChallenge::state)
						.containsExactlyInAnyOrder(
								tuple(verificationId, VerificationMethod.DNS, ChallengeState.UNVERIFIED)
						));
	}

	@Test
	@DisplayName("should verify a pending claim with DNS activation method and activate it")
	@Transactional
	void shouldVerifyPendingClaim() {
		final String namespase = "john-doe";
		final String domain = "springframework.org";
		final String groupId = "org.springframework.boot";
		final String txtRecord = "konfigyr-verification=dns-pending-token-1";

		try (MockedConstruction<InitialDirContext> ignored = mockDns(domain, "some-other-record", txtRecord)) {

			final GroupVerification verification =  mvc.post().uri("/namespaces/{namespace}/group-verifications/{groupId}/verify", namespase, groupId)
					.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
					.exchange()
					.assertThat()
					.apply(log())
					.hasStatusOk()
					.bodyJson()
					.convertTo(GroupVerification.class)
					.returns(groupId, GroupVerification::groupId)
					.returns(VerificationState.ACTIVE, GroupVerification::state)
					.satisfies(it -> assertThat(it.createdAt()).isNotNull())
					.satisfies(it -> assertThat(it.verifiedAt()).isNotNull())
					.returns(null, GroupVerification::revokedAt)
					.actual();

			mvc.get().uri("/namespaces/{namespace}/group-verifications/{verificationId}/verification-challenges", namespase, verification.id().serialize())
					.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
					.exchange()
					.assertThat()
					.apply(log())
					.hasStatusOk()
					.bodyJson()
					.convertTo(collectionModel(VerificationChallenge.class))
					.satisfies(it -> assertThat(it.getContent())
							.hasSize(1)
							.extracting(VerificationChallenge::verificationId, VerificationChallenge::method, VerificationChallenge::state)
							.containsExactlyInAnyOrder(
									tuple(verification.id(), VerificationMethod.DNS, ChallengeState.VERIFIED)
							));
		}
	}

	@Test
	@DisplayName("should fail to verify a pending claim with DNS activation method")
	@Transactional
	void shouldFailToVerifyPendingClaim() {
		final String namespase = "konfigyr";
		final String domain = "company.com";
		final String groupId = "com.company.faild";
		submit(namespase, groupId);

		try (MockedConstruction<InitialDirContext> ignored = mockDnsNoTxt(domain)) {
			final GroupVerification verification =  mvc.post().uri("/namespaces/{namespace}/group-verifications/{groupId}/verify", namespase, groupId)
					.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
					.exchange()
					.assertThat()
					.apply(log())
					.hasStatusOk()
					.bodyJson()
					.convertTo(GroupVerification.class)
					.returns(groupId, GroupVerification::groupId)
					.returns(VerificationState.PENDING, GroupVerification::state)
					.satisfies(it -> assertThat(it.createdAt()).isNotNull())
					.satisfies(it -> assertThat(it.id()).isNotNull())
					.returns(null, GroupVerification::revokedAt)
					.actual();

			mvc.get().uri("/namespaces/{namespace}/group-verifications/{verificationId}/verification-challenges", namespase, verification.id().serialize())
					.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
					.exchange()
					.assertThat()
					.apply(log())
					.hasStatusOk()
					.bodyJson()
					.convertTo(collectionModel(VerificationChallenge.class))
					.satisfies(it -> assertThat(it.getContent())
							.hasSize(1)
							.extracting(VerificationChallenge::verificationId, VerificationChallenge::method, VerificationChallenge::state)
							.containsExactlyInAnyOrder(
									tuple(verification.id(), VerificationMethod.DNS, ChallengeState.EXPIRED)
							));
		}
	}

	private void submit(String namespace, String groupId) {
		mvc.post().uri("/namespaces/{namespace}/group-verifications", namespace)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupId\":\"" + groupId + "\",\"verificationMethod\":\"DNS\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.convertTo(GroupVerification.class)
				.returns(groupId, GroupVerification::groupId)
				.returns(VerificationState.PENDING, GroupVerification::state)
				.satisfies(it -> assertThat(it.createdAt()).isNotNull());
	}
}
