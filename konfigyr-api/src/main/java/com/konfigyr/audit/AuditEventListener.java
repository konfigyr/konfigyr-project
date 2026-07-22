package com.konfigyr.audit;

import com.konfigyr.account.AccountEvent;
import com.konfigyr.artifactory.ArtifactCoordinates;
import com.konfigyr.artifactory.ArtifactoryEvent;
import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.kms.KeysetManagementEvent;
import com.konfigyr.membership.InvitationEvent;
import com.konfigyr.namespace.*;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import com.konfigyr.vault.Profile;
import com.konfigyr.vault.ProfileEvent;
import com.konfigyr.vault.VaultEvent;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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

	private final AuditEventRepository auditEventRepository;
	private final NamespaceResolver namespaceResolver;
	private final ObservationRegistry observationRegistry;

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
				.details("recipient", event.recipient().displayName())
		);
	}

	@TransactionalEventListener(id = "audit.invitation-declined", classes = InvitationEvent.Declined.class)
	void on(InvitationEvent.Declined event) {
		insert(event, builder -> builder
				.namespace(event.namespace())
				.entityType("invitation")
				.entityId(event.namespace())
				.eventType("invitation.declined")
				.details("key", event.key())
				.details("recipient", event.recipient().displayName())
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

	// ── Namespace application events ───────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.application-created", classes = NamespaceEvent.ApplicationCreated.class)
	void on(NamespaceEvent.ApplicationCreated event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-application")
				.entityId(event.application().id())
				.eventType("namespace.application-created")
				.details("name", event.application().name())
		);
	}

	@TransactionalEventListener(id = "audit.application-updated", classes = NamespaceEvent.ApplicationUpdated.class)
	void on(NamespaceEvent.ApplicationUpdated event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-application")
				.entityId(event.application().id())
				.eventType("namespace.application-updated")
				.details("name", event.application().name())
		);
	}

	@TransactionalEventListener(id = "audit.application-reset", classes = NamespaceEvent.ApplicationReset.class)
	void on(NamespaceEvent.ApplicationReset event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-application")
				.entityId(event.application().id())
				.eventType("namespace.application-reset")
				.details("name", event.application().name())
		);
	}

	@TransactionalEventListener(id = "audit.application-removed", classes = NamespaceEvent.ApplicationRemoved.class)
	void on(NamespaceEvent.ApplicationRemoved event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-application")
				.entityId(event.application().id())
				.eventType("namespace.application-removed")
				.details("name", event.application().name())
		);
	}

	// ── Namespace trusted issuer events ─────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.trusted-issuer-created", classes = NamespaceEvent.TrustedIssuerCreated.class)
	void on(NamespaceEvent.TrustedIssuerCreated event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-trusted-issuer")
				.entityId(event.issuer().id())
				.eventType("namespace.trusted-issuer-created")
				.details("name", event.issuer().name())
				.details("issuerUri", event.issuer().issuerUri())
		);
	}

	@TransactionalEventListener(id = "audit.trusted-issuer-updated", classes = NamespaceEvent.TrustedIssuerUpdated.class)
	void on(NamespaceEvent.TrustedIssuerUpdated event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-trusted-issuer")
				.entityId(event.issuer().id())
				.eventType("namespace.trusted-issuer-updated")
				.details("name", event.issuer().name())
				.details("issuerUri", event.issuer().issuerUri())
		);
	}

	@TransactionalEventListener(id = "audit.trusted-issuer-removed", classes = NamespaceEvent.TrustedIssuerRemoved.class)
	void on(NamespaceEvent.TrustedIssuerRemoved event) {
		insert(event, builder -> builder
				.namespace(event.id())
				.entityType("namespace-trusted-issuer")
				.entityId(event.issuer().id())
				.eventType("namespace.trusted-issuer-removed")
				.details("name", event.issuer().name())
				.details("issuerUri", event.issuer().issuerUri())
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

	@TransactionalEventListener(id = "audit.service-released", classes = ServiceEvent.Released.class)
	void on(ServiceEvent.Released event) {
		insert(event, builder -> {
			final List<String> artifacts = toSecureList(event.manifest().artifacts(),
					artifact -> ArtifactCoordinates.of(artifact).format());

			builder.entityType("service")
					.entityId(event.id())
					.eventType("service.released")
					.details("artifacts", artifacts);
		});
	}

	@TransactionalEventListener(id = "audit.service-release-failed", classes = ServiceEvent.ReleaseFailed.class)
	void on(ServiceEvent.ReleaseFailed event) {
		insert(event, builder -> builder
				.entityType("service")
				.entityId(event.id())
				.eventType("service.release-failed")
				.details("errors", toSecureList(event.errors()))
		);
	}

	@TransactionalEventListener(id = "audit.service-deleted", classes = ServiceEvent.Deleted.class)
	void on(ServiceEvent.Deleted event) {
		insert(event, builder -> builder
				.entityType("service")
				.entityId(event.id())
				.eventType("service.deleted")
		);
	}

	// ── Vault events ──────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.profile-created", classes = ProfileEvent.Created.class)
	void on(ProfileEvent.Created event) {
		insert(event, builder -> builder
				.entityType("profile")
				.entityId(event.id())
				.eventType("profile.created")
				.details("name", event.get().slug())
		);
	}

	@TransactionalEventListener(id = "audit.profile-updated", classes = ProfileEvent.Updated.class)
	void on(ProfileEvent.Updated event) {
		insert(event, builder -> builder
				.entityType("profile")
				.entityId(event.id())
				.eventType("profile.updated")
				.details("name", event.get().slug())
		);
	}

	@TransactionalEventListener(id = "audit.profile-deleted", classes = ProfileEvent.Deleted.class)
	void on(ProfileEvent.Deleted event) {
		insert(event, builder -> builder
				.entityType("profile")
				.entityId(event.id())
				.eventType("profile.deleted")
				.details("name", event.get().slug())
		);
	}

	// the VaultEvent.ChangesApplied event is never published within a transaction
	@EventListener(id = "audit.changes-applied", classes = VaultEvent.ChangesApplied.class)
	void on(VaultEvent.ChangesApplied event) {
		insert(event, builder -> builder
				.entityType("profile")
				.entityId(event.id())
				.eventType("profile.changes-applied")
				.actor(event.result().author())
				.details("revision", event.result().revision())
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

	@TransactionalEventListener(id = "audit.keyset-deleted", classes = KeysetManagementEvent.Deleted.class)
	void on(KeysetManagementEvent.Deleted event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.deleted")
		);
	}

	@TransactionalEventListener(id = "audit.keyset-reactivated", classes = KeysetManagementEvent.Reactivated.class)
	void on(KeysetManagementEvent.Reactivated event) {
		insert(event, builder -> builder
				.namespace(event.namespace())
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.reactivated")
				.details("key", event.key())
		);
	}

	@TransactionalEventListener(id = "audit.keyset-deactivated", classes = KeysetManagementEvent.Deactivated.class)
	void on(KeysetManagementEvent.Deactivated event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.deactivated")
				.details("key", event.key())
		);
	}

	@TransactionalEventListener(id = "audit.keyset-compromised", classes = KeysetManagementEvent.Compromised.class)
	void on(KeysetManagementEvent.Compromised event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.compromised")
				.details("key", event.key())
		);
	}

	@TransactionalEventListener(id = "audit.keyset-restored", classes = KeysetManagementEvent.Restored.class)
	void on(KeysetManagementEvent.Restored event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.restored")
				.details("key", event.key())
		);
	}

	@TransactionalEventListener(id = "audit.keyset-destroyed", classes = KeysetManagementEvent.Destroyed.class)
	void on(KeysetManagementEvent.Destroyed event) {
		insert(event, builder -> builder
				.entityType("keyset")
				.entityId(event.id())
				.eventType("keyset.destroyed")
				.details("key", event.key())
		);
	}

	// ── Artifactory events ──────────────────────────────────────────────────

	@TransactionalEventListener(id = "audit.publication-created", classes = ArtifactoryEvent.PublicationCreated.class)
	void on(ArtifactoryEvent.PublicationCreated event) {
		insert(event, builder -> builder
				.namespace(event.owner().id())
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.publication-created")
				.details("coordinates", event.coordinates().format())
		);
	}

	@TransactionalEventListener(id = "audit.publication-completed", classes = ArtifactoryEvent.PublicationCompleted.class)
	void on(ArtifactoryEvent.PublicationCompleted event) {
		insert(event, builder -> builder
				.namespace(event.owner().id())
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.publication-completed")
				.details("coordinates", event.coordinates().format())
		);
	}

	@TransactionalEventListener(id = "audit.publication-failed", classes = ArtifactoryEvent.PublicationFailed.class)
	void on(ArtifactoryEvent.PublicationFailed event) {
		insert(event, builder -> builder
				.namespace(event.owner().id())
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.publication-failed")
				.details("coordinates", event.coordinates().format())
		);
	}

	@TransactionalEventListener(id = "audit.publication-retracted", classes = ArtifactoryEvent.PublicationRetracted.class)
	void on(ArtifactoryEvent.PublicationRetracted event) {
		insert(event, builder -> builder
				.namespace(event.owner().id())
				.entityType("artifact-version")
				.entityId(event.id())
				.eventType("artifact-version.publication-retracted")
				.details("coordinates", event.coordinates().format())
		);
	}

	@TransactionalEventListener(id = "audit.artifact-deregistered", classes = ArtifactoryEvent.Deregistered.class)
	void on(ArtifactoryEvent.Deregistered event) {
		insert(event, builder -> builder
				.namespace(event.owner().id())
				.entityType("artifact-definition")
				.entityId(event.id())
				.eventType("artifact-definition.deregistered")
				.details("key", event.key().format())
		);
	}

	@TransactionalEventListener(id = "audit.artifact-visibility-changed", classes = ArtifactoryEvent.VisibilityChanged.class)
	void on(ArtifactoryEvent.VisibilityChanged event) {
		insert(event, builder -> builder
				.namespace(event.owner().id())
				.entityType("artifact-definition")
				.entityId(event.id())
				.eventType("artifact-definition.visibility-changed")
				.details("key", event.key().format())
				.details("visibility", event.visibility().name())
		);
	}

	@TransactionalEventListener(id = "audit.ownership-transfer-requested", classes = ArtifactoryEvent.OwnershipTransferRequested.class)
	void on(ArtifactoryEvent.OwnershipTransferRequested event) {
		insert(event, builder -> builder
				.namespace(event.to().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.sent")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
		insert(event, builder -> builder
				.namespace(event.from().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.requested")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
	}

	@TransactionalEventListener(id = "audit.ownership-transfer-accepted", classes = ArtifactoryEvent.OwnershipTransferAccepted.class)
	void on(ArtifactoryEvent.OwnershipTransferAccepted event) {
		insert(event, builder -> builder
				.namespace(event.to().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.received")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
		insert(event, builder -> builder
				.namespace(event.from().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.transferred")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
	}

	@TransactionalEventListener(id = "audit.ownership-transfer-rejected", classes = ArtifactoryEvent.OwnershipTransferRejected.class)
	void on(ArtifactoryEvent.OwnershipTransferRejected event) {
		insert(event, builder -> builder
				.namespace(event.to().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.request-rejected")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
		insert(event, builder -> builder
				.namespace(event.from().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.rejected")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
	}

	@TransactionalEventListener(id = "audit.ownership-transfer-cancelled", classes = ArtifactoryEvent.OwnershipTransferCancelled.class)
	void on(ArtifactoryEvent.OwnershipTransferCancelled event) {
		insert(event, builder -> builder
				.namespace(event.to().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.cancelled")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
		insert(event, builder -> builder
				.namespace(event.from().id())
				.entityType("artifact-ownership-transfer")
				.entityId(event.id())
				.eventType("artifact-ownership-transfer.request-cancelled")
				.details("groupId", event.groupId())
				.details("from", event.from().slug())
				.details("to", event.to().slug())
		);
	}

	private void insert(EntityEvent event, Consumer<AuditEvent.Builder> factory) {
		AuditObservation.create(observationRegistry, event).observe(() -> {
			try {
				final AuditEvent.Builder builder = AuditEvent.builder()
						.namespace(namespaceResolver.resolve(event));

				AuthenticatedPrincipal.fromSecurityContext().ifPresentOrElse(
						builder::actor,
						() -> builder.actor(SYSTEM_ACTOR)
				);

				factory.accept(builder);

				auditEventRepository.insert(builder.build());
			} catch (Exception ex) {
				log.error("Failed to persist audit event: {}", event, ex);
			}
		});
	}

	/**
	 * Copies the given {@code collection} into a plain {@link ArrayList} before it is stored as an
	 * {@link AuditEvent} detail.
	 * <p>
	 * Audit event details are serialized using a Jackson mapper hardened with Spring Security's
	 * {@code SecurityJacksonModules}, which installs a {@code PolymorphicTypeValidator} that only
	 * allows a curated subset of types to be (de)serialized polymorphically. Collections such as the
	 * ones returned by {@link List#of} or {@link java.util.stream.Stream#toList()} are backed by
	 * JDK-internal {@code ImmutableCollections} types that are not part of that allow-list, so passing
	 * them straight through would fail serialization. Re-collecting into a plain {@link ArrayList}
	 * guarantees the stored value is always a type the validator recognizes.
	 *
	 * @param collection the collection to convert, can be {@literal null} or empty
	 * @param <T> the type of elements in the collection
	 * @return an unmodifiable {@link ArrayList} copy of the given collection, never {@literal null}
	 */
	private static <T> List<T> toSecureList(@Nullable Collection<T> collection) {
		return toSecureList(collection, Function.identity());
	}

	/**
	 * Maps each element of the given {@code collection} with the supplied {@code mapper} and copies the
	 * result into a plain {@link ArrayList} before it is stored as an {@link AuditEvent} detail.
	 * <p>
	 * See {@link #toSecureList(Collection)} for why this copy is necessary: the mapped result must land
	 * in a type accepted by the {@code PolymorphicTypeValidator} that guards the audit Jackson mapper,
	 * rather than whatever collection type {@code mapper} or the caller happened to produce.
	 *
	 * @param collection the collection to convert, can be {@literal null} or empty
	 * @param mapper the function applied to each element, can't be {@literal null}
	 * @param <T> the type of elements in the source collection
	 * @param <R> the type of elements in the secured collection
	 * @return an unmodifiable {@link ArrayList} of the mapped elements, never {@literal null}
	 */
	private static <T, R> List<R> toSecureList(@Nullable Collection<T> collection, Function<T, R> mapper) {
		if (CollectionUtils.isEmpty(collection)) {
			return Collections.emptyList();
		}

		final List<R> list = new ArrayList<>(collection.size());

		for (T entry : collection) {
			list.add(mapper.apply(entry));
		}

		return Collections.unmodifiableList(list);
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
				case ProfileEvent pe -> resolve(pe.get());
				case VaultEvent ve -> resolve(ve.get());
				default -> null;
			};
		}

		private EntityId resolve(Profile profile) {
			final EntityId service = profile.service();

			return context.select(SERVICES.NAMESPACE_ID)
					.from(SERVICES)
					.where(SERVICES.ID.eq(service.get()))
					.fetchOptional(SERVICES.NAMESPACE_ID, EntityId.class)
					.orElseThrow(() -> new NamespaceException("Could not resolve namespace for profile " +
							profile.slug() + " of service " + service.get()));
		}

	}

}
