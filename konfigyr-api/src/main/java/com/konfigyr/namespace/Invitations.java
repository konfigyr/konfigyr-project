package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Service that defines how {@link Invitation invitations} are created and managed for potentially new
 * {@link Namespace namespace team members}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public interface Invitations {

	/**
	 * Retrieve all {@link Invitation invitations} that are sent to the given {@link Namespace}.
	 *
	 * @param namespace namespace for which invitations should be retrieved, can't be {@literal null}
	 * @param pageable  paging instructions, can't be {@literal null}
	 * @return paged collections of invitations, never {@literal null}
	 */
	@NonNull
	Page<Invitation> find(@NonNull Namespace namespace, @NonNull Pageable pageable);

	/**
	 * Retrieve a single {@link Invitation invitations} by its key that is sent for the given {@link Namespace}.
	 *
	 * @param namespace namespace for which invitation is sent, can't be {@literal null}
	 * @param key       invitation key, can't be {@literal null}
	 * @return found invitation or an empty {@link Optional}, never {@literal null}
	 */
	@NonNull
	Optional<Invitation> get(@NonNull Namespace namespace, @NonNull String key);

	/**
	 * Creates the {@link Invitation} for the {@link Invite invitation attempt} and sends the invitation
	 * email with the link to the invite recipient to join the {@link Namespace} as a new member.
	 *
	 * @param invite invite to be sent to the new member, can't be {@literal null}
	 * @return invitation
	 */
	@NonNull
	@DomainEventPublisher(publishes = "namespace.invitation-created")
	Invitation create(@NonNull Invite invite);

	/**
	 * Method that is invoked when the recipient of the {@link Invitation} accepts the request and wants to
	 * join the {@link Namespace}.
	 * <p>
	 * The given {@link Invitation} would be removed from the database and a new {@link Member} would be added
	 * to the {@link Namespace} with the defined {@link NamespaceRole}.
	 *
	 * @param invitation invitation to be accepted, can't be {@literal null}
	 * @param recipient  the entity identifier of the account that would be become a member, can't be {@literal null}
	 */
	@DomainEventPublisher(publishes = "namespace.invitation-accepted")
	void accept(@NonNull Invitation invitation, @NonNull EntityId recipient);

	/**
	 * Cancels the given {@link Invitation invitations} and revokes any sent out {@link Invite invites}
	 * to recipients.
	 *
	 * @param invitation invitation to be canceled, can't be {@literal null}
	 */
	@DomainEventPublisher(publishes = "namespace.invitation-canceled")
	void cancel(@NonNull Invitation invitation);

}
