package com.konfigyr.vault.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CollectionModel;
import com.konfigyr.hateoas.PagedModel;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import com.konfigyr.vault.changes.ChangeRequestReviewCommand;
import com.konfigyr.vault.state.StateRepository;
import com.konfigyr.vault.state.StateRepositoryFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class VaultChangeRequestControllerTest extends AbstractControllerTest {

	@Autowired
	StateRepositoryFactory stateRepositoryFactory;

	@Autowired
	Services services;

	Service service;
	StateRepository repository;

	@BeforeEach
	void setup() {
		service = services.get(EntityId.from(2)).orElseThrow();
	}

	@AfterEach
	void cleanup() throws Exception {
		if (repository != null) {
			repository.destroy();
			repository.close();
		}
	}

	@Test
	@DisplayName("should search for change requests")
	void searchChangeRequest() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "konfigyr", service.slug())
				.param("term", "server port")
				.param("profile", "locked")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(ChangeRequest.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ChangeRequest::id)
						.containsExactly(EntityId.from(2))
				)
				.extracting(PagedModel::getMetadata)
				.isNotNull()
				.returns(20L, PagedModel.PageMetadata::size)
				.returns(1L, PagedModel.PageMetadata::number)
				.returns(1L, PagedModel.PageMetadata::totalElements)
				.returns(1L, PagedModel.PageMetadata::totalPages);
	}

	@Test
	@DisplayName("should paginate search for change requests")
	void paginateChangeRequestSearch() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "konfigyr", service.slug())
				.param("page", "2")
				.param("size", "2")
				.param("sort", "updated,desc")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(pagedModel(ChangeRequest.class))
				.satisfies(it -> assertThat(it.getContent())
						.extracting(ChangeRequest::id)
						.containsExactly(EntityId.from(2), EntityId.from(4))
				)
				.extracting(PagedModel::getMetadata)
				.isNotNull()
				.returns(2L, PagedModel.PageMetadata::size)
				.returns(2L, PagedModel.PageMetadata::number)
				.returns(5L, PagedModel.PageMetadata::totalElements)
				.returns(3L, PagedModel.PageMetadata::totalPages);
	}

	@Test
	@DisplayName("should fail to search change requests for an unknown service")
	void searchRequestsForUnknownService() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "konfigyr", "unknown-service")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(serviceNotFound("unknown-service"));
	}

	@Test
	@DisplayName("should fail to search change requests for an unknown namespace")
	void searchRequestsForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "unknown-namespace", service.slug())
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown-namespace"));
	}

	@Test
	@DisplayName("should fail to search change requests when user is not a member of a namespace")
	void searchRequestsWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "john-doe", "john-doe-blog")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to search change requests without required scope")
	void searchRequestsWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "john-doe", "john-doe-blog")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_PROFILES));
	}

	@Test
	@DisplayName("should retrieve change request for a service and number")
	void retrieveChangeRequest() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(ChangeRequest.class)
				.returns(EntityId.from(2), ChangeRequest::id)
				.returns(2L, ChangeRequest::number)
				.returns(ChangeRequestState.OPEN, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.MERGEABLE, ChangeRequest::mergeStatus)
				.returns(2, ChangeRequest::count)
				.returns("Increase server port", ChangeRequest::subject)
				.returns("Move service to new port range", ChangeRequest::description)
				.returns("John Doe", ChangeRequest::createdBy);
	}

	@Test
	@DisplayName("should retrieve change request for a service and an unknown number")
	void retrieveUnknownChangeRequest() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "99999")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("99999"));
	}

	@Test
	@DisplayName("should fail to retrieve change request when user is not a member of a namespace")
	void retrieveRequestsWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to retrieve change request without required scope")
	void retrieveRequestsWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_PROFILES));
	}

	@Test
	@DisplayName("should retrieve change request history for a service and number")
	void retrieveHistoryForChangeRequest() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/history", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(collectionModel(ChangeRequestHistory.class));
	}

	@Test
	@DisplayName("should retrieve change request history for a service and an unknown number")
	void retrieveHistoryForUnknownChangeRequest() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/history", "konfigyr", service.slug(), "100000")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("100000"));
	}

	@Test
	@DisplayName("should fail to retrieve change request history when user is not a member of a namespace")
	void retrieveRequestHistoryWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/history", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to retrieve change request history without required scope")
	void retrieveRequestHistoryWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/history", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_PROFILES));
	}

	@Test
	@DisplayName("should retrieve change request changes for a service and number")
	void retrieveChangesForChangeRequest() {
		repository = stateRepositoryFactory.create(service);

		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/changes", "konfigyr", service.slug(), "5")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(collectionModel(VaultChangeRequestController.ChangeRequestChange.class))
				.extracting(CollectionModel::getContent)
				.returns(true, Collection::isEmpty);
	}

	@Test
	@DisplayName("should retrieve change request changes for a service and an unknown number")
	void retrieveChangesForUnknownChangeRequest() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/changes", "konfigyr", service.slug(), "9999")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("9999"));
	}

	@Test
	@DisplayName("should fail to retrieve change request changes when user is not a member of a namespace")
	void retrieveRequestChangesWithoutMembership() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/changes", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to retrieve change request changes without required scope")
	void retrieveRequestChangesWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/services/{service}/changes/{number}/changes", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_PROFILES));
	}

	@Transactional
	@EnumSource(ChangeRequestReviewCommand.Operation.class)
	@DisplayName("should submit a review for a change request")
	@ParameterizedTest(name = "submitting review with {0} operation")
	void reviewChangeRequest(ChangeRequestReviewCommand.Operation operation) {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/review", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"state\":\"" + operation + "\",\"comment\": \"Submitting review: " + operation + "\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ChangeRequest.class)
				.returns(EntityId.from(5), ChangeRequest::id)
				.returns(1L, ChangeRequest::number)
				.returns(ChangeRequestState.DISCARDED, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.NOT_OPEN, ChangeRequest::mergeStatus)
				.returns(1, ChangeRequest::count)
				.returns("Experimental feature toggle", ChangeRequest::subject)
				.returns("Testing feature toggle rollout", ChangeRequest::description)
				.returns("Jane Doe", ChangeRequest::createdBy);
	}

	@Test
	@DisplayName("should fail to review change request with invalid payload")
	void reviewChangeRequestWithInvalidPayload() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/review", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(problemDetailFor(HttpStatus.BAD_REQUEST, problem -> problem
						.hasTitleContaining("Invalid")
						.hasDetailContaining("invalid request data")
						.hasPropertySatisfying("errors", errors -> assertThat(errors)
								.isNotNull()
								.isInstanceOf(Collection.class)
								.asInstanceOf(InstanceOfAssertFactories.collection(Map.class))
								.extracting("pointer")
								.containsExactlyInAnyOrder("state")
						)
				));
	}

	@Test
	@DisplayName("should fail to review unknown change request")
	void reviewUnknownChangeRequest() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/review", "konfigyr", service.slug(), "9999999")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"state\":\"APPROVE\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("9999999"));
	}

	@Test
	@DisplayName("should fail to review change request when user is not a member of a namespace")
	void reviewChangeRequestWithoutMembership() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/review", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"state\":\"APPROVE\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to review change request without required scope")
	void reviewChangeRequestWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/review", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"state\":\"APPROVE\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_PROFILES));
	}

	@Test
	@Transactional
	@DisplayName("should update change request subject and description")
	void updateChangeRequest() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/changes/{number}", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"subject\":\"Updated subject\",\"description\":\"Updated change request description\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ChangeRequest.class)
				.returns(EntityId.from(5), ChangeRequest::id)
				.returns(1L, ChangeRequest::number)
				.returns(ChangeRequestState.DISCARDED, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.NOT_OPEN, ChangeRequest::mergeStatus)
				.returns(1, ChangeRequest::count)
				.returns("Updated subject", ChangeRequest::subject)
				.returns("Updated change request description", ChangeRequest::description);
	}

	@Test
	@Transactional
	@DisplayName("should update change request with empty payload")
	void updateChangeRequestWithEmptyPayload() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/changes/{number}", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ChangeRequest.class)
				.returns(EntityId.from(5), ChangeRequest::id)
				.returns(1L, ChangeRequest::number)
				.returns(ChangeRequestState.DISCARDED, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.NOT_OPEN, ChangeRequest::mergeStatus)
				.returns(1, ChangeRequest::count)
				.returns("Experimental feature toggle", ChangeRequest::subject)
				.returns("Testing feature toggle rollout", ChangeRequest::description);
	}

	@Test
	@DisplayName("should fail to review unknown change request")
	void updateUnknownChangeRequest() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "99999")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"Unknown change request...\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("99999"));
	}

	@Test
	@DisplayName("should fail to update change request when user is not a member of a namespace")
	void updateChangeRequestWithoutMembership() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/changes/{number}", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"subject\":\"Insufficient access...\"}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to update change request without required scope")
	void updateChangeRequestWithoutScope() {
		mvc.put().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_PROFILES));
	}

	@Test
	@DisplayName("should fail to merge unknown change request")
	void mergeUnknownChangeRequest() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/merge", "konfigyr", service.slug(), "99999")
				.with(authentication(TestPrincipals.john(), OAuthScope.WRITE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("99999"));
	}

	@Test
	@DisplayName("should fail to merge change request when user is not a member of a namespace")
	void mergeChangeRequestWithoutMembership() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/merge", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.WRITE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to merge change request without required scope")
	void mergeChangeRequestWithoutScope() {
		mvc.post().uri("/namespaces/{slug}/services/{service}/changes/{number}/merge", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.WRITE_PROFILES));
	}

	@Test
	@Transactional
	@DisplayName("should discard change request")
	void discardChangeRequest() {
		repository = stateRepositoryFactory.create(service);

		mvc.delete().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(ChangeRequest.class)
				.returns(EntityId.from(2), ChangeRequest::id)
				.returns(2L, ChangeRequest::number)
				.returns(ChangeRequestState.DISCARDED, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.MERGEABLE, ChangeRequest::mergeStatus)
				.returns(2, ChangeRequest::count)
				.returns("Increase server port", ChangeRequest::subject)
				.returns("Move service to new port range", ChangeRequest::description);
	}

	@Test
	@DisplayName("should fail to discard unknown change request")
	void discardUnknownChangeRequest() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "99999")
				.with(authentication(TestPrincipals.john(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(changeRequestNotFound("99999"));
	}

	@Test
	@DisplayName("should fail to discard change request when user is not a member of a namespace")
	void discardChangeRequestWithoutMembership() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/changes/{number}", "john-doe", "john-doe-blog", "1")
				.with(authentication(TestPrincipals.jane(), OAuthScope.DELETE_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should fail to discard change request without required scope")
	void discardChangeRequestWithoutScope() {
		mvc.delete().uri("/namespaces/{slug}/services/{service}/changes/{number}", "konfigyr", service.slug(), "2")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_PROFILES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.DELETE_PROFILES));
	}

	static Consumer<MvcTestResult> changeRequestNotFound(String number) {
		return problemDetailFor(HttpStatus.NOT_FOUND, problem -> problem
				.hasTitle("Change request not found")
				.hasDetailContaining("We couldn't find a change request with the matching number")
		).andThen(hasFailedWithException(ChangeRequestNotFoundException.class, ex -> ex
				.hasMessageContaining("Could not find a change request with the following number: " + number)
		));
	}

}
