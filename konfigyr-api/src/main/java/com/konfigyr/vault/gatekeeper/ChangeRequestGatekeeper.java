package com.konfigyr.vault.gatekeeper;

import com.konfigyr.entity.EntityId;
import com.konfigyr.vault.ChangeRequestMergeStatus;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.OrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Central coordinator responsible for evaluating all {@link Gate} evaluation rules
 * against a given {@link GateContext} that is created for the affected change request.
 * <p>
 * The gatekeeper executes gates in a deterministic order and stops at the first gate that
 * produces a blocking result. If no gate blocks the request, the change request is considered
 * {@link ChangeRequestMergeStatus#MERGEABLE}.
 * <p>
 * The gatekeeper itself contains no domain logic; it merely orchestrates the evaluation process.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
public final class ChangeRequestGatekeeper {

	private final List<? extends Gate> gates;
	private final GateContextFactory contextFactory;

	ChangeRequestGatekeeper(GateContextFactory contextFactory, Iterable<? extends Gate> gates) {
		this.contextFactory = contextFactory;
		this.gates = StreamSupport.stream(gates.spliterator(), false)
				.sorted(OrderComparator.INSTANCE)
				.toList();

		Assert.notEmpty(this.gates, "At least one gate must be configured");
	}

	/**
	 * Evaluates the change request merge state against all configured gates for the given
	 * {@link com.konfigyr.vault.ChangeRequest} entity identifier.
	 *
	 * @param changeRequestId the change request entity identifier, can't be {@literal null}
	 * @return the resulting {@link ChangeRequestMergeStatus}, never {@literal null}
	 */
	public ChangeRequestMergeStatus evaluate(EntityId changeRequestId) {
		if (log.isDebugEnabled()) {
			log.debug("Evaluating change request with {} against {} gate(s)", changeRequestId, gates.size());
		}

		final GateContext context = contextFactory.create(changeRequestId);

		for (Gate gate : gates) {
			final GateResult result = gate.evaluate(context);

			if (log.isDebugEnabled()) {
				log.debug("Gatekeeper obtained evaluation result: [gate={}, result={}, context={}]",
						ClassUtils.getShortName(gate.getClass()), result, context);
			}

			if (result instanceof GateResult.Block(ChangeRequestMergeStatus status, String reason)) {
				log.info("Gatekeeper evaluated merge status of '{}' (reason: {}) using '{}' for change request: {}",
						status, reason, ClassUtils.getShortName(gate.getClass()), context.changeRequestId());
				return status;
			}
		}

		return ChangeRequestMergeStatus.MERGEABLE;
	}

}
