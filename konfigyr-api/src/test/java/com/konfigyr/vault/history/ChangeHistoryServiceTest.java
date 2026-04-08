package com.konfigyr.vault.history;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ChangeHistoryServiceTest extends AbstractIntegrationTest {

	@Autowired
	ProfileManager profiles;

	@Autowired
	ChangeHistoryService chronicle;

	@Test
	@Transactional
	@DisplayName("should insert change history for applied results")
	void commitChangeHistory() {
		final var principal = (AuthenticatedPrincipal) TestPrincipals.john().getPrincipal();

		final var result = new ApplyResult(
				"new-revision",
				"parent-revision",
				"Subject of changes",
				"Description of changes",
				Set.of(
						PropertyTransition.added("logging.level.root", PropertyValue.sealed(
								ByteArray.fromString("INFO"),
								ByteArray.fromString("info-checksum")
						)),
						PropertyTransition.added("logging.level.web", PropertyValue.sealed(
								ByteArray.fromString("DEBUG"),
								ByteArray.fromString("debug-checksum")
						)),
						PropertyTransition.updated("spring.application.name", PropertyValue.sealed(
								ByteArray.fromString("old-value"),
								ByteArray.fromString("old-value-checksum")
						), PropertyValue.sealed(
								ByteArray.fromString("new-value"),
								ByteArray.fromString("new-value-checksum")
						)),
						PropertyTransition.removed("server.port", PropertyValue.sealed(
								ByteArray.fromString("8080"),
								ByteArray.fromString("8080-checksum")
						))
				),
				principal,
				OffsetDateTime.now()
		);

		assertThatNoException().isThrownBy(() -> chronicle.commit(EntityId.from(4), result));

		final var revision = assertThat(chronicle.examine(profileFor(4), result.revision()))
				.as("Created revision should exist for profile")
				.isPresent()
				.get()
				.returns(result.revision(), ChangeHistory::revision)
				.returns(result.subject(), ChangeHistory::subject)
				.returns(result.description(), ChangeHistory::description)
				.returns(result.timestamp(), ChangeHistory::appliedAt)
				.returns("John Doe", ChangeHistory::appliedBy)
				.actual();

		assertThat(chronicle.traceRevision(revision))
				.as("Revision should contain property changes")
				.hasSize(4)
				.allSatisfy(it -> assertThat(it)
						.as("Property changes should have the same revision, author and timestamp")
						.returns(result.revision(), PropertyHistory::revision)
						.returns(result.timestamp(), PropertyHistory::appliedAt)
						.returns("John Doe", PropertyHistory::appliedBy)
				)
				.satisfiesOnlyOnce(it -> assertThat(it)
						.as("Changes should contain the %s property", "logging.level.root")
						.returns("logging.level.root", PropertyHistory::name)
						.returns(PropertyTransitionType.ADDED, PropertyHistory::action)
						.returns(null, PropertyHistory::from)
						.returns(PropertyValue.sealed(
								ByteArray.fromString("INFO"),
								ByteArray.fromString("info-checksum")
						), PropertyHistory::to)
				)
				.satisfiesOnlyOnce(it -> assertThat(it)
						.as("Changes should contain the %s property", "logging.level.web")
						.returns("logging.level.web", PropertyHistory::name)
						.returns(PropertyTransitionType.ADDED, PropertyHistory::action)
						.returns(null, PropertyHistory::from)
						.returns(PropertyValue.sealed(
								ByteArray.fromString("DEBUG"),
								ByteArray.fromString("debug-checksum")
						), PropertyHistory::to)
				)
				.satisfiesOnlyOnce(it -> assertThat(it)
						.as("Changes should contain the %s property", "spring.application.name")
						.returns("spring.application.name", PropertyHistory::name)
						.returns(PropertyTransitionType.UPDATED, PropertyHistory::action)
						.returns(PropertyValue.sealed(
								ByteArray.fromString("old-value"),
								ByteArray.fromString("old-value-checksum")
						), PropertyHistory::from)
						.returns(PropertyValue.sealed(
								ByteArray.fromString("new-value"),
								ByteArray.fromString("new-value-checksum")
						), PropertyHistory::to)
				)
				.satisfiesOnlyOnce(it -> assertThat(it)
						.as("Changes should contain the %s property", "server.port")
						.returns("server.port", PropertyHistory::name)
						.returns(PropertyTransitionType.REMOVED, PropertyHistory::action)
						.returns(PropertyValue.sealed(
								ByteArray.fromString("8080"),
								ByteArray.fromString("8080-checksum")
						), PropertyHistory::from)
						.returns(null, PropertyHistory::to)
				);
	}

	@Test
	@DisplayName("should fail to commit change history for unknown profile")
	void commitChangeHistoryForUnknownProfile() {
		assertThatExceptionOfType(ProfileNotFoundException.class)
				.isThrownBy(() -> chronicle.commit(EntityId.from(9999), Mockito.mock(ApplyResult.class)));
	}

	@Test
	@DisplayName("should list change history for a profile and use token to paginate the results")
	void listChangeHistory() {
		final var profile = profileFor(4);

		CursorPage<ChangeHistory> page = chronicle.fetchHistory(profile, CursorPageable.of(3));

		assertThatObject(page)
				.as("Should have a next page but no previous page")
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(ChangeHistory::revision)
				.containsExactly("last-revision", "sixth-revision", "fifth-revision");

		page = chronicle.fetchHistory(profile, page.nextPageable());

		assertThatObject(page)
				.as("Should have both next and previous pages")
				.returns(true, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(ChangeHistory::revision)
				.containsExactly("fourth-revision", "third-revision", "second-revision");

		page = chronicle.fetchHistory(profile, page.nextPageable());

		assertThatObject(page)
				.as("Should not have any more pages")
				.returns(false, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(1)
				.extracting(ChangeHistory::revision)
				.containsExactly("first-revision");

		page = chronicle.fetchHistory(profile, page.previousPageable());

		assertThatObject(page)
				.as("Should reach back to the second page")
				.returns(true, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(ChangeHistory::revision)
				.containsExactly("fourth-revision", "third-revision", "second-revision");

		page = chronicle.fetchHistory(profile, page.previousPageable());

		assertThatObject(page)
				.as("Should reach the first page")
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(ChangeHistory::revision)
				.containsExactly("last-revision", "sixth-revision", "fifth-revision");
	}

	@Test
	@DisplayName("should return the full change history for profile when pageable is unpaged")
	void retrieveChangeHistoryForUnpagedCursor() {
		final var page = chronicle.fetchHistory(profileFor(4), CursorPageable.unpaged());

		assertThatObject(page)
				.returns(7, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page)
				.hasSize(7)
				.extracting(ChangeHistory::revision)
				.containsExactly("last-revision", "sixth-revision", "fifth-revision",
						"fourth-revision", "third-revision", "second-revision", "first-revision");
	}

	@Test
	@DisplayName("should return the full change history for profile when pageable size is greater than content size")
	void retrieveChangeHistoryForPagedCursor() {
		final var page = chronicle.fetchHistory(profileFor(4), CursorPageable.of(100));

		assertThatObject(page)
				.returns(7, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page)
				.hasSize(7)
				.extracting(ChangeHistory::revision)
				.containsExactly("last-revision", "sixth-revision", "fifth-revision",
						"fourth-revision", "third-revision", "second-revision", "first-revision");
	}

	@Test
	@DisplayName("should examine the change history for profile and revision")
	void examineChangeHistory() {
		assertThat(chronicle.examine(profileFor(3), "first-revision"))
				.isPresent()
				.get()
				.returns(EntityId.from(8), ChangeHistory::id)
				.returns("first-revision", ChangeHistory::revision)
				.returns("First change", ChangeHistory::subject)
				.returns("Initial changes", ChangeHistory::description)
				.returns("John Doe", ChangeHistory::appliedBy)
				.satisfies(it -> assertThat(it.appliedAt())
						.isCloseTo(OffsetDateTime.now().minusDays(2), within(5, ChronoUnit.MINUTES))
				);
	}

	@Test
	@DisplayName("should fail to examine the change history for profile and unknown revision")
	void examineUnknownChangeHistory() {
		assertThat(chronicle.examine(profileFor(3), "unknown-revision"))
				.isEmpty();
	}

	@Test
	@DisplayName("should trace property changes for a profile and use token to paginate the results")
	void tracePropertyChanges() {
		final var profile = profileFor(4);

		CursorPage<PropertyHistory> page = chronicle.traceProperty(profile, "spring.application.name", CursorPageable.of(3));

		assertThatObject(page)
				.as("Should be the first page")
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(PropertyHistory::revision)
				.containsExactly("last-revision", "sixth-revision", "fifth-revision");

		page = chronicle.traceProperty(profile, "spring.application.name", page.nextPageable());

		assertThatObject(page)
				.as("Should be the last page")
				.returns(false, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(PropertyHistory::revision)
				.containsExactly("fourth-revision", "second-revision", "first-revision");

		page = chronicle.traceProperty(profile, "spring.application.name", page.previousPageable());

		assertThatObject(page)
				.as("Should navigate back to the the first page")
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(3)
				.extracting(PropertyHistory::revision)
				.containsExactly("last-revision", "sixth-revision", "fifth-revision");
	}

	@Test
	@DisplayName("should return the full property transitions page for profile when pageable is unpaged")
	void retrievePropertyTransitionForUnpagedCursor() {
		final var page = chronicle.traceProperty(profileFor(4), "spring.application.name", CursorPageable.unpaged());

		assertThatObject(page)
				.returns(6, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

//		assertThat(page)
//				.hasSize(7)
//				.extracting(ChangeHistory::id)
//				.containsExactly("last-revision", "sixth-revision", "fifth-revision",
//						"fourth-revision", "third-revision", "second-revision", "first-revision");
	}

	@Test
	@DisplayName("should return the full property transitions page for profile when pageable size is greater than content size")
	void retrievePropertyTransitionForPagedCursor() {
		final var page = chronicle.traceProperty(profileFor(4), "spring.application.group", CursorPageable.of(10));

		assertThatObject(page)
				.returns(2, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page)
				.hasSize(2)
				.extracting(PropertyHistory::revision, PropertyHistory::name, PropertyHistory::action)
				.containsExactly(
						tuple("third-revision", "spring.application.group", PropertyTransitionType.REMOVED),
						tuple("second-revision", "spring.application.group", PropertyTransitionType.ADDED)
				);
	}

	@Test
	@DisplayName("should trace property changes for a profile and revision")
	void tracePropertyChangesForRevision() {
		final var revision = chronicle.examine(profileFor(4), "second-revision")
				.orElseThrow(() -> new IllegalStateException("Revision not found"));

		assertThat(chronicle.traceRevision(revision))
				.hasSize(2)
				.extracting(PropertyHistory::revision, PropertyHistory::name, PropertyHistory::action)
				.containsExactlyInAnyOrder(
						tuple("second-revision", "spring.application.name", PropertyTransitionType.UPDATED),
						tuple("second-revision", "spring.application.group", PropertyTransitionType.ADDED)
				);
	}

	@Test
	@DisplayName("should trace property changes for a profile and revision that have no property transitions")
	void traceEmptyPropertyChangesForRevision() {
		final var revision = chronicle.examine(profileFor(3), "first-revision")
				.orElseThrow(() -> new IllegalStateException("Revision not found"));

		assertThat(chronicle.traceRevision(revision))
				.isEmpty();
	}

	private Profile profileFor(long id) {
		return assertThat(profiles.get(EntityId.from(id)))
				.as("Profile with id %d should exist", id)
				.isPresent()
				.get()
				.actual();
	}

}
