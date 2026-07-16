package com.konfigyr.artifactory;

/**
 * Determines which namespaces are allowed to read an {@link ArtifactDefinition} and its
 * {@link VersionedArtifact versions}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public enum ArtifactVisibility {

	/**
	 * The artifact can be read by any namespace.
	 */
	PUBLIC,

	/**
	 * The artifact can only be read by its owning namespace.
	 */
	PRIVATE

}
