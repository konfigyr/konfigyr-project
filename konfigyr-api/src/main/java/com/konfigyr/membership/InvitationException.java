package com.konfigyr.membership;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.io.Serial;

/**
 * Exception thrown by the {@link Invitations} service when creating or managing
 * {@link Invitation namespace team member invitations}.
 * <p>
 * This exception carries an {@link ErrorCode} that identifies the specific failure reason,
 * allowing callers and error handlers to distinguish between different failure scenarios
 * (e.g., invitation not found, recipient already a member, member limit reached).
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ErrorCode
 **/
@Getter
public class InvitationException extends MembershipException {

	@Serial
	private static final long serialVersionUID = 3608929360545001766L;

	private final ErrorCode code;

	public InvitationException(@NonNull ErrorCode code, @NonNull String message) {
		super(code.statusCode, message);
		this.code = code;
		this.body.setProperty("code", code.name());
	}

	public InvitationException(@NonNull ErrorCode code, @NonNull String message, @Nullable Throwable cause) {
		super(code.statusCode, message, cause);
		this.code = code;
		this.body.setProperty("code", code.name());
	}

	@NonNull
	@Override
	public String getDetailMessageCode() {
		return "problemDetail." + getClass().getName() + "." + code.name();
	}

	@NonNull
	@Override
	public String getTitleMessageCode() {
		return "problemDetail.title." + getClass().getName() + "." + code.name();
	}

	/**
	 * Enumeration of error codes that identify the specific reason a {@link InvitationException} was thrown.
	 */
	public enum ErrorCode {

		/**
		 * The requested {@link Invitation} could not be found, either because it does not exist
		 * or because the requesting account is not the intended recipient.
		 */
		INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND),

		/**
		 * An attempt was made to accept an {@link Invitation} that has already expired.
		 */
		INVITATION_EXPIRED(HttpStatus.BAD_REQUEST),

		/**
		 * The {@link Invitation} could not be accepted because the recipient account does not exist.
		 */
		RECIPIENT_NOT_FOUND,

		/**
		 * An {@link Invite} was sent to a recipient who is already a {@link Member} of the namespace,
		 * or has a pending {@link Invitation}.
		 */
		ALREADY_INVITED,

		/**
		 * The sender of the {@link Invite} is either not a namespace member or does not have
		 * sufficient permissions (requires {@link com.konfigyr.namespace.NamespaceRole#ADMIN}) to
		 * invite new members.
		 */
		INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN),

		/**
		 * The namespace does not have the {@link com.konfigyr.namespace.NamespaceFeatures#MEMBERS_COUNT}
		 * feature enabled, meaning invitations are not permitted under the current plan.
		 */
		NOT_ALLOWED(HttpStatus.BAD_REQUEST),

		/**
		 * The namespace has reached the maximum number of members defined by its
		 * {@link com.konfigyr.namespace.NamespaceFeatures#MEMBERS_COUNT} feature limit.
		 */
		MEMBER_LIMIT_REACHED(HttpStatus.BAD_REQUEST);

		final HttpStatusCode statusCode;

		ErrorCode() {
			this(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ErrorCode(HttpStatusCode statusCode) {
			this.statusCode = statusCode;
		}
	}
}
