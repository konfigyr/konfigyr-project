package com.konfigyr.membership;

import com.konfigyr.account.Account;
import com.konfigyr.namespace.Namespace;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service that manages the lifecycle of {@link Invitation invitations} for namespace team members.
 * <p>
 * Controllers are responsible for resolving {@link Account} and {@link Namespace} objects before
 * calling these methods. The service does not perform entity lookups across module boundaries — it
 * expects fully resolved aggregates from the caller.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@NullMarked
public interface Invitations {

	/**
	 * Retrieve a page of {@link Invitation invitations} sent to the given {@link Account recipient}.
	 *
	 * @param recipient the recipient account, can't be {@literal null}
	 * @param pageable  paging instructions, can't be {@literal null}
	 * @return paged collection of invitations, never {@literal null}
	 */
	Page<Invitation> find(Account recipient, Pageable pageable);

	/**
	 * Retrieve a page of {@link Invitation invitations} created for the given {@link Namespace}.
	 *
	 * @param namespace the namespace for which invitations are listed, can't be {@literal null}
	 * @param pageable  paging instructions, can't be {@literal null}
	 * @return paged collection of invitations, never {@literal null}
	 */
	Page<Invitation> find(Namespace namespace, Pageable pageable);

	/**
	 * Retrieve a single {@link Invitation} by its key for the given {@link Account recipient}.
	 * Returns an empty {@link Optional} if no matching invitation exists.
	 *
	 * @param recipient the recipient account, can't be {@literal null}
	 * @param key       invitation key, can't be {@literal null}
	 * @return matching invitation or empty, never {@literal null}
	 */
	Optional<Invitation> get(Account recipient, String key);

	/**
	 * Retrieve a single {@link Invitation} by its key for the given {@link Namespace}.
	 * Returns an empty {@link Optional} if no matching invitation exists.
	 *
	 * @param namespace the namespace for which the invitation was created, can't be {@literal null}
	 * @param key       invitation key, can't be {@literal null}
	 * @return matching invitation or empty, never {@literal null}
	 */
	Optional<Invitation> get(Namespace namespace, String key);

	/**
	 * Creates an {@link Invitation} from the given {@link Invite} and sends an invitation email
	 * to the recipient. The sender must be an {@link com.konfigyr.namespace.NamespaceRole#ADMIN admin}
	 * member of the namespace.
	 *
	 * @param namespace the namespace to invite the recipient into, can't be {@literal null}
	 * @param invite    the invite command, can't be {@literal null}
	 * @return the created invitation, never {@literal null}
	 * @throws InvitationException with {@link InvitationException.ErrorCode#INSUFFICIENT_PERMISSIONS}
	 *                             if the sender is not a namespace admin
	 * @throws InvitationException with {@link InvitationException.ErrorCode#ALREADY_INVITED}
	 *                             if the recipient is already a member or has a pending invitation
	 * @throws InvitationException with {@link InvitationException.ErrorCode#MEMBER_LIMIT_REACHED}
	 *                             if the namespace has reached its member limit
	 */
	@DomainEventPublisher(publishes = "namespace.invitation-created")
	Invitation create(Namespace namespace, Invite invite);

	/**
	 * Accepts the given {@link Invitation} on behalf of the {@link Account recipient}, adding them
	 * as a new {@link Member} of the namespace with the role defined in the invitation.
	 * <p>
	 * The invitation is removed from the database once accepted. Only {@link InvitationState#PENDING}
	 * invitations may be accepted.
	 *
	 * @param recipient  the account accepting the invitation, can't be {@literal null}
	 * @param invitation the invitation to accept, can't be {@literal null}
	 * @throws InvitationException with {@link InvitationException.ErrorCode#INVITATION_NOT_FOUND}
	 *                             if the recipient does not match the invitation's intended recipient
	 * @throws InvitationException with {@link InvitationException.ErrorCode#INVITATION_EXPIRED}
	 *                             if the invitation has expired
	 */
	@DomainEventPublisher(publishes = "namespace.invitation-accepted")
	void accept(Account recipient, Invitation invitation);

	/**
	 * Declines the given {@link Invitation} on behalf of the {@link Account recipient}.
	 * The invitation is removed from the database once declined.
	 *
	 * @param recipient  the account declining the invitation, can't be {@literal null}
	 * @param invitation the invitation to decline, can't be {@literal null}
	 */
	@DomainEventPublisher(publishes = "namespace.invitation-declined")
	void decline(Account recipient, Invitation invitation);

	/**
	 * Cancels the given {@link Invitation} as a namespace administrator, revoking the outstanding
	 * invite before the recipient has acted on it. The invitation is removed from the database.
	 *
	 * @param namespace  the namespace for which the invitation is canceled, can't be {@literal null}
	 * @param invitation the invitation to cancel, can't be {@literal null}
	 */
	@DomainEventPublisher(publishes = "namespace.invitation-canceled")
	void cancel(Namespace namespace, Invitation invitation);

}
