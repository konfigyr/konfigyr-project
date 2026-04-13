package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestMergeStatus;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class RepositoryStateGate implements Gate {

	static final int ORDER = LifecycleGate.ORDER + 10;

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public GateResult evaluate(GateContext context) {
		final RepositorySnapshot snapshot = context.repositorySnapshot();

		if (!context.baseRevision().equals(snapshot.revision())) {
			return GateResult.block(ChangeRequestMergeStatus.OUTDATED,
					"Changeset is outdated, StateRepository(revision='%s') != ChangeRequest(baseRevision='%s')"
							.formatted(snapshot.revision(), context.baseRevision()));
		}

		if (snapshot.conflicted()) {
			return GateResult.block(ChangeRequestMergeStatus.CONFLICTING,
					"Change request can not be merged due to conflicts in the repository state.");
		}

		return GateResult.pass();
	}
}
