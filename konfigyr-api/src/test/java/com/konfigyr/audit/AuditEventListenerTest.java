package com.konfigyr.audit;

import com.konfigyr.account.AccountEvent;
import com.konfigyr.artifactory.Artifact;
import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.artifactory.Manifest;
import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.entity.EntityId;
import com.konfigyr.kms.KeysetManagementEvent;
import com.konfigyr.namespace.*;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileEvent;
import com.konfigyr.vault.ProfilePolicy;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@Transactional
class AuditEventListenerTest extends AbstractIntegrationTest {

	@Autowired
	AuditEventListener listener;

	@Autowired
	AuditEventRepository repository;

	@AfterEach
	void cleanup() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("should resolve actor from the security context when an authenticated principal is present")
	void shouldResolveActorFromSecurityContext() {
		final var account = TestAccounts.john().build();

		setSecurityContext(TestPrincipals.from(account));

		listener.on(new AccountEvent.Updated(EntityId.from(500)));

		assertAuditRecord("account", EntityId.from(500))
				.extracting(AuditRecord::actor)
				.returns(account.id().serialize(), Actor::id)
				.returns(PrincipalType.USER_ACCOUNT.name(), Actor::type)
				.returns(account.displayName(), Actor::name);
	}

	@Test
	@DisplayName("should fall back to system actor when no security context is present")
	void shouldFallBackToSystemActor() {
		listener.on(new AccountEvent.Updated(EntityId.from(501)));

		assertAuditRecord("account", EntityId.from(501))
				.extracting(AuditRecord::actor)
				.returns("system", Actor::id)
				.returns(PrincipalType.SYSTEM.name(), Actor::type)
				.returns("Konfigyr", Actor::name)
				.isEqualTo(AuditEventListener.SYSTEM_ACTOR);
	}

