package com.konfigyr.vault;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;

import java.util.List;
import java.util.Optional;

/**
 * Interface that attemtps to describe an authoritative ledger for querying and tracing configuration
 * state transitions within a {@link Vault}.
 * <p>
 * The {@link VaultChronicle} acts as the system of record for all configuration changes applied
 * to a {@link Profile}. It maintains an immutable, append-only history of revisions, where each
 * revision consists of one or more {@link PropertyTransition}s.
 * <p>
 * All transitions handled by this interface are expected to contain sealed (encrypted) values.
 * Implementations must never persist or expose unsealed (plaintext) values.
 * <p>
 * This component serves multiple purposes:
 * <ul>
 *     <li>Auditability – providing a trace of who changed what and when</li>
 *     <li>Debugging – enabling inspection of configuration evolution</li>
 *     <li>Temporal queries – reconstructing state at a given point in time</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface VaultChronicle {

	/**
	 * Retrieves a paginated list of historical changes for the given {@link Profile}.
	 * <p>
	 * The result is ordered in reverse chronological order, most recent changes are shown first.
	 *
	 * @param profile the profile whose history is being queried, must not be {@code null}
	 * @param pageable the cursor pageable to be used to retrieve change history, must not be {@code null}
	 * @return a page containing matching history entries and pagination metadata
	 */
	CursorPage<ChangeHistory> fetchHistory(Profile profile, CursorPageable pageable);

	/**
	 * Retrieves a change history of a specific revision of a {@link Profile}.
	 *
	 * @param profile the profile to which the revision belongs, must not be {@code null}
	 * @param revision the unique revision identifier, must not be {@code null} or empty
	 * @return the matching revision or, or empty if not found
	 */
	Optional<ChangeHistory> examine(Profile profile, String revision);

	/**
	 * Traces the evolution of a specific property across multiple revisions of a {@link Profile}.
	 * <p>
	 * The result represents a chronological sequence of changes affecting the given property.
	 *
	 * @param profile the profile to which the property belongs, must not be {@code null}
	 * @param propertyName the name of the property, must not be {@code null} or empty
	 * @param pageable the cursor pageable to be used to retrieve property transitions, must not be {@code null}
	 * @return a page of property history records, never {@code null}
	 */
	CursorPage<PropertyHistory> traceProperty(Profile profile, String propertyName, CursorPageable pageable);

	/**
	 * Retrieves all property changes associated with a specific {@link ChangeHistory} revision
	 * of a {@link Profile}.
	 * <p>
	 * This method provides a low-level view of the changes that occurred within a single change.
	 *
	 * @param revision the change history revision, must not be {@code null} or empty
	 * @return a list of property history records associated with the revision, never {@code null}
	 */
	List<PropertyHistory> traceRevision(ChangeHistory revision);

}
