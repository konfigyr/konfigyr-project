package com.konfigyr.artifactory.ownership;

import org.jspecify.annotations.NullMarked;

/**
 * Defines a strategy for verifying ownership of a {@code groupId} using a specific verification mechanism.
 * <p>
 * Implementations encapsulate different verification approaches (e.g. DNS TXT records, source code hosting
 * providers, etc.), each identified by a {@link VerificationMethod}.
 * <p>
 * A strategy receives a {@link GroupVerification} request and a {@link VerificationChallenge}, and must
 * return a {@link VerificationResult} indicating whether verification succeeded or failed, including
 * a reason in case of failure.
 *
 * @author Mila Zarkovic
 * @see VerificationMethod
 * @see VerificationResult
 * @see GroupVerification
 * @see VerificationChallenge
 */
@NullMarked
public interface VerificationStrategy {

	/**
	 * Returns the verification method supported by this strategy.
	 * <p>
	 * This is used to route verification requests to the appropriate implementation.
	 *
	 * @return the {@link VerificationMethod} handled by this strategy
	 */
	VerificationMethod method();

	/**
	 * Executes verification of a {@code groupId} using the provided challenge.
	 * <p>
	 * The implementation should perform the necessary external checks (e.g. DNS lookup,
	 * HTTP request to a repository hosting API, etc.) and return a {@link VerificationResult}
	 * describing the outcome.
	 *
	 * @param verification the verification request containing the target group and metadata
	 * @param challenge the challenge data used to prove ownership
	 * @return the result of the verification attempt
	 */
	VerificationResult verify(GroupVerification verification, VerificationChallenge challenge);
}

