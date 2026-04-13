	package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestHistory;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import static com.konfigyr.data.tables.VaultChangeRequestEvents.VAULT_CHANGE_REQUEST_EVENTS;

/**
 * Represents a snapshot of the change request review state derived from the event history.
 * <p>
 * This snapshot abstracts away the underlying event model and exposes only the information
 * required for merge decisions.
 *
 * @param approved is this change request approved
 * @param changesRequested are additional changes requested for this change request
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
record ReviewSnapshot(boolean approved, boolean changesRequested) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5936555726403049696L;

	@RequiredArgsConstructor
	static final class Provider implements SnapshotProvider<ReviewSnapshot> {

		private final DSLContext context;

		@Override
		public ReviewSnapshot get(GateContext context) {
			final List<ChangeRequestHistory.Type> types = this.context.select(VAULT_CHANGE_REQUEST_EVENTS.TYPE)
					.from(VAULT_CHANGE_REQUEST_EVENTS)
					.where(DSL.and(
							VAULT_CHANGE_REQUEST_EVENTS.CHANGE_REQUEST_ID.eq(context.changeRequestId().get()),
							VAULT_CHANGE_REQUEST_EVENTS.TYPE.in(
									ChangeRequestHistory.Type.APPROVED.name(),
									ChangeRequestHistory.Type.CHANGES_REQUESTED.name()
							)
					))
					.fetch(VAULT_CHANGE_REQUEST_EVENTS.TYPE, ChangeRequestHistory.Type.class);

			return new ReviewSnapshot(
					types.contains(ChangeRequestHistory.Type.APPROVED),
					types.contains(ChangeRequestHistory.Type.CHANGES_REQUESTED)
			);
		}

	}

}
