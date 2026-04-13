package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.state.RepositoryState;
import com.konfigyr.vault.state.StateRepository;
import com.konfigyr.vault.state.StateRepositoryFactory;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a snapshot of {@link com.konfigyr.vault.state.StateRepository} relevant
 * for merge evaluation.
 * <p>
 * This includes information such as the current target profile branch head and whether
 * the change request can be merged cleanly.
 *
 * @param revision the current revision of the target profile branch
 * @param conflicted are there conflicts between the target and changeset branches
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
record RepositorySnapshot(String revision, boolean conflicted) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5936555726403049696L;

	@RequiredArgsConstructor
	static final class Provider implements SnapshotProvider<RepositorySnapshot> {

		private final StateRepositoryFactory stateRepositoryFactory;

		@Override
		public RepositorySnapshot get(GateContext context) {
			final StateRepository repository = stateRepositoryFactory.get(context.service());
			final RepositoryState state = repository.get(context.profile());

			return new RepositorySnapshot(state.revision(), false);
		}

	}

}
