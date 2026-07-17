package com.konfigyr.artifactory.controller;

import com.konfigyr.artifactory.OwnerNotFoundException;
import com.konfigyr.artifactory.ownership.GroupIdNotVerifiedException;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransfer;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransferAlreadyResolvedException;
import com.konfigyr.artifactory.transfer.ArtifactOwnershipTransferNotFoundException;
import com.konfigyr.artifactory.transfer.TransferState;
import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

/**
 * Tests reuse the pre-seeded {@code artifact-ownership-transfers.sql} fixture wherever possible:
 * transfer {@code 1} (PENDING, from=konfigyr, to=ebf) backs the accept/reject/cancel scenarios, and
 * transfer {@code 2} (ACCEPTED, from=john-doe, to=konfigyr) backs the already-resolved conflict case.
 * Only the "request" endpoint itself needs to create new data, since that's the behavior under test.
 */
class ArtifactOwnershipTransfersControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should fail to list transfers for an unknown namespace")
	void shouldFailFindOwner() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers?direction=INCOMING", "fake-namespace")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Organization not found")
						.hasDetailContaining("The namespace you're trying to access doesn't exist")
				).andThen(hasFailedWithException(OwnerNotFoundException.class, ex -> ex
						.hasMessageContaining("Could not find an owner with the following name: fake-namespace"))));
	}

	@Test
	@DisplayName("should list incoming transfers for a namespace")
	void shouldListIncomingTransfers() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers?direction=INCOMING", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ArtifactOwnershipTransfer.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ArtifactOwnershipTransfer::id)
						.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(3)));
	}

	@Test
	@DisplayName("should list outgoing transfers for a namespace")
	void shouldListOutgoingTransfers() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers?direction=OUTGOING", "ebf")
				.with(authentication(TestPrincipals.max(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ArtifactOwnershipTransfer.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ArtifactOwnershipTransfer::id)
						.containsExactlyInAnyOrder(EntityId.from(1), EntityId.from(4)));
	}

	@Test
	@DisplayName("should list transfers filtered by search term")
	void shouldListTransfersFilteredByTerm() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers?direction=INCOMING&term=billing", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(pagedModel(ArtifactOwnershipTransfer.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ArtifactOwnershipTransfer::id)
						.containsExactly(EntityId.from(1)));
	}

	@Test
	@DisplayName("should fail to list transfers without direction query parameter")
	void shouldFailListingWithoutDirection() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("Bad Request")
						.hasDetailContaining("Required parameter 'direction' is not present")));
	}

	@Test
	@DisplayName("should fetch a transfer visible to either party")
	void shouldFetchTransferById() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers/{id}", "konfigyr", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ArtifactOwnershipTransfer.class)
				.returns(EntityId.from(2), ArtifactOwnershipTransfer::id)
				.returns(TransferState.ACCEPTED, ArtifactOwnershipTransfer::state);
	}

	@Test
	@DisplayName("should return 404 fetching a transfer not visible to the requesting namespace")
	void shouldFailFetchingTransferForUninvolvedNamespace() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers/{id}", "ebf", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.max(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Ownership transfer not found")
						.hasDetailContaining("visible to the 'ebf' namespace")
				).andThen(hasFailedWithException(ArtifactOwnershipTransferNotFoundException.class)));
	}

	@Test
	@DisplayName("should return 404 fetching an unknown transfer")
	void shouldFailFetchingUnknownTransfer() {
		mvc.get().uri("/namespaces/{namespace}/artifact-transfers/{id}", "konfigyr", EntityId.from(999_999).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
						.hasTitle("Ownership transfer not found")
						.hasDetailContaining("visible to the 'konfigyr' namespace")
				).andThen(hasFailedWithException(ArtifactOwnershipTransferNotFoundException.class)));
	}

	@Test
	@Transactional
	@DisplayName("should request a new transfer")
	void shouldRequestTransfer() {
		request("konfigyr", "ebf", "com.konfigyr");
	}

	@Test
	@DisplayName("should fail to request a transfer for a non-admin of the requesting namespace")
	void shouldFailRequestForNonAdmin() {
		mvc.post().uri("/namespaces/{namespace}/artifact-transfers", "konfigyr")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupId\":\"com.konfigyr\",\"fromNamespace\":\"ebf\"}")
				.with(authentication(TestPrincipals.max(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(problem -> problem
						.hasTitle("Access Denied")
						.hasDetailContaining("It looks like you do not have the necessary roles or permissions")));
	}

	@Test
	@DisplayName("should fail to request a transfer when the requester has no active claim on the groupId")
	void shouldFailRequestWithoutActiveClaim() {
		mvc.post().uri("/namespaces/{namespace}/artifact-transfers", "ebf")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupId\":\"com.konfigyr\",\"fromNamespace\":\"john-doe\"}")
				.with(authentication(TestPrincipals.max(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitle("GroupId is not verified")
						.hasDetailContaining("does not hold an active verification claim")
				).andThen(hasFailedWithException(GroupIdNotVerifiedException.class)));
	}

	@Test
	@Transactional
	@DisplayName("should accept a pending transfer as the current owner")
	void shouldAcceptTransfer() {
		mvc.post().uri("/namespaces/{namespace}/artifact-transfers/{id}/accept", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ArtifactOwnershipTransfer.class)
				.returns(EntityId.from(1), ArtifactOwnershipTransfer::id)
				.returns(TransferState.ACCEPTED, ArtifactOwnershipTransfer::state);
	}

	@Test
	@DisplayName("should fail to accept a transfer as the requesting namespace instead of the current owner")
	void shouldFailAcceptForWrongParty() {
		mvc.post().uri("/namespaces/{namespace}/artifact-transfers/{id}/accept", "ebf", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.max(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(problem -> problem
						.hasTitle("Access Denied")
						.hasDetailContaining("It looks like you do not have the necessary permissions")));
	}

	@Test
	@DisplayName("should fail to accept a transfer that was already resolved")
	void shouldFailAcceptingAlreadyResolvedTransfer() {
		mvc.post().uri("/namespaces/{namespace}/artifact-transfers/{id}/accept", "john-doe", EntityId.from(2).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.CONFLICT, problem -> problem
						.hasTitle("Transfer already resolved")
						.hasDetailContaining("has already been resolved with a 'ACCEPTED' status")
				).andThen(hasFailedWithException(ArtifactOwnershipTransferAlreadyResolvedException.class, ex -> ex
						.returns(EntityId.from(2), ArtifactOwnershipTransferAlreadyResolvedException::getId)
						.returns(TransferState.ACCEPTED, ArtifactOwnershipTransferAlreadyResolvedException::getState))));
	}

	@Test
	@Transactional
	@DisplayName("should reject a pending transfer as the current owner")
	void shouldRejectTransfer() {
		mvc.post().uri("/namespaces/{namespace}/artifact-transfers/{id}/reject", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ArtifactOwnershipTransfer.class)
				.returns(TransferState.REJECTED, ArtifactOwnershipTransfer::state);
	}

	@Test
	@Transactional
	@DisplayName("should cancel a pending transfer as the requesting namespace")
	void shouldCancelTransfer() {
		mvc.delete().uri("/namespaces/{namespace}/artifact-transfers/{id}", "ebf", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.max(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ArtifactOwnershipTransfer.class)
				.returns(TransferState.CANCELLED, ArtifactOwnershipTransfer::state);
	}

	@Test
	@DisplayName("should fail to cancel a transfer as the current owner instead of the requesting namespace")
	void shouldFailCancelForWrongParty() {
		mvc.delete().uri("/namespaces/{namespace}/artifact-transfers/{id}", "konfigyr", EntityId.from(1).serialize())
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(problem -> problem
						.hasTitle("Access Denied")
						.hasDetailContaining("It looks like you do not have the necessary permissions")));
	}

	private ArtifactOwnershipTransfer request(String to, String from, String groupId) {
		return mvc.post().uri("/namespaces/{namespace}/artifact-transfers", to)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"groupId\":\"" + groupId + "\",\"fromNamespace\":\"" + from + "\"}")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.convertTo(ArtifactOwnershipTransfer.class)
				.returns(groupId, ArtifactOwnershipTransfer::groupId)
				.returns(TransferState.PENDING, ArtifactOwnershipTransfer::state)
				.actual();
	}

}
