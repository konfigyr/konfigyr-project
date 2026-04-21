package com.konfigyr.vault;

import com.konfigyr.markdown.MarkdownContents;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;

/**
 * Immutable representation of a historical event associated with a {@link ChangeRequest}.
 * <p>
 * This type models a single entry in the change request history and serves as the authoritative audit
 * record of what happened, when it happened, and why. The change request history is <b>append-only</b>
 * and should be treated as an event log rather than a mutable state store. Each instance represents a
 * fact that has already occurred and must never be modified or deleted.
 * <p>
 * The {@link Type} defines the nature of the event. Typical events include:
 * </p>
 * <ul>
 *     <li>{@link Type#APPROVED} – Change request was approved</li>
 *     <li>{@link Type#CHANGES_REQUESTED} – Additional changes requested</li>
 *     <li>{@link Type#MERGED} – Change Request was merged into main</li>
 *     <li>{@link Type#REBASED} – Branch was rebased onto a newer base</li>
 *     <li>{@link Type#DISCARDED} – Change Request was closed without merge</li>
 * </ul>
 *
 * @param id the unique identifier of the change request history entry, never {@literal null}
 * @param type the type of the event, never {@literal null}
 * @param comment the comment associated with the event, can be {@literal null}
 * @param details additional details about the event, can't be {@literal null}
 * @param initiator the name of the principal that initiated the event; never {@literal null}
 * @param timestamp the time at which the event occurred, never {@literal null}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see ChangeRequest
 */
@Entity
@NullMarked
public record ChangeRequestHistory(
	@Identity String id,
	Type type,
	@Nullable MarkdownContents comment,
	@Nullable ObjectNode details,
	String initiator,
	OffsetDateTime timestamp
) {

	/**
	 * Enumeration of supported change request history event types.
	 */
	public enum Type {

		/**
		 * The change request was created.
		 */
		CREATED,

		/**
		 * The change request was approved.
		 */
		APPROVED,

		/**
		 * The change request was commented on.
		 */
		COMMENTED,

		/**
		 * The changes within the change request need to be reworked before they can be merged.
		 */
		CHANGES_REQUESTED,

		/**
		 * The change request subject was changed.
		 */
		RENAMED,

		/**
		 * The change request was merged into the main branch.
		 */
		MERGED,

		/**
		 * The change request branch was rebased onto a newer base commit.
		 */
		REBASED,

		/**
		 * The change request was closed without being merged.
		 */
		DISCARDED
	}

}
