package com.konfigyr.membership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import com.konfigyr.support.FullName;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Defines a request, or an attempt, to get the recipient to join a {@link Namespace} as a team member.
 * <p>
 * To create a new {@link Invitation} you first need to send an {@link Invite} request. When the invite
 * is successfully sent, the invitation is created and stored until it expires or the recipient acts on it.
 * <p>
 * Once the invitation is accepted the invited user becomes a {@link Member} of the
 * {@link Namespace} with the {@link NamespaceRole} that was defined in the original {@link Invite}.
 * The lifecycle of an invitation is tracked via {@link InvitationState}: only {@link InvitationState#PENDING}
 * invitations may be accepted or declined.
 *
 * @param key          unique invitation key used as a single-use token, can't be {@literal null}
 * @param organization organization details for the namespace to which this invitation belongs, can't be {@literal null}
 * @param sender       information about the account that sent the invite, can be {@literal null} when
 *                     the sender account no longer exists
 * @param recipient    information about the account that is being invited, can't be {@literal null}
 * @param role         role the recipient will hold once they join the namespace, can't be {@literal null}
 * @param state        current lifecycle state of this invitation, can't be {@literal null}
 * @param createdAt    date when this invitation was created, can't be {@literal null}
 * @param expiryDate   date when this invitation expires, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Invite
 * @see InvitationState
 **/
@Entity
public record Invitation(
		@NonNull @Identity String key,
		@NonNull @Association(aggregateType = Namespace.class) Organization organization,
		@Nullable Sender sender,
		@NonNull Recipient recipient,
		@NonNull NamespaceRole role,
		@NonNull InvitationState state,
		@NonNull OffsetDateTime createdAt,
		@NonNull OffsetDateTime expiryDate
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1687904987462773230L;

	/**
	 * Checks if this {@link Invitation} is expired by comparing its expiry date to the current time.
	 *
	 * @return {@code true} when the expiry date is in the past, {@code false} otherwise.
	 */
	public boolean isExpired() {
		return OffsetDateTime.now().isAfter(expiryDate);
	}

	/**
	 * Creates a new {@link Builder fluent invitation builder} instance.
	 *
	 * @return invitation builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A self-contained projection of the {@link Namespace} to which an {@link Invitation} belongs.
	 * <p>
	 * Captures the essential display information needed to identify the organization without requiring
	 * a separate namespace lookup after loading an invitation.
	 *
	 * @param id          entity identifier of the namespace, can't be {@literal null}
	 * @param slug        URL-safe identifier of the namespace, can't be {@literal null}
	 * @param name        display name of the namespace, can't be {@literal null}
	 * @param description optional description of the namespace, can be {@literal null}
	 */
	public record Organization(
			@NonNull EntityId id,
			@NonNull String slug,
			@NonNull String name,
			@Nullable String description
	) implements Serializable {

		@Serial
		private static final long serialVersionUID = Invitation.serialVersionUID;

		public Organization(@NonNull Namespace namespace) {
			this(namespace.id(), namespace.slug(), namespace.name(), namespace.description());
		}
	}

	/**
	 * Information about the account that originally sent the {@link Invite} and created the
	 * {@link Invitation}.
	 * <p>
	 * The sender record may be absent if the sender's account was deleted after the invitation was created.
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

		public Sender(@NonNull EntityId id, @NonNull String email, String firstName, String lastName) {
			this(id, email, FullName.of(firstName, lastName));
		}
	}

	/**
	 * Information about the account that is the target of the {@link Invitation}.
	 * <p>
	 * The recipient may or may not have an existing {@link com.konfigyr.account.Account} at the time
	 * the {@link Invite} is sent. When an account is found for the recipient email, the identifier and
	 * name are populated. Otherwise, only the email address is known.
	 *
	 * @param id    entity identifier of the recipient account, can be {@literal null} when no account
	 *              exists for the recipient email
	 * @param email email address of the recipient, can't be {@literal null}
	 * @param name  full name of the recipient account, can be {@literal null} when no account exists
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

		public Recipient(@NonNull EntityId id, @NonNull String email, String firstName, String lastName) {
			this(id, email, FullName.of(firstName, lastName));
		}

		/**
		 * Returns {@code true} if this recipient already has a {@link com.konfigyr.account.Account}.
		 *
		 * @return {@code true} when an account identifier is present, {@code false} otherwise
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
		private Organization organization;
		private Sender sender;
		private Recipient recipient;
		private NamespaceRole role;
		private InvitationState state;
		private OffsetDateTime createdAt;
		private OffsetDateTime expiryDate;

		private Builder() {
		}

		/**
		 * Specify the unique key for this {@link Invitation}.
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
		 * Specify the {@link Organization} for this {@link Invitation}.
		 *
		 * @param organization organization to which this invitation belongs
		 * @return invitation builder
		 */
		@NonNull
		public Builder organization(Organization organization) {
			this.organization = organization;
			return this;
		}

		/**
		 * Specify the {@link Organization} for this {@link Invitation} from a {@link Namespace}.
		 *
		 * @param namespace namespace to which this invitation belongs
		 * @return invitation builder
		 */
		@NonNull
		public Builder organization(Namespace namespace) {
			return namespace == null ? this : organization(new Organization(namespace));
		}

		/**
		 * Specify the {@link Sender} that created this {@link Invitation}.
		 *
		 * @param sender invitation sender, can be {@literal null}
		 * @return invitation builder
		 */
		@NonNull
		public Builder sender(Sender sender) {
			this.sender = sender;
			return this;
		}

		/**
		 * Specify the {@link Recipient} of this {@link Invitation}.
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
		 * Specify the {@link NamespaceRole} the recipient will hold once they accept this {@link Invitation}.
		 *
		 * @param role namespace role for the potential new member
		 * @return invitation builder
		 */
		@NonNull
		public Builder role(NamespaceRole role) {
			this.role = role;
			return this;
		}

		/**
		 * Specify the {@link InvitationState lifecycle state} of this {@link Invitation}.
		 * When not set, defaults to {@link InvitationState#PENDING}.
		 *
		 * @param state invitation state
		 * @return invitation builder
		 */
		@NonNull
		public Builder state(InvitationState state) {
			this.state = state;
			return this;
		}

		/**
		 * Specify the date when this {@link Invitation} was created and sent.
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
		 * Specify the date when this {@link Invitation} expires.
		 *
		 * @param expiryDate expiry date
		 * @return invitation builder
		 */
		@NonNull
		public Builder expiryDate(OffsetDateTime expiryDate) {
			this.expiryDate = expiryDate;
			return this;
		}

		/**
		 * Creates a new instance of the {@link Invitation} using the values defined in the builder.
		 * When no {@link InvitationState} is set, defaults to {@link InvitationState#PENDING}.
		 *
		 * @return invitation instance, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public Invitation build() {
			Assert.hasText(key, "Invitation key can not be blank");
			Assert.notNull(organization, "Invitation organization can not be null");
			Assert.notNull(recipient, "Invitation recipient can not be null");
			Assert.notNull(role, "Namespace member role can not be null");
			Assert.notNull(createdAt, "Invitation creation date can not be null");
			Assert.notNull(expiryDate, "Invitation expiry date can not be null");

			if (state == null) {
				state = InvitationState.PENDING;
			}

			return new Invitation(key, organization, sender, recipient, role, state, createdAt, expiryDate);
		}
	}
}
