package com.konfigyr.membership;

/**
 * Defines the lifecycle state of an {@link Invitation}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public enum InvitationState {

	/**
	 * The invitation has been created and sent to the recipient. It is awaiting a response.
	 * <p>
	 * The service should allow the recipient to accept or decline the invitation in this state.
	 */
	PENDING,

	/**
	 * The recipient accepted the invitation and has become a namespace member.
	 * <p>
	 * The service should reject any further operations on this invitation.
	 */
	ACCEPTED,

	/**
	 * The invitation passed its expiry date without being acted upon.
	 * <p>
	 * The service should reject accept and decline operations and may clean up the record.
	 */
	EXPIRED,

	/**
	 * The invitation was explicitly canceled by a namespace administrator before the recipient
	 * acted on it.
	 * <p>
	 * The service should reject any further operations on this invitation.
	 */
	REVOKED

}
