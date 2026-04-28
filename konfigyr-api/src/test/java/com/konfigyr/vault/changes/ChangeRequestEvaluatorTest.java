package com.konfigyr.vault.changes;

import com.konfigyr.entity.EntityId;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.vault.ChangeRequestMergeStatus;
import com.konfigyr.vault.gatekeeper.ChangeRequestGatekeeper;
import org.assertj.core.api.ObjectAssert;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestEvaluatorTest extends AbstractIntegrationTest {

	@Mock
	ChangeRequestGatekeeper gatekeeper;

	@Autowired
	DSLContext context;

	ChangeRequestEvaluator evaluator;

	@BeforeEach
	void setup() {
		evaluator = new ChangeRequestEvaluator(context, gatekeeper);
	}

	@Test
	@Transactional
	@DisplayName("should evaluate and update the change request merge status")
	void updateChangeRequestMergeStatus() {
		doReturn(ChangeRequestMergeStatus.CONFLICTING).when(gatekeeper).evaluate(EntityId.from(2));

		assertMergeStateFor(2)
				.isEqualTo(ChangeRequestMergeStatus.MERGEABLE);

		assertThatNoException()
				.as("should rethrow gatekeeper exceptions")
				.isThrownBy(() -> evaluator.evaluate(EntityId.from(2)));

		assertMergeStateFor(2)
				.isEqualTo(ChangeRequestMergeStatus.CONFLICTING);

		verify(gatekeeper).evaluate(EntityId.from(2));
	}

	@Test
	@Transactional
	@DisplayName("should evaluate to the same change request merge status")
	void changeRequestMergeStatusUnchanged() {
		doReturn(ChangeRequestMergeStatus.CHANGES_REQUESTED).when(gatekeeper).evaluate(EntityId.from(3));

		assertMergeStateFor(3)
				.isEqualTo(ChangeRequestMergeStatus.CHANGES_REQUESTED);

		assertThatNoException()
				.as("should rethrow gatekeeper exceptions")
				.isThrownBy(() -> evaluator.evaluate(EntityId.from(3)));

		assertMergeStateFor(3)
				.isEqualTo(ChangeRequestMergeStatus.CHANGES_REQUESTED);

		verify(gatekeeper).evaluate(EntityId.from(3));
	}

	@Test
	@DisplayName("should not catch any gatekeeper exceptions")
	void rethrowExceptions() {
		final var cause = new IllegalStateException("gatekeeper failure");
		doThrow(cause).when(gatekeeper).evaluate(EntityId.from(1));

		assertMergeStateFor(1)
				.isEqualTo(ChangeRequestMergeStatus.MERGEABLE);

		assertThatException()
				.as("should rethrow gatekeeper exceptions")
				.isThrownBy(() -> evaluator.evaluate(EntityId.from(1)))
				.isEqualTo(cause);

		assertMergeStateFor(1)
				.isEqualTo(ChangeRequestMergeStatus.MERGEABLE);

		verify(gatekeeper).evaluate(EntityId.from(1));
	}

	ObjectAssert<ChangeRequestMergeStatus> assertMergeStateFor(long id) {
		return assertThatObject(
				context.select(VAULT_CHANGE_REQUESTS.MERGE_STATUS)
						.from(VAULT_CHANGE_REQUESTS)
						.where(VAULT_CHANGE_REQUESTS.ID.eq(id))
						.fetchOne(VAULT_CHANGE_REQUESTS.MERGE_STATUS, ChangeRequestMergeStatus.class)
		);
	}

}
