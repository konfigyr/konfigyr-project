package com.konfigyr.vault.gatekeeper;

import com.konfigyr.vault.ChangeRequestMergeStatus;
import com.konfigyr.vault.ChangeRequestState;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LifecycleGateTest {

	final LifecycleGate gate = new LifecycleGate();

	@Mock
	GateContext context;

	@Test
	@DisplayName("should have the highest precedence specified as the order value")
	void assertHighestPrecedence() {
		assertThat(gate.getOrder())
				.isEqualTo(Ordered.HIGHEST_PRECEDENCE);
	}

	@Test
	@DisplayName("should evaluate to pass gate result when change request is in an open state")
	void evaluateForOpenChangeRequest() {
		doReturn(ChangeRequestState.OPEN).when(context).changeRequestState();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Pass.class);
	}

	@Test
	@DisplayName("should evaluate to merge status of 'not open' when change request is in a merged state")
	void evaluateForMergedChangeRequest() {
		doReturn(ChangeRequestState.MERGED).when(context).changeRequestState();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Block.class)
				.asInstanceOf(InstanceOfAssertFactories.type(GateResult.Block.class))
				.returns(ChangeRequestMergeStatus.NOT_OPEN, GateResult.Block::status)
				.returns("Change request is already merged", GateResult.Block::reason);
	}

	@Test
	@DisplayName("should evaluate to merge status  of 'not open' when change request is in a discarded state")
	void evaluateForDiscardedChangeRequest() {
		doReturn(ChangeRequestState.DISCARDED).when(context).changeRequestState();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Block.class)
				.asInstanceOf(InstanceOfAssertFactories.type(GateResult.Block.class))
				.returns(ChangeRequestMergeStatus.NOT_OPEN, GateResult.Block::status)
				.returns("Change request is discarded", GateResult.Block::reason);
	}

}
