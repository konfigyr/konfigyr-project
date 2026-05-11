package com.konfigyr.vault.changes;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.ChangeRequestMergeStatus;
import com.konfigyr.vault.gatekeeper.ChangeRequestGatekeeper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;

/**
 * Component responsible for evaluating and persisting the merge status of change requests.
 * <p>
 * The {@link  ChangeRequestEvaluator} encapsulates the logic required to determine whether a
 * {@link com.konfigyr.vault.ChangeRequest} can be merged, requires attention, or is otherwise
 * blocked. It acts as the execution layer on top of the {@code gatekeeper}, which performs the
 * actual rule-based evaluation.
 * <p>
 * This type is typically invoked by the queue processing infrastructure and operates in an
 * asynchronous, event-driven manner. It is designed to be stateless and thread-safe, allowing
 * concurrent execution across multiple workers and application instances. This is the primary
 * reason why this processor is running under an eventual consistency model. The persisted merge
 * status reflects the outcome of the latest successful evaluation and may temporarily lag behind
 * the actual state of the system until re-evaluation is triggered.
 * <p>
 * The evaluator is responsible for delegating evaluation to the {@link ChangeRequestGatekeeper},
 * persisting the resulting {@link ChangeRequestMergeStatus} and providing observability through
 * logging and metrics.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor
class ChangeRequestEvaluator {

	private final DSLContext context;
	private final ChangeRequestGatekeeper gatekeeper;

	/**
	 * Evaluates the current merge status of the given change request and persists the result.
	 *
	 * <p>
	 * This method delegates the evaluation to the {@link ChangeRequestGatekeeper}, which applies
	 * a set of ordered rules to determine the appropriate {@link ChangeRequestMergeStatus}.
	 * These rules may consider:
	 * <ul>
	 *     <li>The current state of the change request</li>
	 *     <li>The state of the underlying repository (e.g., revisions, conflicts)</li>
	 *     <li>Historical events such as approvals or requested changes</li>
	 * </ul>
	 * <p>
	 * Any exception thrown during evaluation or persistence will propagate to the caller and
	 * should be handled by the surrounding queue infrastructure, which is responsible for
	 * retrying the operation with a configured backoff strategy.
	 *
	 * @param changeRequestId the identifier of the change request to evaluate, must not be {@literal null}
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, label = "vault.change-request.evaluator")
	void evaluate(EntityId changeRequestId) {
		final ChangeRequestMergeStatus status = gatekeeper.evaluate(changeRequestId);

		context.update(VAULT_CHANGE_REQUESTS)
				.set(VAULT_CHANGE_REQUESTS.MERGE_STATUS, status.name())
				.where(VAULT_CHANGE_REQUESTS.ID.eq(changeRequestId.get()))
				.execute();

		log.info("Change request merge status has been successfully updated to '{}' for: {}", status, changeRequestId);
	}

}
