package com.konfigyr.vault.gatekeeper;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.ChangeRequestMergeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestGatekeeperTest {

	@Mock
	GateContextFactory contextFactory;

	@Mock
	GateContext context;

	@Test
	@DisplayName("should fail to create a gatekeeper instance with no rules")
	void createGatekeeperWithoutRules() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ChangeRequestGatekeeper(contextFactory, List.of()))
				.withMessage("At least one gate must be configured");
	}

	@Test
	@DisplayName("should fail to evaluate due to context factory error")
	void contextFactoryFailure() {
		final var gate = mock(Gate.class);
		final var gatekeeper = new ChangeRequestGatekeeper(contextFactory, List.of(gate));

		final var cause = new RuntimeException("context factory failure");
		doThrow(cause).when(contextFactory).create(EntityId.from(1246525));

		assertThatException()
				.isThrownBy(() -> gatekeeper.evaluate(EntityId.from(1246525)))
				.isEqualTo(cause);

		verify(contextFactory).create(EntityId.from(1246525));
		verifyNoInteractions(gate);
	}

	@Test
	@DisplayName("should fail to evaluate due to gate processing error")
	void gateProcessingFailure() {
		final var gate = mock(Gate.class);
		final var gatekeeper = new ChangeRequestGatekeeper(contextFactory, List.of(gate));

		final var cause = new RuntimeException("context factory failure");
		doThrow(cause).when(gate).evaluate(context);
		doReturn(context).when(contextFactory).create(EntityId.from(1246525));

		assertThatException()
				.isThrownBy(() -> gatekeeper.evaluate(EntityId.from(1246525)))
				.isEqualTo(cause);

		verify(contextFactory).create(EntityId.from(1246525));
		verify(gate).evaluate(context);
	}

	@Test
	@DisplayName("gatekeeper should stop at the first rule that provides a blocking result")
	void stopForFirstBlockingResult() {
		final var first = gateFor(1, GateResult.pass());
		final var second = gateFor(2, GateResult.block(ChangeRequestMergeStatus.NOT_OPEN, "It's closed, duh!"));
		final var third = gateFor(3, GateResult.pass());

		final var gatekeeper = new ChangeRequestGatekeeper(contextFactory, List.of(first, second, third));

		doReturn(context).when(contextFactory).create(EntityId.from(96736));

		assertThat(gatekeeper.evaluate(EntityId.from(96736)))
				.isEqualTo(ChangeRequestMergeStatus.NOT_OPEN);

		verify(contextFactory).create(EntityId.from(96736));
		verify(first).evaluate(context);
		verify(second).evaluate(context);
		verify(third, never()).evaluate(context);
	}

	@Test
	@DisplayName("gatekeeper should return mergeable state when gates do not provide a blocking result")
	void evaluateToDefaultMergeableState() {
		final var first = gateFor(1, GateResult.pass());
		final var second = gateFor(2, GateResult.pass());
		final var third = gateFor(3, GateResult.pass());

		final var gatekeeper = new ChangeRequestGatekeeper(contextFactory, List.of(first, second, third));

		doReturn(context).when(contextFactory).create(EntityId.from(586152));

		assertThat(gatekeeper.evaluate(EntityId.from(586152)))
				.isEqualTo(ChangeRequestMergeStatus.MERGEABLE);

		verify(contextFactory).create(EntityId.from(586152));
		verify(first).evaluate(context);
		verify(second).evaluate(context);
		verify(third).evaluate(context);
	}

	static Gate gateFor(int order, GateResult result) {
		final var gate = mock(Gate.class, withSettings()
				.strictness(Strictness.LENIENT)
				.name(String.format("MockedGate(order=%d, result=%s)", order, result)));

		doReturn(order).when(gate).getOrder();
		doReturn(result).when(gate).evaluate(any());
		return gate;
	}

}
