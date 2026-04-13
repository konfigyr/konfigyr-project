package com.konfigyr.vault.changes;

import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.ChangeRequestHistory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command representing a user-initiated review action on a change request.
 * <p>
 * The {@link ChangeRequestReviewCommand} unifies all review-related operations into a single,
 * intention-revealing value object. It captures both the identity of the target change request
 * and the action being performed, along with the acting principal and an optional comment.
 * <p>
 * The target {@link com.konfigyr.vault.ChangeRequest} is identified using the combination of
 * {@code service} and {@code number}, reflecting the domain constraint that change request
 * numbers are unique within the scope of a service.
 * <p>
 * The {@code operation} defines the semantic meaning of the command and determines how it will
 * affect the review state:
 * <ul>
 *     <li>{@link Operation#APPROVE} records an approval for the change request</li>
 *     <li>{@link Operation#REQUEST_CHANGES} indicates that modifications are required</li>
 *     <li>{@link Operation#COMMENT} adds a non-blocking comment</li>
 * </ul>
 * <p>
 * The {@code principal} represents the authenticated actor performing the operation and is expected
 * to be used for audit and history tracking purposes.
 * <p>
 * The optional {@code comment} provides additional context for the action and is particularly relevant
 * for change requests and general discussion.
 * <p>
 * This type is a pure value object and is safe to use across application boundaries. It does not
 * perform validation or enforce business rules; those concerns are handled by the application service
 * processing the command.
 *
 * @param service   the service to which the change request belongs
 * @param number    the change request number within the service scope
 * @param operation the review operation to perform
 * @param principal the authenticated user performing the action
 * @param comment   an optional comment providing context for the operation
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public record ChangeRequestReviewCommand(
		Service service,
		Long number,
		AuthenticatedPrincipal principal,
		Operation operation,
		@Nullable String comment
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 8232221754974152410L;

	/**
	 * Creates a new {@link ChangeRequestReviewCommand} instance for an operation that does not require a comment.
	 *
	 * @param service   the service to which the change request belongs
	 * @param number    the change request number within the service scope
	 * @param principal the authenticated user performing the action
	 * @param operation the review operation to perform
	 */
	public ChangeRequestReviewCommand(Service service, Long number, AuthenticatedPrincipal principal, Operation operation) {
		this(service, number, principal, operation, null);
	}

	/**
	 * Resolve the matching {@link ChangeRequestHistory.Type} for the given {@link Operation}.
	 *
	 * @return the matching {@link ChangeRequestHistory.Type}, never {@literal null}.
	 */
	ChangeRequestHistory.Type type() {
		return switch (operation) {
			case APPROVE -> ChangeRequestHistory.Type.APPROVED;
			case COMMENT -> ChangeRequestHistory.Type.COMMENTED;
			case REQUEST_CHANGES -> ChangeRequestHistory.Type.CHANGES_REQUESTED;
		};
	}

	/**
	 * Enumeration of supported review operations that can be applied to a change request.
	 * <p>
	 * Each operation corresponds to a distinct type of review event and contributes
	 * differently to the overall review state of the change request.
	 */
	public enum Operation {

		/**
		 * Indicates that the change request has been reviewed and approved.
		 * <p>
		 * Approvals contribute positively towards merge eligibility.
		 */
		APPROVE,

		/**
		 * Adds a comment to the change request without affecting its review status.
		 * <p>
		 * This operation is informational and does not block or enable merging.
		 */
		COMMENT,

		/**
		 * Indicates that the change request requires further modifications.
		 * <p>
		 * This operation typically blocks merging until the requested changes are addressed.
		 */
		REQUEST_CHANGES,
	}

}
