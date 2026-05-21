package com.konfigyr.membership;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceRole;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Value object that represents an invitation attempt — the intent to invite a person to join a
 * {@link Namespace} team.
 * <p>
 * An {@link Invite} is the command that triggers creation of an {@link Invitation}. It carries
 * the minimum information needed to issue the invite: who is sending it, who is being invited,
 * and what role they will hold. The sender must be a namespace member with sufficient permissions
 * (typically {@link NamespaceRole#ADMIN}) to invite new members.
 * <p>
 * Once the invite is successfully processed by the {@link Invitations} service, an {@link Invitation}
 * record is created and an email is sent to the recipient.
 *
 * @param sender    entity identifier of the {@link com.konfigyr.account.Account} sending the invite,
 *                  can't be {@literal null}
 * @param recipient email address of the person being invited, can't be {@literal null}
 * @param role      the {@link NamespaceRole} the recipient will hold if they accept, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Invitation
 * @see Invitations
 **/
@ValueObject
public record Invite(
		@NonNull EntityId sender,
		@NonNull String recipient,
		@NonNull NamespaceRole role
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 9093799533515560353L;

}
