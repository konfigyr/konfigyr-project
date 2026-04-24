package com.konfigyr.audit;

import com.konfigyr.account.AccountEvent;
import com.konfigyr.artifactory.Artifact;
import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.kms.KeysetManagementEvent;
import com.konfigyr.namespace.*;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.vault.ProfileEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.konfigyr.data.tables.Services.SERVICES;

/**
 * Centralized audit event listener that captures domain events from all bounded contexts
 * and persists them as {@link AuditEvent audit entries}.
 * <p>
 * This component is the single point of audit policy for the Konfigyr platform. It listens
 * to domain events published by the account, namespace, service, invitation, vault, KMS, and
 * artifactory modules, maps each to an {@link AuditEvent}, and delegates persistence to the
 * {@link AuditEventRepository}.
 * <p>
 * Actor attribution is resolved from the current {@link org.springframework.security.core.context.SecurityContext}.
 * For events that occur outside an authenticated context (e.g. background batch jobs), a system
 * actor is used as a fallback.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see AuditEvent
 * @see AuditEventRepository
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class AuditEventListener {

	static final Actor SYSTEM_ACTOR = new Actor("system", PrincipalType.SYSTEM.name(), "Konfigyr");

	private final NamespaceResolver resolver;
	private final AuditEventRepository repository;

	// ── Account events ──────────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.account-updated", classes = AccountEvent.Updated.class)
	void on(AccountEvent.Updated event) {
		insert(event, builder -> builder
				.entityType("account")
				.entityId(event.id())
				.eventType("account.updated")
		);
	}

	@TransactionalEventListener(id = "audit.account-deleted", classes = AccountEvent.Deleted.class)
	void on(AccountEvent.Deleted event) {
		insert(event, builder -> builder
				.entityType("account")
				.entityId(event.id())
				.eventType("account.deleted")
		);
	}

	// ── Namespace events ────────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.namespace-created", classes = NamespaceEvent.Created.class)
	void on(NamespaceEvent.Created event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace")
				.entityId(event.id())
				.eventType("namespace.created")
		);
	}

	@TransactionalEventListener(id = "audit.namespace-renamed", classes = NamespaceEvent.Renamed.class)
	void on(NamespaceEvent.Renamed event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace")
				.entityId(event.id())
				.eventType("namespace.renamed")
				.details("from", event.from().get())
				.details("to", event.to().get())
		);
	}

	@TransactionalEventListener(id = "audit.namespace-deleted", classes = NamespaceEvent.Deleted.class)
	void on(NamespaceEvent.Deleted event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace")
				.entityId(event.id())
				.eventType("namespace.deleted")
		);
	}

	@TransactionalEventListener(id = "audit.namespace-member-added", classes = NamespaceEvent.MemberAdded.class)
	void on(NamespaceEvent.MemberAdded event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace")
				.entityId(event.id())
				.eventType("namespace.member-added")
				.details("account", event.account())
				.details("role", event.role().name())
		);
	}

	@TransactionalEventListener(id = "audit.namespace-member-updated", classes = NamespaceEvent.MemberUpdated.class)
	void on(NamespaceEvent.MemberUpdated event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace")
				.entityId(event.id())
				.eventType("namespace.member-updated")
				.details("account", event.account())
				.details("role", event.role().name())
		);
	}

	@TransactionalEventListener(id = "audit.namespace-member-removed", classes = NamespaceEvent.MemberRemoved.class)
	void on(NamespaceEvent.MemberRemoved event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace")
				.entityId(event.id())
				.eventType("namespace.member-removed")
				.details("account", event.account())
		);
	}

	// ── Namespace invitation events ───────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.invitation-created", classes = InvitationEvent.Created.class)
	void on(InvitationEvent.Created event) {
		insert(event, builder -> builder
				.namespace(event.namespace())
				.entityType("invitation")
				.entityId(event.namespace())
				.eventType("invitation.created")
				.details("key", event.key())
		);
	}

	@TransactionalEventListener(id = "audit.invitation-accepted", classes = InvitationEvent.Accepted.class)
	void on(InvitationEvent.Accepted event) {
		insert(event, builder -> builder
				.namespace(event.namespace())
				.entityType("invitation")
				.entityId(event.namespace())
				.eventType("invitation.accepted")
				.details("key", event.key())
		);
	}

	@TransactionalEventListener(id = "audit.invitation-canceled", classes = InvitationEvent.Canceled.class)
	void on(InvitationEvent.Canceled event) {
		insert(event, builder -> builder
				.namespace(event.namespace())
				.entityType("invitation")
				.entityId(event.namespace())
				.eventType("invitation.canceled")
				.details("key", event.key())
		);
	}

	// ── Service events ──────────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.service-created", classes = ServiceEvent.Created.class)
	void on(ServiceEvent.Created event) {
		insert(event, builder -> builder
				.namespace(event.get().namespace())
				.entityType("service")
				.entityId(event.id())
				.eventType("service.created")
		);
	}

	@TransactionalEventListener(id = "audit.service-renamed", classes = ServiceEvent.Renamed.class)
	void on(ServiceEvent.Renamed event) {
		insert(event, builder -> builder
				.namespace(event.get().namespace())
				.entityType("service")
				.entityId(event.id())
				.eventType("service.renamed")
				.details("from", event.from().get())
				.details("to", event.to().get())
		);
	}

	@TransactionalEventListener(id = "audit.service-published", classes = ServiceEvent.Published.class)
	void on(ServiceEvent.Published event) {
		insert(event, builder -> {
			final List<String> artifacts = new ArrayList<>();

			for (Artifact artifact : event.manifest().artifacts()) {
				artifacts.add(ArtifactCoordinates.of(artifact).format());
			}

			builder.entityType("service")
					.entityId(event.id())
					.eventType("service.published")
					.details("artifacts", Collections.unmodifiableList(artifacts));
		});
	}

	@TransactionalEventListener(id = "audit.service-deleted", classes = ServiceEvent.Deleted.class)
	void on(ServiceEvent.Deleted event) {
		insert(event, builder -> builder
				.entityType("service")
				.entityId(event.id())
				.eventType("service.deleted")
		);
	}

	// ── Vault (Profile) events ──────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.profile-created", classes = ProfileEvent.Created.class)
	void on(ProfileEvent.Created event) {
		insert(event, builder -> builder
				.entityType("service")
				.entityId(event.get().service())
				.eventType("profile.created")
				.details("name", event.get().slug())
		);
	}

	@TransactionalEventListener(id = "audit.profile-updated", classes = ProfileEvent.Updated.class)
	void on(ProfileEvent.Updated event) {
		insert(event, builder -> builder
				.entityType("service")
				.entityId(event.get().service())
				.eventType("profile.updated")
				.details("name", event.get().slug())
		);
	}

	@TransactionalEventListener(id = "audit.profile-deleted", classes = ProfileEvent.Deleted.class)
	void on(ProfileEvent.Deleted event) {
		insert(event, builder -> builder
				.entityType("service")
				.entityId(event.get().service())
				.eventType("profile.deleted")
				.details("name", event.get().slug())
		);
	}

	// ── KMS events ──────────────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.keyset-created", classes = KeysetManagementEvent.Created.class)
	void on(KeysetManagementEvent.Created event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.created")
		);
	}

	@TransactionalEventListener(id = "audit.keyset-rotated", classes = KeysetManagementEvent.Rotated.class)
	void on(KeysetManagementEvent.Rotated event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.rotated")
		);
	}

	@TransactionalEventListener(id = "audit.keyset-disabled", classes = KeysetManagementEvent.Disabled.class)
	void on(KeysetManagementEvent.Disabled event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.disabled")
		);
	}

	@TransactionalEventListener(id = "audit.keyset-activated", classes = KeysetManagementEvent.Activated.class)
	void on(KeysetManagementEvent.Activated event) {
		insert(event, builder -> builder
				.namespace(event.namespace())
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.activated")
		);
	}

	@TransactionalEventListener(id = "audit.keyset-removed", classes = KeysetManagementEvent.Removed.class)
	void on(KeysetManagementEvent.Removed event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.removed")
		);
	}

	@TransactionalEventListener(id = "audit.keyset-destroyed", classes = KeysetManagementEvent.Destroyed.class)
	void on(KeysetManagementEvent.Destroyed event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.destroyed")
		);
	}

	// ── Artifactory events ──────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.release-created", classes = ArtifactoryEvent.ReleaseCreated.class)
	void on(ArtifactoryEvent.ReleaseCreated event) {
		insert(event, builder -> builder
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.release-created")
				.details("coordinates", event.coordinates().format())
		);
	}

	@TransactionalEventListener(id = "audit.release-completed", classes = ArtifactoryEvent.ReleaseCompleted.class)
	void on(ArtifactoryEvent.ReleaseCompleted event) {
		insert(event, builder -> builder
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.release-completed")
				.details("coordinates", event.coordinates().format())
		);
	}

	@TransactionalEventListener(id = "audit.release-failed", classes = ArtifactoryEvent.ReleaseFailed.class)
	void on(ArtifactoryEvent.ReleaseFailed event) {
		insert(event, builder -> builder
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.release-failed")
				.details("coordinates", event.coordinates().format())
		);
	}

	private void insert(EntityEvent event, Consumer<AuditEvent.Builder> factory) {
		try {
			final AuditEvent.Builder builder = AuditEvent.builder()
					.namespace(resolver.resolve(event));

			AuthenticatedPrincipal.fromSecurityContext().ifPresentOrElse(
					builder::actor,
					() -> builder.actor(SYSTEM_ACTOR)
			);

			factory.accept(builder);

			repository.insert(builder.build());
		} catch (Exception ex) {
			log.error("Failed to persist audit event: {}", event, ex);
		}
	}

	/**
	 * Internal helper class used by the {@link AuditEventListener} to resolve namespace identifiers
	 * for different types of events.
	 */
	@RequiredArgsConstructor
	static class NamespaceResolver {

		private final DSLContext context;

		@Nullable
		@Transactional(label = "audit-listener.namespace-resolver", readOnly = true)
		EntityId resolve(EntityEvent event) {
			return switch (event) {
				case NamespaceEvent ne -> ne.id();
				case ServiceEvent se -> se.get().namespace();
				case InvitationEvent ie -> ie.namespace();
				case KeysetManagementEvent kme -> kme.namespace();
				case ProfileEvent pe -> resolve(pe);
				default -> null;
			};
		}

		private EntityId resolve(ProfileEvent event) {
			final EntityId service = event.get().service();

			return context.select(SERVICES.NAMESPACE_ID)
					.from(SERVICES)
					.where(SERVICES.ID.eq(service.get()))
					.fetchOptional(SERVICES.NAMESPACE_ID, EntityId.class)
					.orElseThrow(() -> new NamespaceException("Could not resolve namespace for profile " +
							event.get().slug() + " of service " + service.get()));
		}

	}

}
