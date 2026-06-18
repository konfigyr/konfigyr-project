package com.konfigyr.artifactory.ownership;

public interface VerificationStrategy {
	VerificationMethod method();

	VerificationResult verify(VerificationChallenge challenge);
}