	@Test
	@DisplayName("should persist audit record for account updated event")
	void shouldAuditAccountUpdated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new AccountEvent.Updated(EntityId.from(600)));

		assertAuditRecord("account", EntityId.from(600))
				.returns(null, AuditRecord::namespaceId)
				.returns("account", AuditRecord::entityType)
				.returns(EntityId.from(600), AuditRecord::entityId)
				.returns("account.updated", AuditRecord::eventType);
	}

	@Test
	@DisplayName("should persist audit record for account deleted event")
	void shouldAuditAccountDeleted() {
		setSecurityContext(TestPrincipals.jane());

		listener.on(new AccountEvent.Deleted(EntityId.from(601)));

		assertAuditRecord("account", EntityId.from(601))
				.returns(null, AuditRecord::namespaceId)
				.returns("account.deleted", AuditRecord::eventType);
	}

	@Test
	@DisplayName("should persist audit record for namespace created event")
	void shouldAuditNamespaceCreated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new NamespaceEvent.Created(EntityId.from(700)));

		assertAuditRecord("namespace", EntityId.from(700))
				.returns(EntityId.from(700), AuditRecord::namespaceId)
				.returns("namespace", AuditRecord::entityType)
				.returns(EntityId.from(700), AuditRecord::entityId)
				.returns("namespace.created", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for namespace renamed event with from/to details")
	void shouldAuditNamespaceRenamed() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new NamespaceEvent.Renamed(
				EntityId.from(701),
				Slug.slugify("old-name"),
				Slug.slugify("new-name")
		));

		assertAuditRecord("namespace", EntityId.from(701))
				.returns(EntityId.from(701), AuditRecord::namespaceId)
				.returns("namespace", AuditRecord::entityType)
				.returns(EntityId.from(701), AuditRecord::entityId)
				.returns("namespace.renamed", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("from", "old-name")
						.containsEntry("to", "new-name")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace delete event")
	void shouldAuditNamespaceRemoved() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new NamespaceEvent.Deleted(EntityId.from(701)));

		assertAuditRecord("namespace", EntityId.from(701))
				.returns(EntityId.from(701), AuditRecord::namespaceId)
				.returns("namespace.deleted", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for namespace member added event")
	void shouldAuditNamespaceMemberAdded() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new NamespaceEvent.MemberAdded(
				EntityId.from(702), EntityId.from(50), NamespaceRole.ADMIN
		));

		assertAuditRecord("namespace", EntityId.from(702), "namespace.member-added")
				.returns(EntityId.from(702), AuditRecord::namespaceId)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("account", EntityId.from(50))
						.containsEntry("role", "ADMIN")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace member updated event")
	void shouldAuditNamespaceMemberUpdated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new NamespaceEvent.MemberUpdated(
				EntityId.from(703), EntityId.from(50), NamespaceRole.USER
		));

		assertAuditRecord("namespace", EntityId.from(703), "namespace.member-updated")
				.satisfies(it -> assertThat(it.details())
						.containsEntry("account", EntityId.from(50))
						.containsEntry("role", "USER")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace member removed event")
	void shouldAuditNamespaceMemberRemoved() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new NamespaceEvent.MemberRemoved(EntityId.from(703), EntityId.from(51)));

		assertAuditRecord("namespace", EntityId.from(703), "namespace.member-removed")
				.satisfies(it -> assertThat(it.details())
						.containsEntry("account", EntityId.from(51))
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation created event")
	void shouldAuditInvitationCreated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new InvitationEvent.Created(
				EntityId.from(800), "inv-key-123",
				UriComponentsBuilder.fromUriString("https://konfigyr.com").build()
		));

		assertAuditRecord("invitation", EntityId.from(800))
				.returns(EntityId.from(800), AuditRecord::namespaceId)
				.returns("invitation.created", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-123")
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation accepted event")
	void shouldAuditInvitationAccepted() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new InvitationEvent.Accepted(EntityId.from(801), "inv-key-456"));

		assertAuditRecord("invitation", EntityId.from(801))
				.returns("invitation.accepted", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-456")
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation canceled event")
	void shouldAuditInvitationCanceled() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new InvitationEvent.Canceled(EntityId.from(801), "inv-key-789"));

		assertAuditRecord("invitation", EntityId.from(801))
				.returns("invitation.canceled", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-789")
				);
	}

	@Test
	@DisplayName("should persist audit record for service created event")
	void shouldAuditServiceCreated() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(900L)
				.namespace(10L)
				.slug("my-service")
				.name("My Service")
				.build();

		listener.on(new ServiceEvent.Created(service));

		assertAuditRecord("service", EntityId.from(900))
				.returns(EntityId.from(10), AuditRecord::namespaceId)
				.returns("service", AuditRecord::entityType)
				.returns(EntityId.from(900), AuditRecord::entityId)
				.returns("service.created", AuditRecord::eventType);
	}

	@Test
	@DisplayName("should persist audit record for service renamed event with slug details")
	void shouldAuditServiceRenamed() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(901L)
				.namespace(10L)
				.slug("new-slug")
				.name("My Service")
				.build();

		listener.on(new ServiceEvent.Renamed(service, Slug.slugify("old-slug"), Slug.slugify("new-slug")));

		assertAuditRecord("service", EntityId.from(901))
				.returns("service.renamed", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("from", "old-slug")
						.containsEntry("to", "new-slug")
				);
	}

	@Test
	@DisplayName("should persist audit record for service published event with empty artifact coordinates")
	void shouldAuditEmptyServicePublishedManifest() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(902L)
				.namespace(10L)
				.slug("api-service")
				.name("API Service")
				.build();

		final var manifest = mock(Manifest.class);
		doReturn(Collections.emptyList()).when(manifest).artifacts();

		listener.on(new ServiceEvent.Published(service, manifest));

		assertAuditRecord("service", EntityId.from(902))
				.returns(EntityId.from(10), AuditRecord::namespaceId)
				.returns("service.published", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("artifacts", List.of())
				);
	}

	@Test
	@DisplayName("should persist audit record for service published event with artifact coordinates")
	void shouldAuditServicePublishedManifest() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(902L)
				.namespace(10L)
				.slug("api-service")
				.name("API Service")
				.build();

		final var manifest = mock(Manifest.class);
		doReturn(Arrays.asList(
				Artifact.of("com.konfigyr", "konfigyr-api", "1.0.0"),
				Artifact.of("com.konfigyr", "konfigyr-identity", "1.0.0")
		)).when(manifest).artifacts();

		listener.on(new ServiceEvent.Published(service, manifest));

		assertAuditRecord("service", EntityId.from(902))
				.returns(EntityId.from(10), AuditRecord::namespaceId)
				.returns("service.published", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("artifacts", List.of(
								"com.konfigyr:konfigyr-api:1.0.0",
								"com.konfigyr:konfigyr-identity:1.0.0"
						))
				);
	}

	@Test
	@DisplayName("should persist audit record for service deleted event")
	void shouldAuditServiceDeleted() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(903L)
				.namespace(10L)
				.slug("deleted-service")
				.name("Deleted Service")
				.build();

		listener.on(new ServiceEvent.Deleted(service));

		assertAuditRecord("service", EntityId.from(903))
				.returns(EntityId.from(10), AuditRecord::namespaceId)
				.returns("service.deleted", AuditRecord::eventType);
	}

	@Test
	@DisplayName("should persist audit record for profile created event")
	void shouldAuditProfileCreated() {
		setSecurityContext(TestPrincipals.john());

		final Profile profile = Profile.builder()
				.id(EntityId.from(1000))
				.service(EntityId.from(1))
				.slug("production")
				.name("Production")
				.policy(ProfilePolicy.UNPROTECTED)
				.build();

		listener.on(new ProfileEvent.Created(profile));

		assertAuditRecord("service", EntityId.from(1), "profile.created")
				.returns(EntityId.from(1), AuditRecord::namespaceId)
				.returns("service", AuditRecord::entityType)
				.returns(EntityId.from(1), AuditRecord::entityId)
				.returns("profile.created", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("name", "production")
				);
	}

	@Test
	@DisplayName("should persist audit record for profile updated event")
	void shouldAuditProfileUpdated() {
		setSecurityContext(TestPrincipals.jane());

		final Profile profile = Profile.builder()
				.id(EntityId.from(1001))
				.service(EntityId.from(2))
				.slug("production")
				.name("Prod")
				.policy(ProfilePolicy.PROTECTED)
				.build();

		listener.on(new ProfileEvent.Updated(profile));

		assertAuditRecord("service", EntityId.from(2), "profile.updated")
				.returns(EntityId.from(2), AuditRecord::namespaceId)
				.returns("service", AuditRecord::entityType)
				.returns(EntityId.from(2), AuditRecord::entityId)
				.returns("profile.updated", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("name", "production")
				);
	}

	@Test
	@DisplayName("should persist audit record for profile deleted event")
	void shouldAuditProfileDeleted() {
		setSecurityContext(TestPrincipals.john());

		final Profile profile = Profile.builder()
				.id(EntityId.from(1001))
				.service(EntityId.from(3))
				.slug("staging")
				.name("Staging")
				.policy(ProfilePolicy.UNPROTECTED)
				.build();

		listener.on(new ProfileEvent.Deleted(profile));

		assertAuditRecord("service", EntityId.from(3), "profile.deleted")
				.returns(EntityId.from(2), AuditRecord::namespaceId)
				.returns("service", AuditRecord::entityType)
				.returns(EntityId.from(3), AuditRecord::entityId)
				.returns("profile.deleted", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("name", "staging")
				);
	}

	@Test
	@DisplayName("should fail to persist audit record for profile event when namespace can not be resolved for service")
	void shouldAuditProfileEventWithoutNamespace() {
		setSecurityContext(TestPrincipals.john());

		final Profile profile = Profile.builder()
				.id(EntityId.from(1000))
				.service(EntityId.from(1000))
				.slug("unknown")
				.name("unknown")
				.policy(ProfilePolicy.UNPROTECTED)
				.build();

		listener.on(new ProfileEvent.Created(profile));

		assertThat(repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "service")
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, profile.service())
						.build(),
				CursorPageable.unpaged()
		)).isEmpty();
	}

	@Test
	@DisplayName("should persist audit record for keyset created event")
	void shouldAuditKeysetCreated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Created(EntityId.from(1100), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1100))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset", AuditRecord::entityType)
				.returns(EntityId.from(1100), AuditRecord::entityId)
				.returns("keyset.created", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for keyset rotated event")
	void shouldAuditKeysetRotated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Rotated(EntityId.from(1101), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1101))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.rotated", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for keyset disabled event")
	void shouldAuditKeysetDisabled() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Disabled(EntityId.from(1102), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1102))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.disabled", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for keyset activated event")
	void shouldAuditKeysetActivated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Activated(EntityId.from(1103), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1103))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.activated", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for keyset removed event")
	void shouldAuditKeysetRemoved() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Removed(EntityId.from(1104), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1104))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.removed", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for keyset destroyed event")
	void shouldAuditKeysetDestroyed() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Destroyed(EntityId.from(1105), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1105))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.destroyed", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details);
	}

	@Test
	@DisplayName("should persist audit record for artifact release created event")
	void shouldAuditReleaseCreated() {
		setSecurityContext(TestPrincipals.application("test-oauth-application"));

		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		listener.on(new ArtifactoryEvent.ReleaseCreated(EntityId.from(1200), coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1200))
				.returns(null, AuditRecord::namespaceId)
				.returns("artifact-version", AuditRecord::entityType)
				.returns("artifact-version.release-created", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("coordinates", coordinates.format())
				)
				.satisfies(it -> assertThat(it.actor())
						.returns("test-oauth-application", Actor::id)
						.returns(PrincipalType.OAUTH_CLIENT.name(), Actor::type)
						.returns("Test application: test-oauth-application", Actor::name)
				);
	}

	@Test
	@DisplayName("should persist audit record for artifact release completed event")
	void shouldAuditReleaseCompleted() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		listener.on(new ArtifactoryEvent.ReleaseCompleted(EntityId.from(1201), coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1201))
				.returns("artifact-version.release-completed", AuditRecord::eventType)
				.returns(AuditEventListener.SYSTEM_ACTOR, AuditRecord::actor);
	}

	@Test
	@DisplayName("should persist audit record for artifact release failed event")
	void shouldAuditReleaseFailed() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		listener.on(new ArtifactoryEvent.ReleaseFailed(EntityId.from(1202), coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1202))
				.returns("artifact-version.release-failed", AuditRecord::eventType)
				.returns(AuditEventListener.SYSTEM_ACTOR, AuditRecord::actor);
	}

	@Test
	@DisplayName("should not propagate persistence failures from the audit event listener")
	void shouldNotPropagatePersistenceFailures() {
		final var repository = mock(AuditEventRepository.class);
		doThrow(new RuntimeException("DB down")).when(repository).insert(any());

		final var listener = new AuditEventListener(mock(), repository);

		assertThatNoException()
				.isThrownBy(() -> listener.on(new AccountEvent.Updated(EntityId.from(999))));

		verify(repository).insert(any());
	}

	private ObjectAssert<AuditRecord> assertAuditRecord(String entityType, EntityId entityId) {
		return assertAuditRecord(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, entityType)
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, entityId)
						.build()
		);
	}

	private ObjectAssert<AuditRecord> assertAuditRecord(String entityType, EntityId entityId, String eventType) {
		return assertAuditRecord(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, entityType)
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, entityId)
						.criteria(AuditRecord.EVENT_TYPE_CRITERIA, eventType)
						.build()
		);
	}

	private ObjectAssert<AuditRecord> assertAuditRecord(SearchQuery query) {
		final CursorPage<AuditRecord> page = repository.find(query, CursorPageable.unpaged());

		return assertThat(page.content())
				.hasSize(1)
				.first();
	}

	private static void setSecurityContext(Authentication principal) {
		SecurityContextHolder.getContext().setAuthentication(principal);
	}

}
