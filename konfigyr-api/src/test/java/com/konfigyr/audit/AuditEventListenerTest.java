package com.konfigyr.audit;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountEvent;
import com.konfigyr.artifactory.*;
import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.entity.EntityId;
import com.konfigyr.kms.KeysetManagementEvent;
import com.konfigyr.membership.InvitationEvent;
import com.konfigyr.namespace.*;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.test.TestPrincipals;
import com.konfigyr.vault.*;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
				.returns("account.updated", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Account was updated"));
	}

	@Test
	@DisplayName("should persist audit record for account deleted event")
	void shouldAuditAccountDeleted() {
		setSecurityContext(TestPrincipals.jane());

		listener.on(new AccountEvent.Deleted(EntityId.from(601)));

		assertAuditRecord("account", EntityId.from(601))
				.returns(null, AuditRecord::namespaceId)
				.returns("account.deleted", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Account was deleted"));
	}

	@Test
	@DisplayName("should persist audit record for namespace created event")
	void shouldAuditNamespaceCreated() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(700)).when(namespace).id();

		listener.on(new NamespaceEvent.Created(namespace));

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

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(701)).when(namespace).id();

		listener.on(new NamespaceEvent.Renamed(
				namespace,
				Slug.slugify("old-name"),
				Slug.slugify("new-name")
		));

		assertAuditRecord("namespace", EntityId.from(701))
				.returns(EntityId.from(701), AuditRecord::namespaceId)
				.returns("namespace", AuditRecord::entityType)
				.returns(EntityId.from(701), AuditRecord::entityId)
				.returns("namespace.renamed", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Namespace was renamed from '%s' to '%s'", "old-name", "new-name"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("from", "old-name")
						.containsEntry("to", "new-name")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace delete event")
	void shouldAuditNamespaceRemoved() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(701)).when(namespace).id();

		listener.on(new NamespaceEvent.Deleted(namespace));

		assertAuditRecord("namespace", EntityId.from(701))
				.returns(EntityId.from(701), AuditRecord::namespaceId)
				.returns("namespace.deleted", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Namespace was deleted"));
	}

	@Test
	@DisplayName("should persist audit record for namespace member added event")
	void shouldAuditNamespaceMemberAdded() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(702)).when(namespace).id();

		listener.on(new NamespaceEvent.MemberAdded(namespace, EntityId.from(50), NamespaceRole.ADMIN));

		assertAuditRecord("namespace", EntityId.from(702), "namespace.member-added")
				.returns(EntityId.from(702), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Member was added with %s role", NamespaceRole.ADMIN))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("account", EntityId.from(50))
						.containsEntry("role", "ADMIN")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace member updated event")
	void shouldAuditNamespaceMemberUpdated() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(703)).when(namespace).id();

		listener.on(new NamespaceEvent.MemberUpdated(namespace, EntityId.from(50), NamespaceRole.USER));

		assertAuditRecord("namespace", EntityId.from(703), "namespace.member-updated")
				.satisfies(assertAuditRecordMessage("Member role was changed to %s", NamespaceRole.USER))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("account", EntityId.from(50))
						.containsEntry("role", "USER")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace member removed event")
	void shouldAuditNamespaceMemberRemoved() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(703)).when(namespace).id();

		listener.on(new NamespaceEvent.MemberRemoved(namespace, EntityId.from(51)));

		assertAuditRecord("namespace", EntityId.from(703), "namespace.member-removed")
				.satisfies(assertAuditRecordMessage("Member was removed"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("account", EntityId.from(51))
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation created event")
	void shouldAuditInvitationCreated() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(800)).when(namespace).id();

		listener.on(new InvitationEvent.Created(namespace, "inv-key-123"));

		assertAuditRecord("invitation", EntityId.from(800))
				.returns(EntityId.from(800), AuditRecord::namespaceId)
				.returns("invitation.created", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Invitation was sent"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-123")
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation accepted event")
	void shouldAuditInvitationAccepted() {
		setSecurityContext(TestPrincipals.john());

		final var recipient = mock(Account.class);
		doReturn("Jane Doe").when(recipient).displayName();

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(801)).when(namespace).id();

		listener.on(new InvitationEvent.Accepted(namespace, recipient, "inv-key-456"));

		assertAuditRecord("invitation", EntityId.from(801))
				.returns("invitation.accepted", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Invitation was accepted"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-456")
						.containsEntry("recipient", "Jane Doe")
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation declined event")
	void shouldAuditInvitationDeclined() {
		setSecurityContext(TestPrincipals.john());

		final var recipient = mock(Account.class);
		doReturn("John Doe").when(recipient).displayName();

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(801)).when(namespace).id();

		listener.on(new InvitationEvent.Declined(namespace, recipient, "inv-key-456"));

		assertAuditRecord("invitation", EntityId.from(801))
				.returns("invitation.declined", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Invitation was declined"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-456")
						.containsEntry("recipient", "John Doe")
				);
	}

	@Test
	@DisplayName("should persist audit record for invitation canceled event")
	void shouldAuditInvitationCanceled() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(801)).when(namespace).id();

		listener.on(new InvitationEvent.Canceled(namespace, "inv-key-789"));

		assertAuditRecord("invitation", EntityId.from(801))
				.returns("invitation.canceled", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Invitation was canceled"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", "inv-key-789")
				);
	}

	@Test
	@DisplayName("should persist audit record for namespace application created event")
	void shouldAuditNamespaceApplicationCreatedEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var application = mock(NamespaceApplication.class);
		doReturn(EntityId.from(9001)).when(application).id();
		doReturn("created app").when(application).name();

		listener.on(new NamespaceEvent.ApplicationCreated(namespace, application));

		assertAuditRecord("namespace-application", EntityId.from(9001))
				.returns("namespace.application-created", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.returns(Map.of("name", "created app"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Application '%s' was created", application.name()));
	}

	@Test
	@DisplayName("should persist audit record for namespace application updated event")
	void shouldAuditNamespaceApplicationUpdatedEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var application = mock(NamespaceApplication.class);
		doReturn(EntityId.from(9002)).when(application).id();
		doReturn("updated app").when(application).name();

		listener.on(new NamespaceEvent.ApplicationUpdated(namespace, application));

		assertAuditRecord("namespace-application", EntityId.from(9002))
				.returns("namespace.application-updated", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.returns(Map.of("name", "updated app"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Application '%s' was updated", application.name()));
	}

	@Test
	@DisplayName("should persist audit record for namespace application reset event")
	void shouldAuditNamespaceApplicationResetEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var application = mock(NamespaceApplication.class);
		doReturn(EntityId.from(9003)).when(application).id();
		doReturn("namespace app").when(application).name();

		listener.on(new NamespaceEvent.ApplicationReset(namespace, application));

		assertAuditRecord("namespace-application", EntityId.from(9003))
				.returns("namespace.application-reset", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.returns(Map.of("name", "namespace app"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Credentials were reset for '%s' application", application.name()));
	}

	@Test
	@DisplayName("should persist audit record for namespace application removed event")
	void shouldAuditNamespaceApplicationRemovedEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var application = mock(NamespaceApplication.class);
		doReturn(EntityId.from(9004)).when(application).id();
		doReturn("removed app").when(application).name();

		listener.on(new NamespaceEvent.ApplicationRemoved(namespace, application));

		assertAuditRecord("namespace-application", EntityId.from(9004))
				.returns("namespace.application-removed", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.returns(Map.of("name", "removed app"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Application '%s' was removed", application.name()));
	}

	@Test
	@DisplayName("should persist audit record for namespace trusted issuer created event")
	void shouldAuditNamespaceTrustedIssuerCreatedEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var issuer = mock(NamespaceTrustedIssuer.class);
		doReturn(EntityId.from(9010)).when(issuer).id();
		doReturn("Konfigyr CI").when(issuer).name();
		doReturn("https://ci.konfigyr.com").when(issuer).issuerUri();

		listener.on(new NamespaceEvent.TrustedIssuerCreated(namespace, issuer));

		assertAuditRecord("namespace-trusted-issuer", EntityId.from(9010))
				.returns("namespace.trusted-issuer-created", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("name", "Konfigyr CI")
						.containsEntry("issuerUri", "https://ci.konfigyr.com")
				)
				.satisfies(assertAuditRecordMessage("Trusted issuer '%s' has been registered with issuer URI: %s", issuer.name(), issuer.issuerUri()));
	}

	@Test
	@DisplayName("should persist audit record for namespace trusted issuer updated event")
	void shouldAuditNamespaceTrustedIssuerUpdatedEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var issuer = mock(NamespaceTrustedIssuer.class);
		doReturn(EntityId.from(9011)).when(issuer).id();
		doReturn("Konfigyr CI updated").when(issuer).name();
		doReturn("https://ci.konfigyr.com").when(issuer).issuerUri();

		listener.on(new NamespaceEvent.TrustedIssuerUpdated(namespace, issuer));

		assertAuditRecord("namespace-trusted-issuer", EntityId.from(9011))
				.returns("namespace.trusted-issuer-updated", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("name", "Konfigyr CI updated")
						.containsEntry("issuerUri", "https://ci.konfigyr.com")
				)
				.satisfies(assertAuditRecordMessage("Trusted issuer '%s' has been updated with issuer URI: %s", issuer.name(), issuer.issuerUri()));
	}

	@Test
	@DisplayName("should persist audit record for namespace trusted issuer removed event")
	void shouldAuditNamespaceTrustedIssuerRemovedEvent() {
		setSecurityContext(TestPrincipals.john());

		final var namespace = mock(Namespace.class);
		doReturn(EntityId.from(900)).when(namespace).id();

		final var issuer = mock(NamespaceTrustedIssuer.class);
		doReturn(EntityId.from(9012)).when(issuer).id();
		doReturn("Disabled issuer").when(issuer).name();
		doReturn("https://disabled.konfigyr.com").when(issuer).issuerUri();

		listener.on(new NamespaceEvent.TrustedIssuerRemoved(namespace, issuer));

		assertAuditRecord("namespace-trusted-issuer", EntityId.from(9012))
				.returns("namespace.trusted-issuer-removed", AuditRecord::eventType)
				.returns(EntityId.from(900), AuditRecord::namespaceId)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("name", "Disabled issuer")
						.containsEntry("issuerUri", "https://disabled.konfigyr.com")
				)
				.satisfies(assertAuditRecordMessage("Trusted issuer '%s' has been removed (issuer URI: %s)", issuer.name(), issuer.issuerUri()));
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
				.returns("service.created", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Service was created"));
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
				.satisfies(assertAuditRecordMessage("Service was renamed from '%s' to '%s'", "old-slug", "new-slug"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("from", "old-slug")
						.containsEntry("to", "new-slug")
				);
	}

	@Test
	@DisplayName("should persist audit record for service released event with empty artifact coordinates")
	void shouldAuditEmptyServiceReleasedManifest() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(902L)
				.namespace(10L)
				.slug("api-service")
				.name("API Service")
				.build();

		final var manifest = mock(Manifest.class);
		doReturn(Collections.emptyList()).when(manifest).artifacts();

		listener.on(new ServiceEvent.Released(service, manifest));

		assertAuditRecord("service", EntityId.from(902))
				.returns(EntityId.from(10), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Service was released"))
				.returns("service.released", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("artifacts", List.of())
				);
	}

	@Test
	@DisplayName("should persist audit record for service released event with artifact coordinates")
	void shouldAuditServiceReleasedManifest() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(902L)
				.namespace(10L)
				.slug("api-service")
				.name("API Service")
				.build();

		final var manifest = mock(Manifest.class);
		doReturn(Arrays.asList(
				ManifestEntry.builder()
						.artifact(Artifact.of("com.konfigyr", "konfigyr-api", "1.0.0"))
						.checksum("konfigyr-api-checksum")
						.source(ArtifactSource.ARTIFACTORY)
						.resolvedAt(Instant.EPOCH)
						.build(),
				ManifestEntry.builder()
						.artifact(Artifact.of("com.konfigyr", "konfigyr-identity", "1.0.0"))
						.checksum("konfigyr-identity-checksum")
						.source(ArtifactSource.ARTIFACTORY)
						.resolvedAt(Instant.EPOCH)
						.build()
		)).when(manifest).artifacts();

		listener.on(new ServiceEvent.Released(service, manifest));

		assertAuditRecord("service", EntityId.from(902))
				.returns(EntityId.from(10), AuditRecord::namespaceId)
				.returns("service.released", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Service was released"))
				.satisfies(it -> assertThat(it.details())
						.containsEntry("artifacts", List.of(
								"com.konfigyr:konfigyr-api:1.0.0",
								"com.konfigyr:konfigyr-identity:1.0.0"
						))
				);
	}

	@Test
	@DisplayName("should persist audit record for service release failed event with errors")
	void shouldAuditReleaseFailed() {
		setSecurityContext(TestPrincipals.john());

		final Service service = Service.builder()
				.id(903L)
				.namespace(18L)
				.slug("api-service")
				.name("API Service")
				.build();

		listener.on(new ServiceEvent.ReleaseFailed(service, List.of("Release error")));

		assertAuditRecord("service", EntityId.from(903))
				.returns(EntityId.from(18), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Service release failed"))
				.returns("service.release-failed", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("errors", List.of("Release error"))
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
				.returns("service.deleted", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Service was deleted"));
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

		assertAuditRecord("profile", EntityId.from(1000), "profile.created")
				.returns(EntityId.from(1), AuditRecord::namespaceId)
				.returns("profile", AuditRecord::entityType)
				.returns(EntityId.from(1000), AuditRecord::entityId)
				.returns("profile.created", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Profile '%s' was created", profile.slug()))
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

		assertAuditRecord("profile", EntityId.from(1001), "profile.updated")
				.returns(EntityId.from(2), AuditRecord::namespaceId)
				.returns("profile", AuditRecord::entityType)
				.returns(EntityId.from(1001), AuditRecord::entityId)
				.returns("profile.updated", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Profile '%s' was updated", profile.slug()))
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

		assertAuditRecord("profile", EntityId.from(1001), "profile.deleted")
				.returns(EntityId.from(2), AuditRecord::namespaceId)
				.returns("profile", AuditRecord::entityType)
				.returns(EntityId.from(1001), AuditRecord::entityId)
				.returns("profile.deleted", AuditRecord::eventType)
				.satisfies(assertAuditRecordMessage("Profile '%s' was deleted", profile.slug()))
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
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "profile")
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, profile.id())
						.build(),
				CursorPageable.unpaged()
		)).isEmpty();
	}

	@Test
	@DisplayName("should persist audit record for vault changes applied event")
	void shouldAuditVaultChangesApplied() {
		setSecurityContext(TestPrincipals.john());

		final var author = TestAccounts.jane().build();

		final var profile = mock(Profile.class);
		doReturn(EntityId.from(4)).when(profile).id();
		doReturn(EntityId.from(2)).when(profile).service();

		final ApplyResult result = mock(ApplyResult.class);
		doReturn("new-profile-revision").when(result).revision();
		doReturn(TestPrincipals.from(author).getPrincipal()).when(result).author();

		listener.on(new VaultEvent.ChangesApplied(profile, result));

		assertAuditRecord("profile", EntityId.from(4), "profile.changes-applied")
				.returns(EntityId.from(2), AuditRecord::namespaceId)
				.returns("profile", AuditRecord::entityType)
				.returns(EntityId.from(4), AuditRecord::entityId)
				.returns("profile.changes-applied", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.actor())
						.returns(author.id().serialize(), Actor::id)
						.returns(PrincipalType.USER_ACCOUNT.name(), Actor::type)
						.returns(author.displayName(), Actor::name)
				)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("revision", "new-profile-revision")
				);
	}

	@Test
	@DisplayName("should fail to persist audit record for vault event when namespace can not be resolved")
	void shouldAuditVaultEventWithoutNamespace() {
		setSecurityContext(TestPrincipals.john());

		final var profile = mock(Profile.class);
		doReturn(EntityId.from(9999)).when(profile).id();

		listener.on(new VaultEvent.ChangesApplied(profile, mock(ApplyResult.class)));

		assertThat(repository.find(
				SearchQuery.builder()
						.criteria(AuditRecord.ENTITY_TYPE_CRITERIA, "profile")
						.criteria(AuditRecord.ENTITY_ID_CRITERIA, EntityId.from(9999))
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
				.returns(Map.of(), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Keyset was created"));
	}

	@Test
	@DisplayName("should persist audit record for keyset rotated event")
	void shouldAuditKeysetRotated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Rotated(EntityId.from(1101), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1101))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.rotated", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Keyset was rotated"));
	}

	@Test
	@DisplayName("should persist audit record for keyset deleted event")
	void shouldAuditKeysetDeleted() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Deleted(EntityId.from(1102), EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1102))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.deleted", AuditRecord::eventType)
				.returns(Map.of(), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Keyset was successfully removed"));
	}

	@Test
	@DisplayName("should persist audit record for keyset reactivated event")
	void shouldAuditKeysetReactivated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Reactivated(EntityId.from(1103), "5767", EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1103))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.reactivated", AuditRecord::eventType)
				.returns(Map.of("key", "5767"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Key '%s' within a keyset was reactivated", "5767"));
	}

	@Test
	@DisplayName("should persist audit record for keyset deactivated event")
	void shouldAuditKeysetDeactivated() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Deactivated(EntityId.from(1104), "95672", EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1104))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.deactivated", AuditRecord::eventType)
				.returns(Map.of("key", "95672"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Key '%s' within a keyset was disabled", "95672"));
	}

	@Test
	@DisplayName("should persist audit record for keyset compromised event")
	void shouldAuditKeysetCompromised() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Compromised(EntityId.from(1105), "1245", EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1105))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.compromised", AuditRecord::eventType)
				.returns(Map.of("key", "1245"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Key '%s' within a keyset was marked as compromised", "1245"));
	}

	@Test
	@DisplayName("should persist audit record for keyset restored event")
	void shouldAuditKeysetRestored() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Restored(EntityId.from(1105), "53678124", EntityId.from(12)));

		assertAuditRecord("keyset", EntityId.from(1105))
				.returns(EntityId.from(12), AuditRecord::namespaceId)
				.returns("keyset.restored", AuditRecord::eventType)
				.returns(Map.of("key", "53678124"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Key '%s' within a keyset was restored", "53678124"));
	}

	@Test
	@DisplayName("should persist audit record for keyset destroyed event")
	void shouldAuditKeysetDestroyed() {
		setSecurityContext(TestPrincipals.john());

		listener.on(new KeysetManagementEvent.Destroyed(EntityId.from(1106), "596255", EntityId.from(30)));

		assertAuditRecord("keyset", EntityId.from(1106))
				.returns(EntityId.from(30), AuditRecord::namespaceId)
				.returns("keyset.destroyed", AuditRecord::eventType)
				.returns(Map.of("key", "596255"), AuditRecord::details)
				.satisfies(assertAuditRecordMessage("Key '%s' within a keyset was scheduled for destruction", "596255"));
	}

	@Test
	@DisplayName("should persist audit record for artifact publication created event")
	void shouldAuditPublicationCreated() {
		setSecurityContext(TestPrincipals.application("test-oauth-application"));

		final var owner = Owners.konfigyr();
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		listener.on(new ArtifactoryEvent.PublicationCreated(EntityId.from(1200), owner, coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1200))
				.returns(owner.id(), AuditRecord::namespaceId)
				.returns("artifact-version", AuditRecord::entityType)
				.returns("artifact-version.publication-created", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("coordinates", coordinates.format())
				)
				.satisfies(it -> assertThat(it.actor())
						.returns("test-oauth-application", Actor::id)
						.returns(PrincipalType.OAUTH_CLIENT.name(), Actor::type)
						.returns("Test application: test-oauth-application", Actor::name)
				)
				.satisfies(assertAuditRecordMessage("Artifact publication started for %s", coordinates.format()));
	}

	@Test
	@DisplayName("should persist audit record for artifact publication completed event")
	void shouldAuditPublicationCompleted() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		final var owner = Owners.konfigyr();
		listener.on(new ArtifactoryEvent.PublicationCompleted(EntityId.from(1201), owner, coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1201))
				.returns(owner.id(), AuditRecord::namespaceId)
				.returns("artifact-version.publication-completed", AuditRecord::eventType)
				.returns(AuditEventListener.SYSTEM_ACTOR, AuditRecord::actor)
				.satisfies(assertAuditRecordMessage("Artifact %s has been successfully published", coordinates.format()));
	}

	@Test
	@DisplayName("should persist audit record for artifact publication failed event")
	void shouldAuditPublicationFailed() {
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		final var owner = Owners.konfigyr();
		listener.on(new ArtifactoryEvent.PublicationFailed(EntityId.from(1202), owner, coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1202))
				.returns(owner.id(), AuditRecord::namespaceId)
				.returns("artifact-version.publication-failed", AuditRecord::eventType)
				.returns(AuditEventListener.SYSTEM_ACTOR, AuditRecord::actor)
				.satisfies(assertAuditRecordMessage("Failed to publish %s Artifact", coordinates.format()));
	}

	@Test
	@DisplayName("should persist audit record for artifact publication retracted event")
	void shouldAuditPublicationRetracted() {
		final var owner = Owners.konfigyr();
		final var coordinates = ArtifactCoordinates.of("com.konfigyr", "konfigyr-core", "2.0.0");

		listener.on(new ArtifactoryEvent.PublicationRetracted(EntityId.from(1203), owner, coordinates));

		assertAuditRecord("artifact-version", EntityId.from(1203))
				.returns(owner.id(), AuditRecord::namespaceId)
				.returns("artifact-version.publication-retracted", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("coordinates", coordinates.format())
				)
				.satisfies(assertAuditRecordMessage("Artifact %s was retracted", coordinates.format()));
	}

	@Test
	@DisplayName("should persist audit record for artifact deregistered event")
	void shouldAuditArtifactDeregistered() {
		final var owner = Owners.konfigyr();
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-core");

		listener.on(new ArtifactoryEvent.Deregistered(EntityId.from(1204), owner, key));

		assertAuditRecord("artifact-definition", EntityId.from(1204))
				.returns(owner.id(), AuditRecord::namespaceId)
				.returns("artifact-definition.deregistered", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", key.format())
				)
				.satisfies(assertAuditRecordMessage("Artifact %s was deregistered", key.format()));
	}

	@Test
	@DisplayName("should persist audit record for artifact visibility changed event")
	void shouldAuditArtifactVisibilityChanged() {
		final var owner = Owners.konfigyr();
		final var key = ArtifactKey.of("com.konfigyr", "konfigyr-core");

		listener.on(new ArtifactoryEvent.VisibilityChanged(EntityId.from(1205), owner, key, ArtifactVisibility.PUBLIC));

		assertAuditRecord("artifact-definition", EntityId.from(1205))
				.returns(owner.id(), AuditRecord::namespaceId)
				.returns("artifact-definition.visibility-changed", AuditRecord::eventType)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("key", key.format())
						.containsEntry("visibility", ArtifactVisibility.PUBLIC.name())
				)
				.satisfies(assertAuditRecordMessage("Visibility of artifact %s was changed to %s", key.format(), ArtifactVisibility.PUBLIC.name()));
	}

	@Test
	@DisplayName("should persist two audit records for a requested ownership transfer, one per namespace")
	void shouldAuditOwnershipTransferRequested() {
		final var from = new Owner(EntityId.from(1210), "from-namespace");
		final var to = new Owner(EntityId.from(1211), "to-namespace");

		listener.on(new ArtifactoryEvent.OwnershipTransferRequested(EntityId.from(1200), "com.konfigyr", from, to));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1200), "artifact-ownership-transfer.sent")
				.returns(to.id(), AuditRecord::namespaceId)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("groupId", "com.konfigyr")
						.containsEntry("from", from.slug())
						.containsEntry("to", to.slug())
				)
				.satisfies(assertAuditRecordMessage("Ownership transfer request for '%s' was successfully sent to '%s'", "com.konfigyr", from.slug()));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1200), "artifact-ownership-transfer.requested")
				.returns(from.id(), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Ownership transfer for '%s' was requested from '%s'", "com.konfigyr", to.slug()));
	}

	@Test
	@DisplayName("should persist two audit records for an accepted ownership transfer, one per namespace")
	void shouldAuditOwnershipTransferAccepted() {
		final var from = new Owner(EntityId.from(1210), "from-namespace");
		final var to = new Owner(EntityId.from(1211), "to-namespace");

		listener.on(new ArtifactoryEvent.OwnershipTransferAccepted(EntityId.from(1212), "com.konfigyr", from, to));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1212), "artifact-ownership-transfer.received")
				.returns(to.id(), AuditRecord::namespaceId)
				.satisfies(it -> assertThat(it.details())
						.containsEntry("groupId", "com.konfigyr")
						.containsEntry("from", from.slug())
						.containsEntry("to", to.slug())
				)
				.satisfies(assertAuditRecordMessage("Ownership of '%s' was transferred from '%s'", "com.konfigyr", from.slug()));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1212), "artifact-ownership-transfer.transferred")
				.returns(from.id(), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Ownership of '%s' was transferred to '%s'", "com.konfigyr", to.slug()));
	}

	@Test
	@DisplayName("should persist two audit records for a rejected ownership transfer, one per namespace")
	void shouldAuditOwnershipTransferRejected() {
		final var from = new Owner(EntityId.from(1220), "from-namespace");
		final var to = new Owner(EntityId.from(1221), "to-namespace");

		listener.on(new ArtifactoryEvent.OwnershipTransferRejected(EntityId.from(1222), "com.konfigyr", from, to));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1222), "artifact-ownership-transfer.request-rejected")
				.returns(to.id(), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Request to transfer '%s' was rejected by '%s'", "com.konfigyr", from.slug()));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1222), "artifact-ownership-transfer.rejected")
				.returns(from.id(), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Transfer request for '%s' from '%s' was rejected", "com.konfigyr", to.slug()));
	}

	@Test
	@DisplayName("should persist two audit records for a cancelled ownership transfer, one per namespace")
	void shouldAuditOwnershipTransferCancelled() {
		final var from = new Owner(EntityId.from(1230), "from-namespace");
		final var to = new Owner(EntityId.from(1231), "to-namespace");

		listener.on(new ArtifactoryEvent.OwnershipTransferCancelled(EntityId.from(1232), "com.konfigyr", from, to));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1232), "artifact-ownership-transfer.cancelled")
				.returns(to.id(), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Request to transfer '%s' was cancelled", "com.konfigyr"));

		assertAuditRecord("artifact-ownership-transfer", EntityId.from(1232), "artifact-ownership-transfer.request-cancelled")
				.returns(from.id(), AuditRecord::namespaceId)
				.satisfies(assertAuditRecordMessage("Transfer request for '%s' from '%s' was cancelled", "com.konfigyr", to.slug()));
	}

	@Test
	@DisplayName("should not propagate persistence failures from the audit event listener")
	void shouldNotPropagatePersistenceFailures() {
		final var repository = mock(AuditEventRepository.class);
		doThrow(new RuntimeException("DB down")).when(repository).insert(any());

		final var listener = new AuditEventListener(repository, mock(), ObservationRegistry.NOOP);

		assertThatNoException()
				.isThrownBy(() -> listener.on(new AccountEvent.Updated(EntityId.from(999))));

		verify(repository).insert(any());
	}

	@Test
	@DisplayName("should observe audit event listener")
	void shouldObserveEventListener() {
		final var registry = TestObservationRegistry.create();
		final var listener = new AuditEventListener(repository, mock(), registry);

		assertThatNoException()
				.isThrownBy(() -> listener.on(new AccountEvent.Deleted(EntityId.from(1))));

		assertThat(registry)
				.hasObservationWithNameEqualTo(AuditObservation.OBSERVATION_NAME)
				.that()
				.hasBeenStarted()
				.hasBeenStopped()
				.doesNotHaveError()
				.hasHighCardinalityKeyValue("konfigyr.audit.event.id", EntityId.from(1).serialize())
				.hasLowCardinalityKeyValue("konfigyr.audit.event.type", "accounts.deleted")
				.hasContextualNameEqualTo("audit listener event with '%s' type and '%s' resource identifier"
						.formatted("accounts.deleted", EntityId.from(1).serialize()));
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

	private static Consumer<AuditRecord> assertAuditRecordMessage(String message, Object... args) {
		return record -> {
			assertThat(record.message())
					.as("Message bundle should contain an entry for event type: %s", record.eventType())
					.isNotEqualTo(record.eventType());

			assertThat(record.message())
					.as("Audit record message bundle value mismatch")
					.isEqualTo(message, args);
		};
	}

}
