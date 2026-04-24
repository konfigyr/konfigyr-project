package com.konfigyr.audit;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.test.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AuditEventRepositoryTest extends AbstractIntegrationTest {

	@Autowired
	AuditEventRepository repository;

	@Test
	@Transactional
	@DisplayName("should insert and retrieve an audit event with all fields")
	void shouldInsertAndRetrieveAuditEvent() {
		final AuditEvent event = AuditEvent.builder()
				.namespace(EntityId.from(1))
				.entityId(EntityId.from(100))
				.entityType("namespace")
				.eventType("namespace.renamed")
				.actor(new Actor("john.doe@konfigur.com", "user", "John Doe"))
				.details(Map.of("oldName", "alpha"))
				.details("newNema", "beta")
				.build();

		repository.insert(event);

		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "namespace")
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, EntityId.from(100))
						.build(),
				CursorPageable.unpaged()
		);

		final AuditRecord found = assertThat(page.content())
				.hasSize(1)
				.first()
				.isNotNull()
				.actual();

		assertThat(found)
				.returns(event.namespaceId(), AuditRecord::namespaceId)
				.returns(event.entityId(), AuditRecord::entityId)
				.returns(event.eventType(), AuditRecord::eventType)
				.returns(event.details(), AuditRecord::details)
				.returns(event.actor(), AuditRecord::actor)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);
	}

	@Test
	@Transactional
	@DisplayName("should insert audit event without namespace and details")
	void shouldInsertAuditEventWithoutOptionalFields() {
		final AuditEvent event = AuditEvent.builder()
				.entityId(EntityId.from(100))
				.entityType("service")
				.eventType("service.created")
				.actor(new Actor("system", "system", "System"))
				.build();

		repository.insert(event);

		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, EntityId.from(100))
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "service")
						.build(),
				CursorPageable.unpaged()
		);

		final AuditRecord found = assertThat(page.content())
				.hasSize(1)
				.first()
				.isNotNull()
				.actual();

		assertThat(found)
				.returns(null, AuditRecord::namespaceId)
				.returns(event.entityId(), AuditRecord::entityId)
				.returns(event.eventType(), AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details)
				.returns(event.actor(), AuditRecord::actor)
				.satisfies(it -> assertThat(it.createdAt())
						.isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS))
				);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().getFirst())
				.returns(null, AuditRecord::namespaceId)
				.returns(Map.of(), AuditRecord::details)
				.returns("service.created", AuditRecord::eventType);
	}

	@Test
	@Transactional
	@DisplayName("should filter audit records by time range")
	void shouldFilterByTimeRange() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.FROM_CRITERIA, OffsetDateTime.now().minusDays(21))
						.criteria(AuditRecord.TO_CRITERIA, OffsetDateTime.now().minusDays(7))
						.build(),
				CursorPageable.of(10)
		);

		assertThatObject(page)
				.returns(5, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page.content())
				.hasSize(5)
				.allSatisfy(e -> assertThat(e.createdAt())
						.isAfter(OffsetDateTime.now().minusDays(21))
						.isBefore(OffsetDateTime.now().minusDays(7))
				);
	}

	@Test
	@DisplayName("should paginate audit events with cursor")
	void shouldSupportCursorPagination() {
		final var query = SearchQuery.builder()
				.criteria(AuditRecord.NAMESPACE_ID_CRITERIA, EntityId.from(2))
				.build();

		final CursorPage<AuditRecord> firstPage = repository.find(query, CursorPageable.of(4));

		assertThatObject(firstPage)
				.as("Should return the first page of 3")
				.returns(4, CursorPage::size)
				.returns(true, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		final CursorPage<AuditRecord> secondPage = repository.find(query, firstPage.nextPageable());

		assertThatObject(secondPage)
				.as("Should return the second page of 3")
				.returns(4, CursorPage::size)
				.returns(true, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious);

		final CursorPage<AuditRecord> thirdPage = repository.find(query, secondPage.nextPageable());

		assertThatObject(thirdPage)
				.as("Should return the last page of 3")
				.returns(2, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(true, CursorPage::hasPrevious);

		assertThat(firstPage.content().getFirst().createdAt())
				.isAfter(firstPage.content().getLast().createdAt())
				.isAfter(secondPage.content().getFirst().createdAt())
				.isAfter(thirdPage.content().getFirst().createdAt());

		assertThat(repository.find(query, thirdPage.previousPageable()).content())
				.isEqualTo(secondPage.content());

		assertThat(repository.find(query, secondPage.previousPageable()).content())
				.isEqualTo(firstPage.content());
	}

	@Test
	@DisplayName("should query audit events by namespace")
	void shouldQueryEventsByNamespace() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.NAMESPACE_ID_CRITERIA, EntityId.from(2))
						.build(),
				CursorPageable.unpaged()
		);

		assertThat(page.content())
				.hasSizeGreaterThanOrEqualTo(10)
				.allSatisfy(record -> assertThat(record.namespaceId())
						.isEqualTo(EntityId.from(2))
				);
	}

	@Test
	@DisplayName("should query audit events by entity type")
	void shouldQueryEventsByEntityType() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "namespace")
						.build(),
				CursorPageable.unpaged()
		);

		assertThat(page.content())
				.hasSizeGreaterThanOrEqualTo(4)
				.allSatisfy(it -> assertThat(it)
						.returns(EntityId.from(2), AuditRecord::entityId)
						.returns("namespace", AuditRecord::entityType)
				)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns("namespace.member.removed", AuditRecord::eventType),
						it -> assertThat(it)
								.returns("namespace.member.added", AuditRecord::eventType),
						it -> assertThat(it)
								.returns("namespace.updated", AuditRecord::eventType),
						it -> assertThat(it)
								.returns("namespace.renamed", AuditRecord::eventType)
				);
	}

	@Test
	@DisplayName("should query audit events by actor")
	void shouldQueryEventsByActor() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ACTOR_ID_CRITERIA, "john.doe@konfigyr.com")
						.build(),
				CursorPageable.unpaged()
		);

		assertThat(page.content())
				.hasSizeGreaterThanOrEqualTo(6)
				.allSatisfy(it -> assertThat(it.actor())
						.returns("john.doe@konfigyr.com", Actor::id)
						.returns("user", Actor::type)
						.returns("John Doe", Actor::name)
				);
	}

	@Test
	@DisplayName("should query audit events without namespace context")
	void shouldQueryEventsWithoutNamespace() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "account")
						.build(),
				CursorPageable.unpaged()
		);

		assertThat(page.content())
				.hasSize(2)
				.satisfiesExactly(
						it -> assertThat(it)
								.returns(null, AuditRecord::namespaceId)
								.returns(EntityId.from(2), AuditRecord::entityId)
								.returns("account", AuditRecord::entityType)
								.returns("account.created", AuditRecord::eventType),
						it -> assertThat(it)
								.returns(null, AuditRecord::namespaceId)
								.returns(EntityId.from(1), AuditRecord::entityId)
								.returns("account", AuditRecord::entityType)
								.returns("account.created", AuditRecord::eventType)
				);
	}

	@Test
	@Transactional
	@DisplayName("should return empty page when no events match query")
	void shouldReturnEmptyPageForNoMatches() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "nonexistent")
						.build(),
				CursorPageable.unpaged()
		);

		assertThat(page.content())
				.isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("should combine multiple filter criteria")
	void shouldCombineMultipleFilters() {
		final CursorPage<AuditRecord> page = repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.NAMESPACE_ID_CRITERIA, EntityId.from(2))
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "namespace")
						.criteria(AuditRecord.ACTOR_ID_CRITERIA, "jane.doe@konfigyr.com")
						.build(),
				CursorPageable.unpaged()
		);

		assertThatObject(page)
				.returns(2, CursorPage::size)
				.returns(false, CursorPage::hasNext)
				.returns(false, CursorPage::hasPrevious);

		assertThat(page).allSatisfy(it -> assertThat(it)
				.returns(EntityId.from(2), AuditRecord::entityId)
				.returns("namespace", AuditRecord::entityType)
				.extracting(AuditRecord::actor)
				.returns("jane.doe@konfigyr.com", Actor::id)
		).satisfiesExactly(
				it -> assertThat(it.eventType())
						.isEqualTo("namespace.member.removed"),
				it -> assertThat(it.eventType())
						.isEqualTo("namespace.updated")
		);
	}

	@Test
	@Transactional
	@DisplayName("should create new audit table partition")
	void createPartition() {
		assertThatNoException().isThrownBy(repository::createPartition);
	}

}
