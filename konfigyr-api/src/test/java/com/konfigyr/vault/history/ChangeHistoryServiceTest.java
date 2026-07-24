package com.konfigyr.vault.history;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.markdown.MarkdownContents;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static com.konfigyr.data.tables.VaultProperties.VAULT_PROPERTIES;
import static org.assertj.core.api.Assertions.*;

class ChangeHistoryServiceTest extends AbstractIntegrationTest {

	@Autowired
	DSLContext context;

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
				.returns("new-revision", ChangeHistory::revision)
				.returns("Subject of changes", ChangeHistory::subject)
				.returns(MarkdownContents.of("Description of changes"), ChangeHistory::description)
				.returns("John Doe", ChangeHistory::appliedBy)
				.satisfies(it -> assertThat(it.appliedAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				)
				.actual();

		assertThat(chronicle.traceRevision(revision))
				.as("Revision should contain property changes")
				.hasSize(4)
				.allSatisfy(it -> assertThat(it)
						.as("Property changes should have the same revision, author and timestamp")
						.returns(result.revision(), PropertyHistory::revision)
						.returns("John Doe", PropertyHistory::appliedBy)
				)
				.allSatisfy(history -> assertThat(history.appliedAt())
						.as("Property changes should have the same timestamp")
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
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
				.returns("019690a1-0008-7000-8000-000000000008", ChangeHistory::id)
				.returns("first-revision", ChangeHistory::revision)
				.returns("First change", ChangeHistory::subject)
				.returns(MarkdownContents.of("Initial changes"), ChangeHistory::description)
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

	@Test
	@DisplayName("should create partitions for change and property history tables")
	void createTablePartitions() {
		assertThatNoException().isThrownBy(chronicle::createPartitions);
	}

	@Test
	@Transactional
	@DisplayName("should upsert added properties and update the checksum, timestamp and author on a later sync")
	void synchronizeAddedAndUpdatedProperties() {
		final var john = (AuthenticatedPrincipal) TestPrincipals.john().getPrincipal();

		chronicle.synchronize(EntityId.from(1), applyResult("revision-1", john, Set.of(
				PropertyTransition.added("sync-test.logging.level.root", value("info")),
				PropertyTransition.added("sync-test.logging.level.web", value("debug")),
				PropertyTransition.added("sync-test.server.port", value("8080"))
		)));

		assertThat(fetchProperties(1))
				.extracting(r -> r.get(VAULT_PROPERTIES.NAME))
				.contains("sync-test.logging.level.root", "sync-test.logging.level.web", "sync-test.server.port");

		final String originalChecksum = fetchProperty(1, "sync-test.logging.level.root").get(VAULT_PROPERTIES.CHECKSUM);

		final var jane = (AuthenticatedPrincipal) TestPrincipals.jane().getPrincipal();

		chronicle.synchronize(EntityId.from(1), applyResult("revision-2", jane, Set.of(
				PropertyTransition.updated("sync-test.logging.level.root", value("info"), value("warn"))
		)));

		assertThat(fetchProperty(1, "sync-test.logging.level.root"))
				.returns("revision-2", r -> r.get(VAULT_PROPERTIES.REVISION))
				.returns("Jane Doe", r -> r.get(VAULT_PROPERTIES.AUTHOR_NAME))
				.returns("USER_ACCOUNT", r -> r.get(VAULT_PROPERTIES.AUTHOR_TYPE))
				.returns(jane.get(), r -> r.get(VAULT_PROPERTIES.AUTHOR_ID))
				.satisfies(r -> assertThat(r.get(VAULT_PROPERTIES.CHECKSUM)).isNotEqualTo(originalChecksum))
				.satisfies(r -> assertThat(r.get(VAULT_PROPERTIES.UPDATED_AT))
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS)));
	}

	@Test
	@DisplayName("should fail to synchronize properties for unknown profile")
	void synchronizePropertiesForUnknownProfile() {
		assertThatExceptionOfType(ProfileNotFoundException.class)
				.isThrownBy(() -> chronicle.synchronize(EntityId.from(9999), Mockito.mock(ApplyResult.class)));
	}

