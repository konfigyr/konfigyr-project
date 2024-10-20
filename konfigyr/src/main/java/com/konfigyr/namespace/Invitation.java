package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.FullName;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Defines a request, or an attempt, to get the recipient to join a {@link Namespace} as a team member.
 * <p>
 * To create a new {@link Invitation} you first need to send an {@link Invite} request. When this invite
 * attempt is successfully sent, the invitation is created and stored until it expires or the recipient
 * accepts it.
 * <p>
 * Once the invitation is accepted the invited user can become a member of the {@link Namespace} with the
 * {@link NamespaceRole} that was initially defined in the {@link Invite}.
 *
 * @param key 		 unique invitation key, can't be {@literal null}
 * @param namespace  entity identifier of the namespace for which this invitation is created, can't be {@literal null}
 * @param sender	 information about the user that sent the request, can be {@literal null}
 * @param recipient  information about the recipient that would receive the invitation, can't be {@literal null}
 * @param role 		 role which the user would have when he joins the namespace, can't be {@literal null}
 * @param createdAt  date when this invitation was created, can't be {@literal null}
 * @param expiryDate date when this invitation would expire, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Invite
 **/
@Entity
public record Invitation(
		@NonNull @Identity String key,
		@NonNull @Association(aggregateType = Namespace.class) EntityId namespace,
		@Nullable Sender sender,
		@NonNull Recipient recipient,
		@NonNull NamespaceRole role,
		@NonNull OffsetDateTime createdAt,
		@NonNull OffsetDateTime expiryDate
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1687904987462773230L;

	/**
	 * Checks if this {@link Invitation} is expired or not by checking its expiration date.
	 *
	 * @return {@code true} when expiration date is in the past, {@code false} otherwise.
	 */
	public boolean isExpired() {
		return OffsetDateTime.now().isAfter(expiryDate);
	}

	/**
	 * Creates a new {@link Builder fluent namespace builder} instance used to create
	 * the {@link Invitation} record.
	 *
	 * @return invitation builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Information about the sender user account that initially sent the {@link Invite} and created the
	 * {@link Invitation}.
	 *
	 * @param id    entity identifier of the sender account, can't be {@literal null}
	 * @param email email address of the sender account, can't be {@literal null}
	 * @param name  full name of the sender account, can't be {@literal null}
	 */
	public record Sender(
			@NonNull EntityId id,
			@NonNull String email,
			@NonNull FullName name
	) implements Serializable {

		@Serial
		private static final long serialVersionUID = Invitation.serialVersionUID;

		public Sender(@NonNull EntityId id, @NonNull String email, String firstNme, String lastName) {
			this(id, email, FullName.of(firstNme, lastName));
		}
	}

	/**
	 * Information about the recipient that would receive the {@link Invitation}.
	 * <p>
	 * The recipient may or may not have an {@link com.konfigyr.account.Account} at the time when the
	 * {@link Invite} is sent. When there is an account, the identifier and name of the recipient are
	 * known. Otherwise, just the email address would be present.
	 *
	 * @param id    entity identifier of the recipient account, can be {@literal null}
	 * @param email email address of the recipient, can't be {@literal null}
	 * @param name  full name of the recipient account, can be {@literal null}
	 */
	public record Recipient(
			@Nullable EntityId id,
			@NonNull String email,
			@Nullable FullName name
	) implements Serializable {

		@Serial
		private static final long serialVersionUID = Invitation.serialVersionUID;

		public Recipient(@NonNull String email) {
			this(null, email, null);
		}

		public Recipient(@NonNull EntityId id, @NonNull String email, String firstNme, String lastName) {
			this(id, email, FullName.of(firstNme, lastName));
		}

		/**
		 * Checks if this {@link Recipient} has an {@link com.konfigyr.account.Account}.
		 *
		 * @return {@code true} when this recipient is already known to Konfigyr, {@code false} otherwise
		 */
		public boolean exists() {
			return id != null;
		}
	}

	/**
	 * Fluent builder type used to create an {@link Invitation}.
	 */
	public static final class Builder {
		private String key;
		private EntityId namespace;
		private Sender sender;
		private Recipient recipient;
		private NamespaceRole role;
		private OffsetDateTime createdAt;
		private OffsetDateTime expiryDate;

		private Builder() {
		}

		/**
		 * Specify the invitation key for this {@link Invitation}.
		 *
		 * @param key invitation key
		 * @return invitation builder
		 */
		@NonNull
		public Builder key(String key) {
			this.key = key;
			return this;
		}

		/**
		 * Specify the namespace identifier for which this {@link Invitation} is created.
		 *
		 * @param namespace namespace identifier
		 * @return invitation builder
		 */
		@NonNull
		public Builder namespace(Long namespace) {
			return namespace == null ? this : namespace(EntityId.from(namespace));
		}

		/**
		 * Specify the namespace {@link EntityId} for which this {@link Invitation} is created.
		 *
		 * @param namespace namespace identifier
		 * @return invitation builder
		 */
		@NonNull
		public Builder namespace(EntityId namespace) {
			this.namespace = namespace;
			return this;
		}

		/**
		 * Specify the {@link Sender} that initially created this {@link Invitation}.
		 *
		 * @param sender invitation sender
		 * @return invitation builder
		 */
		@NonNull
		public Builder sender(Sender sender) {
			this.sender = sender;
			return this;
		}

		/**
		 * The {@link Recipient} that is intended to receive this {@link Invitation}.
		 *
		 * @param recipient invitation recipient
		 * @return invitation builder
		 */
		@NonNull
		public Builder recipient(Recipient recipient) {
			this.recipient = recipient;
			return this;
		}

		/**
		 * The {@link NamespaceRole} that would be used when the recipient of this {@link Invitation}
		 * becomes a new member of the {@link Namespace}.
		 *
		 * @param role namespace role for the potential namespace member
		 * @return invitation builder
		 */
		@NonNull
		public Builder role(NamespaceRole role) {
			this.role = role;
			return this;
		}

		/**
		 * Define the date when this {@link Invitation} is created and sent to the recipient email address.
		 *
		 * @param createdAt created date
		 * @return invitation builder
		 */
		@NonNull
		public Builder createdAt(OffsetDateTime createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Define the date when this {@link Invitation} would expire.
		 *
		 * @param expiryDate expiry date
		 * @return invitation builder
		 */
		@NonNull
		public Builder expiryDate(OffsetDateTime expiryDate) {
			this.expiryDate = expiryDate;
			return this;
		}

		public Invitation build() {
			Assert.hasText(key, "Invitation key can not be blank");
			Assert.notNull(namespace, "Namespace identifier can not be null");
			Assert.notNull(recipient, "Invitation recipient can not be null");
			Assert.notNull(role, "Namespace member role can not be null");
			Assert.notNull(createdAt, "Invitation creation date can not be null");
			Assert.notNull(expiryDate, "Invitation expiry date can not be null");

			return new Invitation(key, namespace, sender, recipient, role, createdAt, expiryDate);
		}
	}
}
