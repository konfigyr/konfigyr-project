package com.konfigyr.artifactory.ownership;

/**
 * Verification methods supported for proving ownership of a groupId.
 */
public enum VerificationMethod {
    /**
     * DNS-based verification using a DNS challenge record.
     */
    DNS,

    /**
     * GitHub-based verification using a repository-backed proof.
     */
    GITHUB;
}