	@Test
	@Transactional
	@DisplayName("should delete the property row when it is removed")
	void synchronizeRemovedProperty() {
		final var john = (AuthenticatedPrincipal) TestPrincipals.john().getPrincipal();

		chronicle.synchronize(EntityId.from(2), applyResult(john, Set.of(
				PropertyTransition.added("sync-test.application.name", value("service-a")),
				PropertyTransition.added("sync-test.application.group", value("group-a")),
				PropertyTransition.added("sync-test.server.port", value("8080"))
		)));

		chronicle.synchronize(EntityId.from(2), applyResult(john, Set.of(
				PropertyTransition.removed("sync-test.server.port", value("8080"))
		)));

		assertThat(fetchProperties(2))
				.extracting(r -> r.get(VAULT_PROPERTIES.NAME))
				.contains("sync-test.application.name", "sync-test.application.group")
				.doesNotContain("sync-test.server.port");
	}

	@Test
	@Transactional
	@DisplayName("should reflect only the net current state when a merge both adds and removes properties")
	void synchronizeMixedAddAndRemove() {
		final var john = (AuthenticatedPrincipal) TestPrincipals.john().getPrincipal();

		chronicle.synchronize(EntityId.from(1), applyResult(john, Set.of(
				PropertyTransition.added("sync-test.feature.toggle.a", value("true")),
				PropertyTransition.added("sync-test.feature.toggle.b", value("true"))
		)));

		chronicle.synchronize(EntityId.from(1), applyResult(john, Set.of(
				PropertyTransition.added("sync-test.feature.toggle.c", value("true")),
				PropertyTransition.removed("sync-test.feature.toggle.a", value("true"))
		)));

		assertThat(fetchProperties(1))
				.extracting(r -> r.get(VAULT_PROPERTIES.NAME))
				.contains("sync-test.feature.toggle.b", "sync-test.feature.toggle.c")
				.doesNotContain("sync-test.feature.toggle.a");
	}

	@Test
	@DisplayName("should return zero rows for a profile that never had any properties synced")
	void noPropertiesForUntouchedProfile() {
		assertThat(fetchProperties(999999)).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should isolate properties per profile even when property names overlap")
	void synchronizeIsolatesPropertiesPerProfile() {
		final var john = (AuthenticatedPrincipal) TestPrincipals.john().getPrincipal();

		chronicle.synchronize(EntityId.from(1), applyResult(john, Set.of(
				PropertyTransition.added("sync-test.application.name", value("service-a"))
		)));

		chronicle.synchronize(EntityId.from(2), applyResult(john, Set.of(
				PropertyTransition.added("sync-test.application.name", value("service-b"))
		)));

		assertThat(fetchProperties(1))
				.extracting(r -> r.get(VAULT_PROPERTIES.NAME))
				.contains("sync-test.application.name");

		assertThat(fetchProperties(2))
				.extracting(r -> r.get(VAULT_PROPERTIES.NAME))
				.contains("sync-test.application.name");

		assertThat(fetchProperty(1, "sync-test.application.name").get(VAULT_PROPERTIES.CHECKSUM))
				.isNotEqualTo(fetchProperty(2, "sync-test.application.name").get(VAULT_PROPERTIES.CHECKSUM));
	}

	private static PropertyValue value(String content) {
		return PropertyValue.sealed(ByteArray.fromString(content), ByteArray.fromString(content));
	}

	private static ApplyResult applyResult(AuthenticatedPrincipal author, Set<PropertyTransition> changes) {
		return applyResult("revision", author, changes);
	}

	private static ApplyResult applyResult(String revision, AuthenticatedPrincipal author, Set<PropertyTransition> changes) {
		return new ApplyResult(revision, null, "Test changes", null, changes, author, OffsetDateTime.now());
	}

	private List<Record> fetchProperties(long profileId) {
		return context.selectFrom(VAULT_PROPERTIES)
				.where(VAULT_PROPERTIES.PROFILE_ID.eq(profileId))
				.fetch();
	}

	private Record fetchProperty(long profileId, String name) {
		return context.selectFrom(VAULT_PROPERTIES)
				.where(VAULT_PROPERTIES.PROFILE_ID.eq(profileId), VAULT_PROPERTIES.NAME.eq(name))
				.fetchOne();
	}

	private Profile profileFor(long id) {
		return assertThat(profiles.get(EntityId.from(id)))
				.as("Profile with id %d should exist", id)
				.isPresent()
				.get()
				.actual();
	}

}
