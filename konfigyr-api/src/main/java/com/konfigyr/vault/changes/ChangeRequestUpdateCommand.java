package com.konfigyr.vault.changes;

import com.konfigyr.markdown.MarkdownContents;
import com.konfigyr.namespace.Service;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.ChangeRequest;
import com.konfigyr.vault.ChangeRequestState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command representing a metadata update on a change request.
 * <p>
 * This command allows updating the status, subject and/or description of a change request.
 * Fields are nullable to support partial updates, where only the provided values are modified.
 * <p>
 * The target {@link ChangeRequest} is identified using the combination of {@code service} and
 * {@code number}, reflecting the domain constraint that change request numbers are unique within
 * the scope of a service.
 * <p>
 * This operation is typically lightweight and does not influence merge decisions but may
 * optionally be recorded for audit purposes.
 *
 * @param service     the service to which the change request belongs
 * @param number      the change request number within the service scope
 * @param principal   the authenticated user performing the action
 * @param state       the new change request state, or {@code null} if unchanged
 * @param subject     the new subject, or {@code null} if unchanged
 * @param description the new description, or {@code null} if unchanged
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public record ChangeRequestUpdateCommand(
		Service service,
		Long number,
		AuthenticatedPrincipal principal,
		@Nullable ChangeRequestState state,
		@Nullable String subject,
		@Nullable MarkdownContents description
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 3346708491584767438L;

	public ChangeRequestUpdateCommand(ChangeRequest request, AuthenticatedPrincipal principal, ChangeRequestState state) {
		this(request.service(), request.number(), principal, state, null, null);
	}
}
