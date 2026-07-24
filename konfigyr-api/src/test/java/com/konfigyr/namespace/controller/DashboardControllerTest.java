package com.konfigyr.namespace.controller;

import com.konfigyr.feature.FeatureValue;
import com.konfigyr.namespace.NamespaceFeatures;
import com.konfigyr.namespace.dashboard.DashboardSummary;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.AbstractControllerTest;
import com.konfigyr.test.TestPrincipals;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;
import static com.konfigyr.data.tables.VaultProfiles.VAULT_PROFILES;
import static com.konfigyr.data.tables.VaultProperties.VAULT_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

class DashboardControllerTest extends AbstractControllerTest {

	@Autowired
	DSLContext context;

	@Test
	@Transactional
	@DisplayName("should return dashboard summary for a namespace with a limited member plan")
	void shouldReturnDashboardSummary() {
		given(features.get("konfigyr", NamespaceFeatures.MEMBERS_COUNT))
				.willReturn(Optional.of(FeatureValue.limited(10)));

		insertActiveProperty(1, "dashboard-test.logging.level");
		insertActiveProperty(1, "dashboard-test.server.port");
		insertActiveProperty(2, "dashboard-test.server.port");

		mvc.get().uri("/namespaces/{slug}/dashboard", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.bodyJson()
				.convertTo(DashboardSummary.class)
				// namespace owns services 2 and 3, see data/services.sql
				.returns(2L, DashboardSummary::activeServices)
				// change requests 2, 3 and 4 are OPEN for service 2, see data/vault.sql
				.returns(3L, DashboardSummary::openChangeRequests)
				// the configuration properties should be aggregated accross profiles or services
				.returns(3L, DashboardSummary::activeConfigurations)
				// artifacts 2 through 12 and 14 are owned by namespace_id 2, see data/artifactory.sql
				.returns(12L, DashboardSummary::artifactsOwned)
				.satisfies(summary -> assertThat(summary.members())
						// namespace has 2 members: john (ADMIN) and jane (USER), see data/namespace-members.sql
						.returns(2L, DashboardSummary.Members::count)
						.returns(10L, DashboardSummary.Members::limit)
				);
	}

	@Test
	@DisplayName("should return a null member limit when the namespace has an unlimited member plan")
	void shouldReturnNullMemberLimitForUnlimitedPlan() {
		given(features.get("konfigyr", NamespaceFeatures.MEMBERS_COUNT))
				.willReturn(Optional.of(FeatureValue.unlimited()));

		mvc.get().uri("/namespaces/{slug}/dashboard", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(DashboardSummary.class)
				.satisfies(summary -> assertThat(summary.members())
						.returns(2L, DashboardSummary.Members::count)
						.returns(null, DashboardSummary.Members::limit)
				);
	}

	@Test
	@Transactional
	@DisplayName("should count open change requests spread across multiple services via a single joined query")
	void shouldCountOpenChangeRequestsAcrossServices() {
		given(features.get("konfigyr", NamespaceFeatures.MEMBERS_COUNT))
				.willReturn(Optional.of(FeatureValue.unlimited()));

		// service 3 ("konfigyr-api") belongs to the same "konfigyr" namespace as service 2, but has no
		// change requests of its own in data/vault.sql; seed one here to prove the count is a single
		// query joined across every service in the namespace, not a per-service fan-out.
		context.insertInto(VAULT_PROFILES)
				.set(VAULT_PROFILES.ID, 999L)
				.set(VAULT_PROFILES.SERVICE_ID, 3L)
				.set(VAULT_PROFILES.SLUG, "extra")
				.set(VAULT_PROFILES.NAME, "Extra")
				.set(VAULT_PROFILES.STATE, "ACTIVE")
				.set(VAULT_PROFILES.POLICY, "UNPROTECTED")
				.execute();

		context.insertInto(VAULT_CHANGE_REQUESTS)
				.set(VAULT_CHANGE_REQUESTS.ID, 999L)
				.set(VAULT_CHANGE_REQUESTS.SERVICE_ID, 3L)
				.set(VAULT_CHANGE_REQUESTS.PROFILE_ID, 999L)
				.set(VAULT_CHANGE_REQUESTS.NUMBER, 1L)
				.set(VAULT_CHANGE_REQUESTS.STATE, "OPEN")
				.set(VAULT_CHANGE_REQUESTS.MERGE_STATUS, "NOT_APPROVED")
				.set(VAULT_CHANGE_REQUESTS.CHANGE_COUNT, 1)
				.set(VAULT_CHANGE_REQUESTS.BRANCH_NAME, "refs/cr/999")
				.set(VAULT_CHANGE_REQUESTS.BASE_REVISION, "rev-x1")
				.set(VAULT_CHANGE_REQUESTS.HEAD_REVISION, "rev-x2")
				.set(VAULT_CHANGE_REQUESTS.SUBJECT, "Extra change")
				.set(VAULT_CHANGE_REQUESTS.CREATED_BY, "Test User")
				.execute();

		mvc.get().uri("/namespaces/{slug}/dashboard", "konfigyr")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(DashboardSummary.class)
				// 3 pre-existing OPEN requests for service 2, plus the one just added for service 3
				.returns(4L, DashboardSummary::openChangeRequests);
	}

	@Test
	@DisplayName("should return zero counts for a namespace with no services, change requests or properties")
	void shouldReturnZeroCountsForNamespaceWithNoActivity() {
		mvc.get().uri("/namespaces/{slug}/dashboard", "ebf")
				.with(authentication(TestPrincipals.max(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(DashboardSummary.class)
				.returns(0L, DashboardSummary::activeServices)
				.returns(0L, DashboardSummary::openChangeRequests)
				.returns(0L, DashboardSummary::activeConfigurations)
				// artifacts 16 and 17 are owned by namespace_id 3, see data/artifact-ownership-transfers.sql
				.returns(2L, DashboardSummary::artifactsOwned)
				.satisfies(summary -> assertThat(summary.members())
						.returns(1L, DashboardSummary.Members::count)
				);
	}

	@Test
	@DisplayName("should return not found for an unknown namespace")
	void shouldReturnNotFoundForUnknownNamespace() {
		mvc.get().uri("/namespaces/{slug}/dashboard", "unknown")
				.with(authentication(TestPrincipals.john(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(namespaceNotFound("unknown"));
	}

	@Test
	@DisplayName("should deny access when user is not a namespace member")
	void shouldDenyAccessForNonMembers() {
		mvc.get().uri("/namespaces/{slug}/dashboard", "john-doe")
				.with(authentication(TestPrincipals.jane(), OAuthScope.READ_NAMESPACES))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden());
	}

	@Test
	@DisplayName("should deny access when namespaces:read scope is missing")
	void shouldDenyAccessWithoutScope() {
		mvc.get().uri("/namespaces/{slug}/dashboard", "konfigyr")
				.with(authentication(TestPrincipals.john()))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(forbidden(OAuthScope.READ_NAMESPACES));
	}

	@Test
	@DisplayName("should deny access for unauthenticated requests")
	void shouldDenyUnauthenticatedAccess() {
		mvc.get().uri("/namespaces/{slug}/dashboard", "konfigyr")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(unauthorized());
	}

	private void insertActiveProperty(long profileId, String name) {
		context.insertInto(VAULT_PROPERTIES)
				.set(VAULT_PROPERTIES.NAMESPACE_ID, 2L)
				.set(VAULT_PROPERTIES.SERVICE_ID, 2L)
				.set(VAULT_PROPERTIES.PROFILE_ID, profileId)
				.set(VAULT_PROPERTIES.NAME, name)
				.set(VAULT_PROPERTIES.REVISION, "dashboard-test-revision")
				.set(VAULT_PROPERTIES.CHECKSUM, "checksum")
				.set(VAULT_PROPERTIES.AUTHOR_ID, "john.doe@konfigyr.com")
				.set(VAULT_PROPERTIES.AUTHOR_TYPE, "USER")
				.set(VAULT_PROPERTIES.AUTHOR_NAME, "John Doe")
				.execute();
	}

}
