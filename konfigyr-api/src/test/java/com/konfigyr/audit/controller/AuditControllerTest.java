package com.konfigyr.audit.controller;

import com.konfigyr.audit.AuditRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.hateoas.CursorModel;
import com.konfigyr.hateoas.LinkRelation;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class AuditControllerTest extends AbstractControllerTest {

	@Test
	@DisplayName("should retrieve audit events for namespace")
	void shouldRetrieveAuditEvents() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(AuditRecord.class))
				.hasSize(10)
				.allSatisfy(record -> assertThat(record.namespaceId())
						.isEqualTo(EntityId.from(2))
				);
	}

	@Test
	@DisplayName("should paginate audit events with cursor")
	void shouldPaginateAuditEvents() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("size", "3")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(3)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.returns(3L, CursorModel.CursorMetadata::size)
						.satisfies(meta -> assertThat(meta.next()).isNotNull())
				)
				.satisfies(it -> assertThat(it.getLinks())
						.anyMatch(link -> link.hasRel(LinkRelation.NEXT))
				);
	}

	@Test
	@DisplayName("should navigate to next page using cursor token")
	void shouldNavigateToNextPage() {
		final CursorModel<AuditRecord> page = mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("size", "3")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.actual();

		final String nextToken = assertThat(page.getMetadata())
				.isNotNull()
				.extracting(CursorModel.CursorMetadata::next)
				.isNotNull()
				.actual();

		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("size", "3")
				.queryParam("token", nextToken)
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.satisfies(it -> assertThat(it.getContent())
						.hasSize(3)
				)
				.satisfies(it -> assertThat(it.getMetadata())
						.satisfies(meta -> assertThat(meta.previous()).isNotNull())
				)
				.satisfies(it -> assertThat(it.getLinks())
						.anyMatch(link -> link.hasRel(LinkRelation.PREVIOUS))
				);
	}

	@Test
	@DisplayName("should filter audit events by entity type")
	void shouldFilterByEntityType() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("entityType", "namespace")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(AuditRecord.class))
				.hasSize(4)
				.allSatisfy(record -> assertThat(record.entityType())
						.isEqualTo("namespace")
				);
	}

	@Test
	@DisplayName("should filter audit events by event type")
	void shouldFilterByEventType() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("eventType", "service.created")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(AuditRecord.class))
				.hasSize(1)
				.first()
				.satisfies(record -> assertThat(record.eventType())
						.isEqualTo("service.created")
				);
	}

	@Test
	@DisplayName("should filter audit events by actor")
	void shouldFilterByActor() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("actor", "jane.doe@konfigyr.com")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(AuditRecord.class))
				.hasSizeGreaterThanOrEqualTo(2)
				.allSatisfy(record -> assertThat(record.actor().id())
						.isEqualTo("jane.doe@konfigyr.com")
				);
	}

	@Test
	@DisplayName("should filter audit events by time range")
	void shouldFilterByTimeRange() {
		final String from = OffsetDateTime.now().minusDays(21).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		final String to = OffsetDateTime.now().minusDays(7).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("from", from)
				.queryParam("to", to)
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(AuditRecord.class))
				.allSatisfy(record -> assertThat(record.createdAt())
						.isAfter(OffsetDateTime.now().minusDays(21))
						.isBefore(OffsetDateTime.now().minusDays(7))
				);
	}

	@Test
	@DisplayName("should combine multiple filter criteria")
	void shouldCombineFilters() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("entityType", "namespace")
				.queryParam("actor", "jane.doe@konfigyr.com")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.extracting(CursorModel::getContent, InstanceOfAssertFactories.iterable(AuditRecord.class))
				.hasSize(2)
				.allSatisfy(record -> {
					assertThat(record.entityType()).isEqualTo("namespace");
					assertThat(record.actor().id()).isEqualTo("jane.doe@konfigyr.com");
				});
	}

	@Test
	@DisplayName("should return empty result for non-matching filters")
	void shouldReturnEmptyForNoMatches() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.queryParam("entityType", "nonexistent")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(cursorModel(AuditRecord.class))
				.satisfies(it -> assertThat(it.getContent())
						.isEmpty()
				);
	}

	@Test
	@DisplayName("should return not found for unknown namespace")
	void shouldReturnNotFoundForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/audit", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should deny access when user is not a namespace member")
	void shouldDenyAccessForNonMembers() {
		mvc.get().uri("/namespaces/{slug}/audit", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should deny access when namespaces:read scope is missing")
	void shouldDenyAccessWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should deny access for unauthenticated requests")
	void shouldDenyUnauthenticatedAccess() {
		mvc.get().uri("/namespaces/{slug}/audit", "konfigyr")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(unauthorized());
	}

}
