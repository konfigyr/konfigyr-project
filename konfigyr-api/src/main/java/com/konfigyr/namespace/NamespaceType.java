package com.konfigyr.namespace;

/**
 * Enumeration that defines the type of {@link Namespace}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see Namespace
 **/
public enum NamespaceType {
	/**
	 * Type used to define a personal {@link Namespace}, mainly used for personal or hobby use as
	 * it is not possible to add members to it.
	 */
	PERSONAL,

	/**
	 * When there is a need for collaboration between different people to manage artifacts or
	 * vaults within a {@link Namespace}, this type should be used.
	 */
	TEAM,

	/**
	 * {@link Namespace} with this type allows usage of a single-sign on (SSO) to manage members
	 * and accesses to artifacts and vaults.
	 */
	ENTERPRISE
}
