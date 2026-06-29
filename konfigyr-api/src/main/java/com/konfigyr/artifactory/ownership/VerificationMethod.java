package com.konfigyr.artifactory.ownership;

/**
 * Verification methods supported for proving ownership of a groupId.
 *
 * @author Vitalii Kushnir
 * @since 1.0.0
 */
public enum VerificationMethod {
	/**
	 * DNS-based verification using a DNS challenge record.
	 */
	DNS,

	/**
	 * Source code host verification using a temporary public repository as proof (etc. GitHub, GitLab...).
	 */
	SOURCE_CODE
}
