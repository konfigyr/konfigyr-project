package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Record that defines the invitation attempt to a person to join the {@link Namespace} team.
 * <p>
 * The invite needs to contain the {@link EntityId entity identifiers} of the {@link Namespace} and of
 * the {@link com.konfigyr.account.Account sender account}, the email of the person that is being invited
 * to the {@link Namespace} team and the {@link NamespaceRole} that this new member is going to have within
 * the team.
 * <p>
 * It is important to note that the {@link com.konfigyr.account.Account sender} must be a part of the
 * {@link Namespace} and should have sufficient permissions to invite new members to the team.
 * <p>
 * When the invite is successfully sent to the recipient, the {@link Invitation} would be created and
 * stored in the database.
 *
 * @param namespace entity identifier of the namespace to which this new member would be a part of,
 *                  can't be {@literal null}
 * @param sender	entity identifier of the {@link com.konfigyr.account.Account} that created the
 *                  invite intent, can't be {@literal null}
 * @param recipient email address of the invited person, can't be {@literal null}
 * @param role 		the role which this potentially new member is going to have, can't be {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Invitation
 **/
@ValueObject
public record Invite(
		@NonNull EntityId namespace,
		@NonNull EntityId sender,
		@NonNull String recipient,
		@NonNull NamespaceRole role
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 9093799533515560353L;

}
