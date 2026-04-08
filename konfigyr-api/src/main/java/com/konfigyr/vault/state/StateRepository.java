package com.konfigyr.vault.state;

import com.konfigyr.namespace.Service;
import com.konfigyr.vault.Profile;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;

/**
 * Interface that provides an abstraction over a source-control configuration state repository.
 * <p>
 * {@link StateRepository} defines a high-level, domain-oriented API for managing service configuration stored
 * in a version-controlled backend (e.g., Git), without exposing VCS-specific concepts to consumers.
 *
 * <h3>Conceptual model</h3>
 * <ul>
 *   <li>
 *       <b>Service:</b> a logical unit owning a dedicated repository.
 *   </li>
 *   <li>
 *       <b>Profile:</b> a long-lived branch representing a stable configuration variant, usually an environment
 *       or a deployment profile.
 *   </li>
 *   <li>
 *       <b>Changeset:</b> a short-lived branch created from a profile to stage and review configuration changes
 *       before applying them.
 *   </li>
 * </ul>
 * The typical workflow is:
 * <ol>
 *   <li>Initialize a service repository.</li>
 *   <li>Create one or more profile branches.</li>
 *   <li>Create a changeset from a profile.</li>
 *   <li>Modify the changeset contents.</li>
 *   <li>Apply (squash and merge) or discard the changeset.</li>
 * </ol>
 *
 * <h3>Implementation guidelines</h3>
 * <ul>
 *   <li>
 *       All operations should be idempotent where reasonably possible.
 *   </li>
 *   <li>
 *       The repository should be closed when required operations are executed. Performing operations on a
 *       closed repository should result in an error.
 *   </li>
 *   <li>
 *       Methods should fail fast and clearly when invariants are violated. For instance, if there is a missing
 *       profile, an invalid changeset contents, conflicts...
 *   </li>
 *   <li>
 *       Concurrency control (locking, optimistic checks) is the responsibility of the implementation.
 *   </li>
 *   <li>
 *       No method should rely on a mutable global state.
 *   </li>
 *   <li>
 *       The implementation should avoid exposing VCS-specific concepts to consumers, (e.g., commit hashes, refs).
 *       Performing implicit merges or destructive operations or allowing direct profile modification without
 *       using a changeset.
 *   </li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
public interface StateRepository extends AutoCloseable, DisposableBean {

	/**
	 * Returns a {@link Service} instance that owns this repository.
	 *
	 * @return the repository owner, never {@literal null}.
	 */
	Service owner();

	/**
	 * Creates a new empty profile configuration state for the given {@link Profile} within this repository.
	 * <p>
	 * A profile represents a stable, long-lived configuration branch. Profiles should be created sparingly and
	 * are expected to outlive individual changesets.
	 * <p>
	 * Implementations should prevent accidental overwriting of existing profiles.
	 *
	 * @param profile the profile for which the state branch would be created, can't be {@literal null}
	 * @return the ref name of the newly created profile branch, never {@literal null}.
	 */
	String create(Profile profile);

	/**
	 * Retrieves the current configuration state of the given {@link Profile} as a binary {@link InputStream}.
	 * <p>
	 * This represents the authoritative configuration state that consumers should treat as active and stable.
	 * <p>
	 * Implementations should ensure this method is read-only and has no side effects.
	 *
	 * @param profile the profile for which the state would be retrieved, can't be {@literal null}
	 * @return the profile state contents, never {@literal null}.
	 */
	RepositoryState get(Profile profile);

	/**
	 * Retrieves the current configuration state of the given {@link Profile} as a binary {@link InputStream}.
	 * <p>
	 * This represents the authoritative configuration state that consumers should treat as active and stable.
	 * <p>
	 * Implementations should ensure this method is read-only and has no side effects.
	 *
	 * @param profile the profile for which the state would be retrieved, can't be {@literal null}
	 * @param changeset the changeset branch identifier, can't be {@literal null}.
	 * @return the profile state contents, never {@literal null}.
	 */
	RepositoryState get(Profile profile, String changeset);

	/**
	 * Updates the configuration state of the given {@link Profile} with the given {@link Changeset}.
	 * <p>
	 * This method represents the primary mutation point for configuration changes. All edits should occur
	 * in a dedicated changeset branch and never directly on a profile.
	 * <p>
	 * Implementations should treat the update atomically and avoid partial writes.
	 *
	 * @param profile the profile for which the changes would be prepared, can't be {@literal null}
	 * @param changeset the changeset to be applied, can't be {@literal null}
	 * @return the update outcome, never {@literal null}.
	 */
	MergeOutcome update(Profile profile, Changeset changeset);

	/**
	 * Updates the authoritative configuration state of the {@link Profile} by applying the changes
	 * from the given changeset identifier.
	 * <p>
	 * It is encouraged that the implementations perform this operation using the squash-and-merge strategy.
	 * The changeset data merged into the target profile would then be producing a single logical change.
	 * <p>
	 * After a successful application, the changeset is expected to be removed or rendered unusable.
	 *
	 * @param profile the profile to which the changeset would be applied to, can't be {@literal null}
	 * @param changeset the changeset to be applied, can't be {@literal null}
	 * @return the merge outcome, never {@literal null}.
	 */
	MergeOutcome merge(Profile profile, String changeset);

	/**
	 * Discards a changeset and all staged changes.
	 * <p>
	 * This operation should leave the target profile completely unchanged.
	 * <p>
	 * Implementations should ensure that discarded changesets cannot be accidentally reused.
	 *
	 * @param profile the profile for which the changes would be discarded, can't be {@literal null}
	 * @param changeset the changeset to be discarded, can't be {@literal null}
	 */
	void discard(Profile profile, String changeset);

	/**
	 * Permanently deletes the configuration state of the given {@link Profile}.
	 * <p>
	 * This is a destructive operation and should be used with extreme care. Implementations are encouraged to
	 * enforce safeguards or confirmations at higher layers.
	 *
	 * @param profile the profile for which the state would be destroyed, can't be {@literal null}
	 */
	void delete(Profile profile);

	/**
	 * Retrieves the Git commit history for the specified profile.
	 *
	 * @param profile the profile for which the state would be destroyed, can't be {@literal null}
	 * @param pageable paging and sorting instructions
	 *
	 * @return paged collections of repository versions, never {@literal null}
	 */
	Page<RepositoryVersion> history(Profile profile, Pageable pageable);

	/**
	 * Destroys the repository associated with the {@link Service} and all its contents.
	 * <p>
	 * This is a terminal operation intended for service decommissioning. Once invoked, all profiles,
	 * and history are permanently removed.
	 * <p>
	 * This method should be used only when the service lifecycle has ended.
	 */
	@Override
	void destroy() throws Exception;
}
