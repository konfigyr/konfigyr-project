package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NullMarked;

/**
 * SPI for proving ownership of a Maven {@code groupId} using a specific verification mechanism.
 * <p>
 * Each implementation encapsulates one verification approach (e.g. DNS {@code TXT} record lookup,
 * source code repository existence check) and is identified by the {@link VerificationMethod} it
 * returns from {@link #method()}. At most one implementation per method may be registered; the
 * {@link VerificationStrategies} registry enforces this at startup and throws
 * {@link IllegalArgumentException} on a duplicate.
 * <p>
 * Implementations must be registered as Spring beans. {@link VerificationStrategies} collects all
 * {@link VerificationStrategy} beans automatically and selects the correct one by matching
 * {@link #method()} to the {@link VerificationChallenge#method()} of the active challenge.
 * <p>
 * The {@link #verify(GroupVerification, VerificationChallenge)} contract requires that
 * implementations never throw: all external failures like DNS timeouts, HTTP errors, connection
 * problems; must be caught and mapped to an appropriate {@link VerificationResult.Failure}.
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 * @see VerificationMethod
 * @see VerificationResult
 * @see VerificationStrategies
 * @see GroupVerification
 * @see VerificationChallenge
 */
@NullMarked
public interface VerificationStrategy {

	/**
	 * Returns the {@link VerificationMethod} handled by this strategy.
	 * <p>
	 * This value is the dispatch key used by {@link VerificationStrategies} to select the correct
	 * implementation. Each registered strategy must return a unique method — duplicates are rejected
	 * at startup.
	 *
	 * @return the verification method handled by this strategy; never {@literal null}
	 */
	VerificationMethod method();

	/**
	 * Executes the ownership proof for the given verification using the supplied challenge token.
	 * <p>
	 * The implementation must perform the external check appropriate for its {@link VerificationMethod}
	 * — for example a DNS {@code TXT} record lookup or an HTTP request to a repository hosting API —
	 * and map the outcome to a {@link VerificationResult}.
	 * <p>
	 * <strong>Implementations must never throw.</strong> All external errors must be caught and
	 * returned as a {@link VerificationResult.Failure}. Use the standardised
	 * {@link VerificationResult.FailureReason} values whenever possible:
	 * <ul>
	 *     <li>{@link VerificationResult.FailureReason#TOKEN_MISMATCH} — the target was reachable but
	 *     did not carry the expected token.</li>
	 *     <li>{@link VerificationResult.FailureReason#TARGET_NOT_FOUND} — the verification target
	 *     (DNS record, repository) does not exist.</li>
	 *     <li>{@link VerificationResult.FailureReason#SERVICE_UNAVAILABLE} — the external service
	 *     is temporarily unreachable.</li>
	 *     <li>{@link VerificationResult.FailureReason#INTERNAL_ERROR} — any other unexpected
	 *     failure.</li>
	 * </ul>
	 * A free-form string reason may be used only when none of the above values apply.
	 *
	 * @param verification the ownership claim containing the target {@code groupId} and metadata
	 * @param challenge    the challenge holding the token to match against the external target
	 * @return a {@link VerificationResult.Success} when the token is confirmed, or a
	 *         {@link VerificationResult.Failure} describing why the check did not pass
	 */
	VerificationResult verify(GroupVerification verification, VerificationChallenge challenge);
}
