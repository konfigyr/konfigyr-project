package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestMergeStatus;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ReviewStateGate implements Gate {

	static final int ORDER = RepositoryStateGate.ORDER + 10;

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public GateResult evaluate(GateContext context) {
		final ReviewSnapshot snapshot = context.reviewSnapshot();

		if (snapshot.changesRequested()) {
			return GateResult.block(ChangeRequestMergeStatus.CHANGES_REQUESTED,
					"Additional changes were requested for this change request");
		}

		if (snapshot.approved()) {
			return GateResult.block(ChangeRequestMergeStatus.MERGEABLE, "Change request is approved");
		}

		return GateResult.pass();
	}
}
