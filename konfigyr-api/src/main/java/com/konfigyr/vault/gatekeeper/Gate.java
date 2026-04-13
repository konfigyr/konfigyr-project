package com.konfigyr.vault.gatekeeper;

import org.jspecify.annotations.NullMarked;
import org.springframework.core.Ordered;

/**
 * Represents a single decision point in the change request merge evaluation process.
 * <p>
 * A {@link Gate} inspects the provided {@link GateContext} and determines whether the
 * {@link com.konfigyr.vault.ChangeRequest} can proceed or must be blocked for a specific
 * reason.
 * <p>
 * Gates are evaluated sequentially by the {@link ChangeRequestGatekeeper}. The first gate that produces
 * a blocking result terminates the evaluation chain.
 * <p>
 * Implementations must be side effect-free, and should rely exclusively on the data exposed
 * through the context. Any expensive operations should be performed through lazily resolved
 * snapshots provided by the context.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
interface Gate extends Ordered {

	/**
	 * Evaluates this gate against the provided context.
	 *
	 * @param context the evaluation context
	 * @return a {@link GateResult} indicating whether evaluation should continue or be
	 * terminated with a blocking status
	 */
	GateResult evaluate(GateContext context);

}
