package com.konfigyr.namespace;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serial;

/**
 * Exception that is thrown by the {@link Invitations} service when creating or managing
 * {@link Invitation namespace team member invitations}.
 * <p>
 * This exception comes with a {@link ErrorCode} that can be used to inspect the actual reason
 * why this type of exception was thrown.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ErrorCode
 **/
@Getter
public class InvitationException extends NamespaceException {

	@Serial
	private static final long serialVersionUID = 3608929360545001766L;

	private final ErrorCode code;

	public InvitationException(@NonNull ErrorCode code, @NonNull String message) {
		super(message);
		this.code = code;
	}

	public InvitationException(@NonNull ErrorCode code, @NonNull String message, @Nullable Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Enumeration of possible error codes that better describe the {@link InvitationException}.
	 */
	public enum ErrorCode {

		/**
		 * Code that is used when the {@link Invitations} service can not find the {@link Invitation}.
		 */
		INVITATION_NOT_FOUND,

		/**
		 * Code that is used when an expired {@link Invitation} is being accepted.
		 */
		INVITATION_EXPIRED,

		/**
		 * Code that is used when the {@link Invitation} is being accepted by an unknown recipient.
		 */
		RECIPIENT_NOT_FOUND,

		/**
		 * Error code used when the {@link Invite} is being sent to a recipient that is already
		 * a {@link Member} of a {@link Namespace}.
		 */
		ALREADY_INVITED,

		/**
		 * Error code used when an {@link com.konfigyr.account.Account} wants to send an {@link Invite}
		 * but is either not a {@link Namespace} member or has insufficient permissions to perform the operation.
		 */
		INSUFFICIENT_PERMISSIONS,

		/**
		 * When an {@link Invite} is being sent for the {@link Namespace} with an unsupported {@link NamespaceType},
		 * like {@link NamespaceType#PERSONAL}, this error code would be used.
		 */
		UNSUPPORTED_NAMESPACE_TYPE,
	}
}
