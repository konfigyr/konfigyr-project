package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Abstract event type that should be used for all {@link Namespace} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class NamespaceEvent extends EntityEvent implements Supplier<Namespace>
		permits NamespaceEvent.Created,
		NamespaceEvent.Renamed,
		NamespaceEvent.Deleted,
		NamespaceEvent.ApplicationEvent,
		NamespaceEvent.MembershipEvent,
		NamespaceEvent.TrustedIssuerEvent {

	/**
	 * The namespace that is the subject of the event.
	 */
	private final Namespace namespace;

	protected NamespaceEvent(Namespace namespace) {
		super(namespace.id());
		this.namespace = namespace;
	}

	/**
	 * Returns the {@link Namespace} that is the subject of the event.
	 *
	 * @return the namespace, never {@literal null}.
	 */
	@Override
	public Namespace get() {
		return namespace;
	}

	/**
	 * Event that would be published when a new {@link Namespace} is created.
	 */
	@DomainEvent(name = "created", namespace = "namespaces")
	public static final class Created extends NamespaceEvent {

		/**
		 * Create a new {@link Created} event with the {@link Namespace} that was just
		 * created by the {@link NamespaceManager}.
		 *
		 * @param namespace the created namespace
		 */
		public Created(Namespace namespace) {
			super(namespace);
		}
	}

	/**
	 * Event that would be published when a {@link Namespace} URL slug is updated.
	 */
	@DomainEvent(name = "renamed", namespace = "namespaces")
	public static final class Renamed extends NamespaceEvent {

		private final Slug from;

		private final Slug to;

		/**
		 * Create a new {@link Renamed} event with the {@link Namespace} that was just
		 * updated and the URL slug values.
		 *
		 * @param namespace the renamed namespace
		 * @param from the previous namespace URL slug
		 * @param to the new namespace URL slug
		 */
		public Renamed(Namespace namespace, Slug from, Slug to) {
			super(namespace);
			this.from = from;
			this.to = to;
		}

		/**
		 * The previous URL slug that was used by the {@link Namespace}.
		 *
		 * @return previous URL slug, never {@literal null}
		 */
		@NonNull
		public Slug from() {
			return from;
		}

		/**
		 * The current URL slug that is set for the {@link Namespace}.
		 *
		 * @return current URL slug, never {@literal null}
		 */
		public Slug to() {
			return to;
		}
	}

	/**
	 * Event that would be published when an existing {@link Namespace} is deleted.
	 */
	@DomainEvent(name = "deleted", namespace = "namespaces")
	public static final class Deleted extends NamespaceEvent {

		/**
		 * Create a new {@link Deleted} event with the {@link Namespace} that was just
		 * deleted by the {@link NamespaceManager}.
		 *
		 * @param namespace the deleted namespace
		 */
		public Deleted(Namespace namespace) {
			super(namespace);
		}
	}

	/**
	 * Abstract event that would be used for all {@link com.konfigyr.membership.Memberships membership}
	 * related changes of a {@link Namespace}.
	 */
	public static abstract sealed class MembershipEvent extends NamespaceEvent
			permits MemberAdded, MemberUpdated, MemberRemoved {

		private final EntityId account;

		protected MembershipEvent(Namespace namespace, EntityId account) {
			super(namespace);
			Assert.notNull(account, "Entity identifier of the member user account can not be null");
			this.account = account;
		}

		/**
		 * The entity identifier of the {@link com.konfigyr.account.Account} that was the subject of
		 * the change within the given {@link Namespace}.
		 *
		 * @return account entity identifier, never {@literal null}
		 */
		@NonNull
		public EntityId account() {
			return account;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[id=" + id + ", account=" + account + ", timestamp=" + timestamp + ']';
		}
	}

	/**
	 * Event that would be published when a new {@link com.konfigyr.membership.Member}
	 * is added to the {@link Namespace}.
	 */
	@DomainEvent(name = "member-added", namespace = "namespaces")
	public static final class MemberAdded extends MembershipEvent {

		private final NamespaceRole role;

		/**
		 * Create a new {@link MemberAdded} event with the {@link Namespace}, {@link com.konfigyr.account.Account}
		 * that was added as a new {@link com.konfigyr.membership.Member} and the {@link NamespaceRole}
		 * that was assigned to it.
		 *
		 * @param namespace the namespace to which this member was added
		 * @param account entity identifier of the user account that was added as a member
		 * @param role namespace role assigned to the new member
		 */
		public MemberAdded(Namespace namespace, EntityId account, NamespaceRole role) {
			super(namespace, account);

			Assert.notNull(role, "Namespace role assigned to the member can not be null");
			this.role = role;
		}

		@NonNull
		public NamespaceRole role() {
			return role;
		}
	}

	/**
	 * Event that would be published when an existing {@link com.konfigyr.membership.Member} was
	 * updated within the {@link Namespace}.
	 */
	@DomainEvent(name = "member-updated", namespace = "namespaces")
	public static final class MemberUpdated extends MembershipEvent {

		private final NamespaceRole role;

		/**
		 * Create an {@link MemberUpdated} event with the {@link Namespace}, {@link com.konfigyr.account.Account}
		 * for which the membership was updated and the {@link NamespaceRole} that was changed.
		 *
		 * @param namespace the namespace to which this member was updated
		 * @param account entity identifier of the user account for which the membership was updated
		 * @param role namespace role changed for the member
		 */
		public MemberUpdated(Namespace namespace, EntityId account, NamespaceRole role) {
			super(namespace, account);

			Assert.notNull(role, "Namespace role that was updated for the member can not be null");
			this.role = role;
		}

		@NonNull
		public NamespaceRole role() {
			return role;
		}
	}

	/**
	 * Event that would be published when an existing {@link com.konfigyr.membership.Member} was
	 * removed from the {@link Namespace}.
	 */
	@DomainEvent(name = "member-removed", namespace = "namespaces")
	public static final class MemberRemoved extends MembershipEvent {

		/**
		 * Create an {@link MemberRemoved} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} and the {@link com.konfigyr.account.Account} that was removed from the namespace.
		 *
		 * @param namespace the namespace from which the member was removed
		 * @param account entity identifier of the user account that was removed as a namespace member
		 */
		public MemberRemoved(Namespace namespace, EntityId account) {
			super(namespace, account);
		}
	}

	/**
	 * Abstract event that would be used for all {@link NamespaceApplication} related changes of a {@link Namespace}.
	 */
	public static abstract sealed class ApplicationEvent extends NamespaceEvent
			permits ApplicationCreated, ApplicationUpdated, ApplicationReset, ApplicationRemoved {

		private final NamespaceApplication application;

		protected ApplicationEvent(Namespace namespace, NamespaceApplication application) {
			super(namespace);
			this.application = application;
		}

		/**
		 * The entity identifier of the {@link com.konfigyr.account.Account} that was the subject of
		 * the change within the given {@link Namespace}.
		 *
		 * @return account entity identifier, never {@literal null}
		 */
		@NonNull
		public NamespaceApplication application() {
			return application;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[id=" + id + ", application=" + application.id() + ", timestamp=" + timestamp + ']';
		}
	}

	/**
	 * Event that would be published when a new {@link NamespaceApplication} is added to the {@link Namespace}.
	 */
	@DomainEvent(name = "application-created", namespace = "namespaces")
	public static final class ApplicationCreated extends ApplicationEvent {

		/**
		 * Create a new {@link ApplicationCreated} event with the {@link Namespace} and the created
		 * {@link NamespaceApplication} instance.
		 *
		 * @param namespace the namespace to which this application was added
		 * @param application the created namespace application
		 */
		public ApplicationCreated(Namespace namespace, NamespaceApplication application) {
			super(namespace, application);
		}
	}

	/**
	 * Event that would be published when an {@link NamespaceApplication} was updated for the {@link Namespace}.
	 */
	@DomainEvent(name = "application-updated", namespace = "namespaces")
	public static final class ApplicationUpdated extends ApplicationEvent {

		/**
		 * Create a new {@link ApplicationUpdated} event with the {@link Namespace} and the updated
		 * {@link NamespaceApplication} instance.
		 *
		 * @param namespace the namespace that owns the updated application
		 * @param application the updated namespace application
		 */
		public ApplicationUpdated(Namespace namespace, NamespaceApplication application) {
			super(namespace, application);
		}
	}

	/**
	 * Event that would be published when an {@link NamespaceApplication} was updated for the {@link Namespace}.
	 */
	@DomainEvent(name = "application-reset", namespace = "namespaces")
	public static final class ApplicationReset extends ApplicationEvent {

		/**
		 * Create a new {@link ApplicationUpdated} event with the {@link Namespace} and the reset
		 * {@link NamespaceApplication} instance.
		 *
		 * @param namespace the namespace that owns the updated application
		 * @param application the namespace application that was reset
		 */
		public ApplicationReset(Namespace namespace, NamespaceApplication application) {
			super(namespace, application);
		}
	}

	/**
	 * Event that would be published when an {@link NamespaceApplication} was updated for the {@link Namespace}.
	 */
	@DomainEvent(name = "application-removed", namespace = "namespaces")
	public static final class ApplicationRemoved extends ApplicationEvent {

		/**
		 * Create a new {@link ApplicationRemoved} event with the {@link Namespace} and the removed
		 * {@link NamespaceApplication} instance.
		 *
		 * @param namespace the namespace that owed the removed application
		 * @param application the removed namespace application
		 */
		public ApplicationRemoved(Namespace namespace, NamespaceApplication application) {
			super(namespace, application);
		}
	}

	/**
	 * Abstract event for all {@link NamespaceTrustedIssuer} changes within a {@link Namespace}.
	 */
	public static abstract sealed class TrustedIssuerEvent extends NamespaceEvent
			permits TrustedIssuerCreated, TrustedIssuerUpdated, TrustedIssuerRemoved {

		private final NamespaceTrustedIssuer issuer;

		protected TrustedIssuerEvent(Namespace namespace, NamespaceTrustedIssuer issuer) {
			super(namespace);
			Assert.notNull(issuer, "NamespaceTrustedIssuer can not be null");
			this.issuer = issuer;
		}

		/**
		 * The {@link NamespaceTrustedIssuer} that was the subject of this event.
		 *
		 * @return the trusted issuer, never {@literal null}
		 */
		@NonNull
		public NamespaceTrustedIssuer issuer() {
			return issuer;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[id=" + id + ", issuer=" + issuer.id() + ", timestamp=" + timestamp + ']';
		}
	}

	/**
	 * Event published when a new {@link NamespaceTrustedIssuer} is registered for a {@link Namespace}.
	 */
	@DomainEvent(name = "trusted-issuer-created", namespace = "namespaces")
	public static final class TrustedIssuerCreated extends TrustedIssuerEvent {

		/**
		 * Creates a new trusted issuer created event for the namespace and the issuer that was added.
		 *
		 * @param namespace the namespace to which the issuer was added
		 * @param issuer the created trusted issuer
		 */
		public TrustedIssuerCreated(Namespace namespace, NamespaceTrustedIssuer issuer) {
			super(namespace, issuer);
		}
	}

	/**
	 * Event published when a {@link NamespaceTrustedIssuer} is updated within a {@link Namespace}.
	 */
	@DomainEvent(name = "trusted-issuer-updated", namespace = "namespaces")
	public static final class TrustedIssuerUpdated extends TrustedIssuerEvent {

		/**
		 * Creates a new trusted issuer updated event for the namespace and the issuer that was modified.
		 *
		 * @param namespace the namespace that owns the updated issuer
		 * @param issuer the updated trusted issuer
		 */
		public TrustedIssuerUpdated(Namespace namespace, NamespaceTrustedIssuer issuer) {
			super(namespace, issuer);
		}
	}

	/**
	 * Event published when a {@link NamespaceTrustedIssuer} is removed from a {@link Namespace}.
	 */
	@DomainEvent(name = "trusted-issuer-removed", namespace = "namespaces")
	public static final class TrustedIssuerRemoved extends TrustedIssuerEvent {

		/**
		 * Creates a new trusted issuer removed event for the namespace and the issuer that was removed.
		 *
		 * @param namespace the namespace from which the issuer was removed
		 * @param issuer the removed trusted issuer
		 */
		public TrustedIssuerRemoved(Namespace namespace, NamespaceTrustedIssuer issuer) {
			super(namespace, issuer);
		}
	}

}
