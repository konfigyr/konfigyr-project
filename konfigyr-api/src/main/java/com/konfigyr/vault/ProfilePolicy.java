package com.konfigyr.vault;

/**
 * Enumeration that defines the governance and mutability rules for a configuration profile.
 * <p>
 * These policies express how configuration changes are allowed to flow into a profile and what safeguards
 * must be applied before a change becomes authoritative. It represents a high-level policy decision,
 * not a technical implementation detail.
 * <p>
 * Profile policies define whether configuration changes can be applied directly or require review. This
 * would indirectly drive the UX behavior as they govern how changesets may be submitted and applied.
 * For example, a protected profile requires explicit approval before submitted changes can be applied.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum ProfilePolicy {

	/**
	 * Fully mutable profile with no safeguards.
	 * <p>
	 * Changes can be applied directly once a {@code ChangeSet} is submitted, without requiring explicit
	 * approval. This policy is intended for non-critical environments where rapid iteration is prioritized
	 * over strict governance Typical use cases would include local development or test environments.
	 */
	UNPROTECTED,

	/**
	 * Governed profile requiring explicit approval before changes become authoritative.
	 * <p>
	 * ChangeSets targeting a protected profile must go through a review and approval process. Direct
	 * application of changes is not permitted.
	 * <p>
	 * This policy provides a balance between safety and flexibility and is typically used for production
	 * or pre-production environments.
	 */
	PROTECTED,

	/**
	 * Immutable, read-only profile.
	 * <p>
	 * No configuration changes may be applied to an immutable profile. The existing configuration remains
	 * readable and auditable but cannot be modified.
	 * <p>
	 * This policy is intended for frozen, deprecated, or compliance-bound environments where configuration
	 * drift must be prevented entirely.
	 */
	IMMUTABLE

}
