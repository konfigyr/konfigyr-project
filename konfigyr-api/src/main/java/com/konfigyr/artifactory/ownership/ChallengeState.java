package com.konfigyr.artifactory.ownership;

/**
 * Lifecycle state of a verification challenge attempt.
 */
public enum ChallengeState {
    /**
     * The challenge has been issued but not yet validated.
     */
    UNVERIFIED,

    /**
     * The challenge was successfully validated.
     */
    VERIFIED,

    /**
     * The challenge is no longer valid and can no longer be used.
     */
    EXPIRED;
}
