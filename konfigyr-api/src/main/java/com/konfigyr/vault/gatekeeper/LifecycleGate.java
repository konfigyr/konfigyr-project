package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestMergeStatus;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class LifecycleGate implements Gate {

	static final int ORDER = HIGHEST_PRECEDENCE;

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public GateResult evaluate(GateContext context) {
		return switch (context.changeRequestState()) {
			case MERGED -> GateResult.block(ChangeRequestMergeStatus.NOT_OPEN, "Change request is already merged");
			case DISCARDED -> GateResult.block(ChangeRequestMergeStatus.NOT_OPEN, "Change request is discarded");
			default -> GateResult.pass();
		};
	}
}
