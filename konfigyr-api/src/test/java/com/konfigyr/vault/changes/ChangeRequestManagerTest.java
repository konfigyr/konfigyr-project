package com.konfigyr.vault.changes;

import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.markdown.MarkdownContents;
import com.konfigyr.namespace.Service;
import com.konfigyr.namespace.Services;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.vault.*;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestManagerTest extends AbstractIntegrationTest {

	@Autowired
	ChangeRequestManager manager;

	@Autowired
	ProfileManager profiles;

	@Autowired
	Services services;

	@Mock
	AuthenticatedPrincipal principal;

	@Test
	@DisplayName("should search for all change requests for a service and sort by created date")
	void retrieveAllRequestsForService() {
		final var query = SearchQuery.of(Pageable.ofSize(10));

		assertThat(manager.search(serviceFor(2), query))
				.hasSize(5)
				.extracting(ChangeRequest::id)
				.containsExactly(
						EntityId.from(3),
						EntityId.from(6),
						EntityId.from(2),
						EntityId.from(4),
						EntityId.from(1)
				);
	}

	@Test
	@DisplayName("should search for open change requests for a service and sort them by last modified date")
	void retrieveOpenRequestsForService() {
		final var query = SearchQuery.builder()
				.criteria(ChangeRequest.STATE_CRITERIA, ChangeRequestState.OPEN)
				.pageable(PageRequest.of(0, 10, Sort.by("updated")))
				.build();

		assertThat(manager.search(serviceFor(2), query))
				.hasSize(3)
				.extracting(ChangeRequest::id)
				.containsExactly(EntityId.from(4), EntityId.from(2), EntityId.from(3));
	}

	@Test
	@DisplayName("should search change requests for a service that are targeting a single profile")
	void retrieveRequestsForServiceAndProfile() {
		final var query = SearchQuery.builder()
				.criteria(ChangeRequest.PROFILE_CRITERIA, "locked")
				.build();

		assertThat(manager.search(serviceFor(2), query))
				.hasSize(3)
				.extracting(ChangeRequest::id)
				.containsExactly(EntityId.from(3), EntityId.from(2), EntityId.from(1));
	}

	@Test
	@DisplayName("should search change requests that are matching both profile and state criteria")
	void retrieveRequestsForProfileAndState() {
		final var query = SearchQuery.builder()
				.criteria(ChangeRequest.PROFILE_CRITERIA, "staging")
				.criteria(ChangeRequest.STATE_CRITERIA, ChangeRequestState.DISCARDED)
				.build();

		assertThat(manager.search(serviceFor(2), query))
				.hasSize(1)
				.extracting(ChangeRequest::id)
				.containsExactly(EntityId.from(6));
	}

	@Test
	@DisplayName("should retrieve change request for a service by number")
	void retrieveRequestForService() {
		final var service = serviceFor(2);

		assertThat(manager.get(service, 1L))
				.isPresent()
				.get()
				.returns(service, ChangeRequest::service)
				.returns(EntityId.from(1), ChangeRequest::id)
				.returns(1L, ChangeRequest::number)
				.returns(ChangeRequestState.MERGED, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.MERGEABLE, ChangeRequest::mergeStatus)
				.returns(1, ChangeRequest::count)
				.returns("Update application name", ChangeRequest::subject)
				.returns(MarkdownContents.of("Align application name with new naming convention"), ChangeRequest::description)
				.returns("John Doe", ChangeRequest::createdBy);
	}

	@Test
	@DisplayName("should retrieve change request for a service by an unknown number")
	void retrieveUnknownRequestForService() {
		assertThat(manager.get(serviceFor(1), 9999L))
				.isEmpty();
	}

	@Test
	@DisplayName("should retrieve property transitions for a change request")
	void retrievePropertyTransitions() {
		assertThat(manager.changes(changeRequestFor(2, 1)))
				.hasSize(1)
				.first()
				.returns("spring.application.name", PropertyTransition::name)
				.returns(PropertyTransitionType.UPDATED, PropertyTransition::type);
	}

	@Test
	@DisplayName("should retrieve history records for a change request")
	void retrieveHistoryRecords() {
		assertThat(manager.history(changeRequestFor(1, 1)))
				.hasSize(3)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(ChangeRequestHistory.Type.CREATED, ChangeRequestHistory::type)
								.returns("Jane Doe", ChangeRequestHistory::initiator),
						it -> assertThat(it)
								.returns(ChangeRequestHistory.Type.COMMENTED, ChangeRequestHistory::type)
								.returns("John Doe", ChangeRequestHistory::initiator),
						it -> assertThat(it)
								.returns(ChangeRequestHistory.Type.DISCARDED, ChangeRequestHistory::type)
								.returns("Jane Doe", ChangeRequestHistory::initiator)
				);
	}

	@Test
	@Transactional
	@DisplayName("should create new change request")
	void createChangeRequest() {
		final var service = serviceFor(1);
		final var profile = profiles.get(EntityId.from(5)).orElseThrow();
		final var changes = Set.of(
				PropertyTransition.added("spring.datasource.username",
						PropertyValue.sealed(ByteArray.fromString("checksum"), ByteArray.fromString("db-user"))),
				PropertyTransition.added("spring.datasource.password",
						PropertyValue.sealed(ByteArray.fromString("checksum"), ByteArray.fromString("db-pass")))
		);
		final var result = new ApplyResult("changeset-revision", "profile-revision",
				"Incoming changes for live profile", null, changes, principal, OffsetDateTime.now());

		doReturn(Optional.of("John Doe")).when(principal).getDisplayName();
		final var command = new ChangeRequestCreateCommand(service, profile, result, "changeset-branch");
		final var request = manager.create(command);

		assertThat(request)
				.returns(service, ChangeRequest::service)
				.returns(profile, ChangeRequest::profile)
				.returns(2L, ChangeRequest::number)
				.returns(ChangeRequestState.OPEN, ChangeRequest::state)
				.returns(ChangeRequestMergeStatus.NOT_APPROVED, ChangeRequest::mergeStatus)
				.returns(changes.size(), ChangeRequest::count)
				.returns(result.subject(), ChangeRequest::subject)
				.returns(result.description(), ChangeRequest::description)
				.returns("John Doe", ChangeRequest::createdBy);

		assertThat(manager.changes(request))
				.containsExactlyInAnyOrderElementsOf(changes);
	}

	@Transactional
	@CsvSource({ "MERGED,MERGED", "DISCARDED,DISCARDED" })
	@DisplayName("should update the change request state")
	@ParameterizedTest(name = "should transition change request to {0} state and store {1} event")
	void updateChangeRequestState(ChangeRequestState state, ChangeRequestHistory.Type event) {
		doReturn(Optional.of("Jane Doe")).when(principal).getDisplayName();
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 4L, principal,
				state, null, null);

		assertThat(manager.update(command))
				.returns(EntityId.from(4), ChangeRequest::id)
				.returns(state, ChangeRequest::state)
				.returns("Tune logging levels", ChangeRequest::subject)
				.returns(MarkdownContents.of("Reduce log verbosity in production"), ChangeRequest::description)
				.satisfies(it -> assertThat(it.updatedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThat(manager.history(changeRequestFor(2, 4)))
				.hasSize(2)
				.last()
				.returns(event, ChangeRequestHistory::type)
				.returns("Jane Doe", ChangeRequestHistory::initiator)
				.satisfies(it -> assertThat(it.timestamp())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Transactional
	@CsvSource({ "1,MERGED", "2,OPEN", "5,DISCARDED" })
	@DisplayName("should ignore the change request state update when it is the same")
	@ParameterizedTest(name = "should ignore state update to {1} for change request with number {1}")
	void ignoreChangeRequestStateUpdate(long number, ChangeRequestState state) {
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), number, principal,
				state, null, null);

		assertThat(manager.update(command))
				.returns(state, ChangeRequest::state)
				.satisfies(it -> assertThat(it.updatedAt())
						.isBefore(OffsetDateTime.now().minusDays(1))
				);
	}

	@Test
	@Transactional
	@DisplayName("should update the change request subject")
	void updateChangeRequestSubject() {
		doReturn(Optional.of("John Doe")).when(principal).getDisplayName();
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 1L, principal,
				null, "New subject name", null);

		assertThat(manager.update(command))
				.returns(EntityId.from(1), ChangeRequest::id)
				.returns(command.subject(), ChangeRequest::subject)
				.returns(MarkdownContents.of("Align application name with new naming convention"), ChangeRequest::description);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(4)
				.last()
				.returns(ChangeRequestHistory.Type.RENAMED, ChangeRequestHistory::type)
				.returns("John Doe", ChangeRequestHistory::initiator);
	}

	@Test
	@Transactional
	@DisplayName("should update the change request subject and state")
	void updateChangeRequestSubjectAndState() {
		doReturn(Optional.of("John Doe")).when(principal).getDisplayName();
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 2L, principal,
				ChangeRequestState.DISCARDED, "Discarding change request", null);

		assertThat(manager.update(command))
				.returns(EntityId.from(2), ChangeRequest::id)
				.returns(ChangeRequestState.DISCARDED, ChangeRequest::state)
				.returns(command.subject(), ChangeRequest::subject)
				.returns(MarkdownContents.of("Move service to new port range"), ChangeRequest::description);

		assertThat(manager.history(changeRequestFor(2, 2)))
				.hasSize(4)
				.satisfies(it -> assertThat(it)
								.returns(ChangeRequestHistory.Type.RENAMED, ChangeRequestHistory::type)
								.returns("John Doe", ChangeRequestHistory::initiator),
						Index.atIndex(2)
				)
				.satisfies(it -> assertThat(it)
								.returns(ChangeRequestHistory.Type.DISCARDED, ChangeRequestHistory::type)
								.returns("John Doe", ChangeRequestHistory::initiator),
						Index.atIndex(3)
				);
	}

	@Test
	@Transactional
	@DisplayName("should update the change request subject and ignore state")
	void updateChangeRequestSubjectAndIgnoreState() {
		doReturn(Optional.of("John Doe")).when(principal).getDisplayName();
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 1L, principal,
				ChangeRequestState.DISCARDED, "New subject name", null);

		assertThat(manager.update(command))
				.returns(EntityId.from(1), ChangeRequest::id)
				.returns(ChangeRequestState.MERGED, ChangeRequest::state)
				.returns(command.subject(), ChangeRequest::subject)
				.returns(MarkdownContents.of("Align application name with new naming convention"), ChangeRequest::description);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(4)
				.last()
				.returns(ChangeRequestHistory.Type.RENAMED, ChangeRequestHistory::type)
				.returns("John Doe", ChangeRequestHistory::initiator);
	}

	@Test
	@Transactional
	@DisplayName("should update the change request subject and description")
	void updateChangeRequestSubjectAndDescription() {
		doReturn("john.doe@konfigyr.com").when(principal).get();
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 1L, principal,
				null, "New subject name", MarkdownContents.of("New description"));

		assertThat(manager.update(command))
				.returns(EntityId.from(1), ChangeRequest::id)
				.returns(command.subject(), ChangeRequest::subject)
				.returns(command.description(), ChangeRequest::description);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(4)
				.last()
				.returns(ChangeRequestHistory.Type.RENAMED, ChangeRequestHistory::type)
				.returns("john.doe@konfigyr.com", ChangeRequestHistory::initiator);
	}

	@Test
	@Transactional
	@DisplayName("should update the change request description and ignore same subject")
	void ignoreChangeRequestSubjectUpdate() {
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 1L, principal,
				null, "Update application name", MarkdownContents.of("New description"));

		assertThat(manager.update(command))
				.returns(EntityId.from(1), ChangeRequest::id)
				.returns(command.subject(), ChangeRequest::subject)
				.returns(command.description(), ChangeRequest::description);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(3);
	}

	@Test
	@Transactional
	@DisplayName("should not update the change request description and subject when empty")
	void ignoreChangeRequestUpdate() {
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 1L, principal, null, null, null);

		assertThat(manager.update(command))
				.returns(EntityId.from(1), ChangeRequest::id)
				.returns("Update application name", ChangeRequest::subject)
				.returns(MarkdownContents.of("Align application name with new naming convention"), ChangeRequest::description);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(3);
	}

	@Test
	@DisplayName("should fail to update an unknown change request")
	void updateUnknownChangeRequest() {
		final var command = new ChangeRequestUpdateCommand(serviceFor(2), 9999L, principal,
				null, "New subject name", null);

		assertThatExceptionOfType(ChangeRequestNotFoundException.class)
				.isThrownBy(() -> manager.update(command));
	}

	@Test
	@Transactional
	@DisplayName("should submit a change request comment")
	void submitChangeRequestComment() {
		doReturn("john.doe@konfigyr.com").when(principal).get();
		final var command = new ChangeRequestReviewCommand(serviceFor(2), 1L, principal,
				ChangeRequestReviewCommand.Operation.COMMENT, MarkdownContents.of("So long, and thanks for all the fish"));

		assertThat(manager.review(command))
				.returns(EntityId.from(1), ChangeRequest::id);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(4)
				.last()
				.returns(ChangeRequestHistory.Type.COMMENTED, ChangeRequestHistory::type)
				.returns(MarkdownContents.of("So long, and thanks for all the fish"), ChangeRequestHistory::comment)
				.returns("john.doe@konfigyr.com", ChangeRequestHistory::initiator);
	}

	@Test
	@Transactional
	@DisplayName("should submit a change request approval")
	void submitChangeRequestApproval() {
		doReturn("john.doe@konfigyr.com").when(principal).get();
		final var command = new ChangeRequestReviewCommand(serviceFor(2), 1L, principal,
				ChangeRequestReviewCommand.Operation.APPROVE, MarkdownContents.of("LGTM"));

		assertThat(manager.review(command))
				.returns(EntityId.from(1), ChangeRequest::id);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(4)
				.last()
				.returns(ChangeRequestHistory.Type.APPROVED, ChangeRequestHistory::type)
				.returns(MarkdownContents.of("LGTM"), ChangeRequestHistory::comment)
				.returns("john.doe@konfigyr.com", ChangeRequestHistory::initiator);
	}

	@Test
	@Transactional
	@DisplayName("should request additional changes for change request")
	void requestAdditionalChangesForChangeRequest() {
		doReturn("john.doe@konfigyr.com").when(principal).get();
		final var command = new ChangeRequestReviewCommand(serviceFor(2), 1L, principal,
				ChangeRequestReviewCommand.Operation.REQUEST_CHANGES);

		assertThat(manager.review(command))
				.returns(EntityId.from(1), ChangeRequest::id);

		assertThat(manager.history(changeRequestFor(2, 1)))
				.hasSize(4)
				.last()
				.returns(ChangeRequestHistory.Type.CHANGES_REQUESTED, ChangeRequestHistory::type)
				.returns(null, ChangeRequestHistory::comment)
				.returns("john.doe@konfigyr.com", ChangeRequestHistory::initiator);
	}

	@Test
	@DisplayName("should fail to review an unknown change request")
	void reviewUnknownChangeRequest() {
		final var command = new ChangeRequestReviewCommand(serviceFor(2), 9999L, principal,
				ChangeRequestReviewCommand.Operation.APPROVE);

		assertThatExceptionOfType(ChangeRequestNotFoundException.class)
				.isThrownBy(() -> manager.review(command));
	}

	Service serviceFor(long id) {
		return assertThat(services.get(EntityId.from(id)))
				.as("Service with identifier %d should exists", id)
				.isPresent()
				.get()
				.actual();
	}

	ChangeRequest changeRequestFor(long service, long number) {
		return assertThat(manager.get(serviceFor(service), number))
				.as("Change request with should exists for: [service=%d, number=%d]", service, number)
				.isPresent()
				.get()
				.actual();
	}
}
