package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestMergeStatus;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class ReviewStateGateTest {

	final ReviewStateGate gate = new ReviewStateGate();

	@Mock
	GateContext context;

	@Test
	@DisplayName("should have a lower precedence than repository state gate specified as the order value")
	void assertHighestPrecedence() {
		assertThat(gate.getOrder())
				.isGreaterThan(RepositoryStateGate.ORDER);
	}

	@Test
	@DisplayName("should evaluate to pass gate result when change request history state has no submitted reviews")
	void evaluateForUnreviewedChangeRequest() {
		final var snapshot = new ReviewSnapshot(false, false);
		doReturn(snapshot).when(context).reviewSnapshot();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Pass.class);
	}

	@Test
	@DisplayName("should evaluate to merge status of 'mergable' when change request has been approved")
	void evaluateForApprovedChangeRequest() {
		final var snapshot = new ReviewSnapshot(true, false);
		doReturn(snapshot).when(context).reviewSnapshot();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Block.class)
				.asInstanceOf(InstanceOfAssertFactories.type(GateResult.Block.class))
				.returns(ChangeRequestMergeStatus.MERGEABLE, GateResult.Block::status)
				.returns("Change request is approved", GateResult.Block::reason);
	}

	@Test
	@DisplayName("should evaluate to merge status of 'changes request' when change request needs to be amended")
	void evaluateForAmendingChangeRequest() {
		final var snapshot = new ReviewSnapshot(true, true);
		doReturn(snapshot).when(context).reviewSnapshot();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Block.class)
				.asInstanceOf(InstanceOfAssertFactories.type(GateResult.Block.class))
				.returns(ChangeRequestMergeStatus.CHANGES_REQUESTED, GateResult.Block::status)
				.returns("Additional changes were requested for this change request", GateResult.Block::reason);
	}

}
