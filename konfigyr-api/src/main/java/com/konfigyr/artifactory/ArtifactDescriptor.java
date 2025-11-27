package com.konfigyr.artifactory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.net.URI;

/**
 * Interface that defines the basic information about an artifact that is managed by the
 * {@link Artifactory}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public interface ArtifactDescriptor extends Serializable {

	/**
	 * Returns the {@code groupId} Maven coordinate of the artifact.
	 *
	 * @return the {@code groupId} Maven coordinate, can't be {@literal null}
	 */
	@NonNull
	String groupId();

	/**
	 * Returns the {@code artifactId} Maven coordinate of the artifact.
	 *
	 * @return the {@code artifactId} Maven coordinate, can't be {@literal null}
	 */
	@NonNull
	String artifactId();

	/**
	 * The human-readable name of the artifact.
	 *
	 * @return artifact name, can be {@literal null}.
	 */
	@Nullable
	String name();

	/**
	 * The textual description of the artifact.
	 *
	 * @return artifact description, can be {@literal null}.
	 */
	@Nullable
	String description();

	/**
	 * Artifacts can contain a reference link to a website that contains documentation or additional
	 * information about the artifact.
	 *
	 * @return website URI, may be {@literal null}.
	 */
	@Nullable
	URI website();

	/**
	 * Artifacts can contain a reference link to a source control repository (SCM URL).
	 *
	 * @return repository URI, may be {@literal null}.
	 */
	@Nullable
	URI repository();

}
