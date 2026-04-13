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
class RepositoryStateGateTest {

	final RepositoryStateGate gate = new RepositoryStateGate();

	@Mock
	GateContext context;

	@Test
	@DisplayName("should have a lower precedence than lifecycle gate specified as the order value")
	void assertHighestPrecedence() {
		assertThat(gate.getOrder())
				.isGreaterThan(LifecycleGate.ORDER);
	}

	@Test
	@DisplayName("should evaluate to pass gate result when change request repository state has no conflicts and updates")
	void evaluateForUnchangedTargetRepository() {
		final var revision = "the-change-request-revision";
		final var snapshot = new RepositorySnapshot(revision, false);

		doReturn(revision).when(context).baseRevision();
		doReturn(snapshot).when(context).repositorySnapshot();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Pass.class);
	}

	@Test
	@DisplayName("should evaluate to merge status of 'conflicted' when change request is in a merged state")
	void evaluateForConflictedChangeRequest() {
		final var revision = "the-change-request-revision";
		final var snapshot = new RepositorySnapshot(revision, true);

		doReturn(revision).when(context).baseRevision();
		doReturn(snapshot).when(context).repositorySnapshot();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Block.class)
				.asInstanceOf(InstanceOfAssertFactories.type(GateResult.Block.class))
				.returns(ChangeRequestMergeStatus.CONFLICTING, GateResult.Block::status)
				.returns("Change request can not be merged due to conflicts in the repository state.",
						GateResult.Block::reason);
	}

	@Test
	@DisplayName("should evaluate to merge status of 'outdated' when change request base revision is outdated")
	void evaluateForDiscardedChangeRequest() {
		final var snapshot = new RepositorySnapshot("new-revision", true);

		doReturn("base-revision").when(context).baseRevision();
		doReturn(snapshot).when(context).repositorySnapshot();

		assertThat(gate.evaluate(context))
				.isInstanceOf(GateResult.Block.class)
				.asInstanceOf(InstanceOfAssertFactories.type(GateResult.Block.class))
				.returns(ChangeRequestMergeStatus.OUTDATED, GateResult.Block::status)
				.returns(
						"Changeset is outdated, StateRepository(revision='new-revision') != ChangeRequest(baseRevision='base-revision')",
						GateResult.Block::reason
				);
	}

}
