package com.konfigyr.artifactory.ownership;

/**
 * Lifecycle state of a group verification claim.
 */
public enum VerificationState {
    /**
     * The claim was created and is waiting for a successful proof.
     */
    PENDING,

    /**
     * The claim has been verified and is now active.
     */
    ACTIVE,

    /**
     * The claim was manually revoked or disabled.
     */
    REVOKED,

    /**
     * The claim attempt failed and is no longer valid.
     */
    FAILED;
}
