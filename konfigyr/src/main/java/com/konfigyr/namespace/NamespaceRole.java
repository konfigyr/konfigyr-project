package com.konfigyr.namespace;

/**
 * Defines the role of the {@link Member} within a {@link Namespace} and which actions could be performed.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public enum NamespaceRole {

	/**
	 * Administrator role allows members to perform sensitive operations in the namespace.
	 */
	ADMIN,

	/**
	 * Regular user role allows members to perform basic operations in the namespace.
	 */
	USER

}
