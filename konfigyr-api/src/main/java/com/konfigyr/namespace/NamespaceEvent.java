package com.konfigyr.namespace;

import com.konfigyr.entity.EntityEvent;
import com.konfigyr.entity.EntityId;
import com.konfigyr.support.Slug;
import org.jmolecules.event.annotation.DomainEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.util.Assert;

/**
 * Abstract event type that should be used for all {@link Namespace} related events.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public abstract sealed class NamespaceEvent extends EntityEvent
		permits NamespaceEvent.Created, NamespaceEvent.Renamed, NamespaceEvent.Deleted, NamespaceEvent.MembershipEvent {

	protected NamespaceEvent(EntityId id) {
		super(id);
	}

	/**
	 * Event that would be published when a new {@link Namespace} is created.
	 */
	@DomainEvent(name = "created", namespace = "namespaces")
	public static final class Created extends NamespaceEvent {

		/**
		 * Create a new {@link Created} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} that was just created by the {@link NamespaceManager}.
		 *
		 * @param id entity identifier of the created namespace
		 */
		public Created(EntityId id) {
			super(id);
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
		 * Create a new {@link Renamed} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} that was just updated and the URL slug values.
		 *
		 * @param id entity identifier of the created namespace
		 * @param from the previous namespace URL slug
		 * @param to the new namespace URL slug
		 */
		public Renamed(EntityId id, Slug from, Slug to) {
			super(id);
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
		 * Create a new {@link Deleted} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} that was just deleted by the {@link NamespaceManager}.
		 *
		 * @param id entity identifier of the deleted namespace
		 */
		public Deleted(EntityId id) {
			super(id);
		}
	}

	/**
	 * Abstract event that would be used for all {@link Member} related changes of a {@link Namespace}.
	 */
	public static abstract sealed class MembershipEvent extends NamespaceEvent
			permits MemberAdded, MemberUpdated, MemberRemoved {

		private final EntityId account;

		protected MembershipEvent(EntityId id, EntityId account) {
			super(id);

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
	 * Event that would be published when a new {@link Member} is added to the {@link Namespace}.
	 */
	@DomainEvent(name = "member-added", namespace = "namespaces")
	public static final class MemberAdded extends MembershipEvent {

		private final NamespaceRole role;

		/**
		 * Create a new {@link MemberAdded} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace}, {@link com.konfigyr.account.Account} that was added as a new {@link Member}
		 * and the {@link NamespaceRole} that was assigned to it.
		 *
		 * @param id entity identifier of the namespace
		 * @param account entity identifier of the user account that was added as a member
		 * @param role namespace role assigned to the new member
		 */
		public MemberAdded(EntityId id, EntityId account, NamespaceRole role) {
			super(id, account);

			Assert.notNull(role, "Namespace role assigned to the member can not be null");
			this.role = role;
		}

		@NonNull
		public NamespaceRole role() {
			return role;
		}
	}

	/**
	 * Event that would be published when an existing {@link Member} was updated within the {@link Namespace}.
	 */
	@DomainEvent(name = "member-updated", namespace = "namespaces")
	public static final class MemberUpdated extends MembershipEvent {

		private final NamespaceRole role;

		/**
		 * Create an {@link MemberUpdated} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace}, {@link com.konfigyr.account.Account} for which the membership was updated
		 * and the {@link NamespaceRole} that was changed.
		 *
		 * @param id entity identifier of the namespace
		 * @param account entity identifier of the user account for which the membership was updated
		 * @param role namespace role changed for the member
		 */
		public MemberUpdated(EntityId id, EntityId account, NamespaceRole role) {
			super(id, account);

			Assert.notNull(role, "Namespace role that was updated for the member can not be null");
			this.role = role;
		}

		@NonNull
		public NamespaceRole role() {
			return role;
		}
	}

	/**
	 * Event that would be published when an existing {@link Member} was removed from the {@link Namespace}.
	 */
	@DomainEvent(name = "member-removed", namespace = "namespaces")
	public static final class MemberRemoved extends MembershipEvent {

		/**
		 * Create an {@link MemberRemoved} event with the {@link EntityId entity identifier} of the
		 * {@link Namespace} and the {@link com.konfigyr.account.Account} that was removed from the namespace.
		 *
		 * @param id entity identifier of the namespace
		 * @param account entity identifier of the user account that was removed as a namespace member
		 */
		public MemberRemoved(EntityId id, EntityId account) {
			super(id, account);
		}
	}

}
