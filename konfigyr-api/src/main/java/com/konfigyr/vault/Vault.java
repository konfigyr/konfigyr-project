package com.konfigyr.vault;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.namespace.Service;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Represents a secured configuration vault for a specific {@link Service} and {@link Profile}. To access
 * a {@link Vault} instance, please use the {@link VaultAccessor} that would enforce access control
 * and resolution.
 * <p>
 * A {@link Vault} provides controlled access to configuration state and encapsulates the underlying
 * storage mechanics (e.g., Git-backed repositories, encryption handling, and change workflows).
 * <p>
 * The vault manages configuration state as encrypted properties. The state may be:
 * <ul>
 *     <li>Read in sealed form via {@link #state()}</li>
 *     <li>Unsealed for consumption via {@link #unseal()}</li>
 *     <li>Modified via {@link #apply(PropertyChanges)} or {@link #submit(PropertyChanges)}</li>
 * </ul>
 * <p>
 * Implementations may hold underlying resources such as file handles or Git repository instances.
 * Therefore, {@code Vault} extends {@link AutoCloseable}. Callers must ensure {@link #close()} is
 * invoked when the vault is no longer needed, preferably using try-with-resources.
 * <p>
 * <strong>Thread safety and mutability</strong>
 * <p>
 * {@link Vault} instances are not guaranteed to be thread-safe. They should be treated as
 * request-scoped resources and must not be shared across concurrent threads.
 * <p>
 * Implementations are allowed to be mutable. Invoking mutation operations such as
 * {@link #apply(PropertyChanges)} or {@link #submit(PropertyChanges)} may alter the internal
 * state of the vault instance, including its in-memory configuration representation and
 * underlying repository state.
 * <p>
 * After a successful mutation operation, later calls to {@link #state()} or {@link #unseal()}
 * may reflect the updated configuration state.
 * <p>
 * Callers should therefore treat a {@link Vault} instance as a short-lived, stateful object
 * tied to a single logical operation or request lifecycle.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see VaultAccessor
 */
@NullMarked
public interface Vault extends AutoCloseable {

	/**
	 * Returns the service that owns this vault.
	 *
	 * @return the service, never {@literal null}.
	 */
	Service service();

	/**
	 * Returns the profile for which this vault is configured.
	 *
	 * @return the service, never {@literal null}.
	 */
	Profile profile();

	/**
	 * Returns the current sealed configuration state.
	 * <p>
	 * The returned {@link Properties} instance contains encrypted property values and represents
	 * the authoritative persisted state.
	 *
	 * @return the current sealed configuration state, never {@literal null}
	 */
	Properties state();

	/**
	 * Unseals encrypted property values and returns them as plain-text key-value pairs.
	 * <p>
	 * This operation uses the configured {@link KeysetOperations} to decrypt property values.
	 * Callers must treat the returned data as sensitive.
	 * <p>
	 * Implementations should avoid caching unsealed values beyond the scope of the current request.
	 *
	 * @return a map of unsealed configuration values, never {@literal null}
	 */
	Map<String, String> unseal();

	/**
	 * Seals the given property value.
	 * <p>
	 * This operation uses the configured {@link KeysetOperations} to encrypt the property value.
	 * <p>
	 * Implementations should avoid caching the unsealed value beyond the scope of the current request.
	 *
	 * @param property the property value to seal, cannot be {@literal null}
	 * @return the sealed property value, never {@literal null}
	 */
	PropertyValue seal(PropertyValue property);

	/**
	 * Unseals the given property value.
	 * <p>
	 * This operation uses the configured {@link KeysetOperations} to decrypt the property value.
	 * Callers must treat the returned data as sensitive.
	 * <p>
	 * Implementations should avoid caching the unsealed value beyond the scope of the current request.
	 *
	 * @param property the property value to unseal, cannot be {@literal null}
	 * @return the unsealed property value, never {@literal null}
	 */
	PropertyValue unseal(PropertyValue property);

	/**
	 * Applies the given {@link PropertyChanges} directly to the target {@link Profile} and persists
	 * the result.
	 * <p>
	 * This operation performs an immediate modification of the target profile without going through
	 * the change request workflow. It is intended for profiles that allow direct changes
	 * (e.g. low-risk environments or administrative overrides).
	 * <p>
	 * Implementations are responsible for:
	 * <ul>
	 *     <li>Resolving the target profile and its current head revision</li>
	 *     <li>Applying the provided property changes to the current state</li>
	 *     <li>Creating a new commit representing the applied changes</li>
	 *     <li>Updating the target branch reference to the new head revision</li>
	 * </ul>
	 * <p>
	 * <b>Concurrency and locking:</b> This operation must be executed under a write lock scoped
	 * to the target profile. The lock must protect reading the current head, applying changes,
	 * and updating the branch reference. Concurrent modifications without proper locking may result
	 * in lost updates or inconsistent repository state.
	 * <p>
	 * <b>Base revision handling:</b> The operation must be performed against the latest head of
	 * the target profile. Implementations must ensure they are not applying changes on top of a
	 * stale revision. If the head changes during execution, the operation must be retried or
	 * fail explicitly.
	 * <p>
	 * <b>Idempotency:</b> This operation is not inherently idempotent, as applying the same changes
	 * multiple times may result in multiple commits. Implementations should ensure that retries
	 * are handled carefully, typically by detecting duplicate operations or relying on higher-level
	 * retry guarantees.
	 * <p>
	 * <b>Trust boundary:</b>
	 * The provided {@link PropertyChanges} must not be assumed to be conflict-free. Implementations
	 * must validate that the changes can be applied cleanly to the current state of the profile.
	 * <p>
	 * Upon successful completion, a new revision is created in the repository and the resulting
	 * state becomes the new source of truth for the profile. A {@link VaultEvent.ChangesApplied}
	 * event should be recorded for audit and history purposes.
	 * </p>
	 *
	 * @param changes the property changes to apply, must not be {@literal null}
	 * @return the result of the property change application, never {@literal null}
	 */
	ApplyResult apply(PropertyChanges changes);

	/**
	 * Submits the given {@link PropertyChanges} for approval by creating a change request.
	 * <p>
	 * This operation prepares the provided changes for review by creating a dedicated branch in
	 * the state repository and registering a corresponding change request in the system. The
	 * proposed changes are not applied to the target profile until the resulting change request
	 * is explicitly merged.
	 * <p>
	 * Implementations are responsible for:
	 * <ul>
	 *     <li>Resolving the target profile and its current head revision</li>
	 *     <li>Creating a new branch for the change request based on the current head</li>
	 *     <li>Applying the provided property changes onto the new branch</li>
	 *     <li>Producing an {@link ApplyResult} describing the resulting state</li>
	 *     <li>Persisting a new {@link ChangeRequest} referencing the created branch</li>
	 * </ul>
	 * <p>
	 * <b>Concurrency and locking:</b> This operation must be executed under a write lock scoped
	 * to the target profile. The lock ensures that the branch is created from a consistent base
	 * revision and prevents race conditions with concurrent apply, merge, or discard operations.
	 * <p>
	 * <b>Base revision capture:</b> The current head revision of the target profile at the time
	 * of submission must be recorded as the base revision of the change request. This value is
	 * critical for later merge validation and must be accurate.
	 * <p>
	 * <b>Branch integrity:</b> The created branch must uniquely identify the change request and
	 * must not collide with existing branches. Implementations should enforce a deterministic
	 * naming strategy and verify that the branch does not already exist.
	 * <p>
	 * <b>Idempotency:</b>
	 * This operation is not inherently idempotent, as repeated submissions will create multiple
	 * branches and change requests. Callers are expected to avoid duplicate submissions or handle
	 * them at a higher level.
	 * <p>
	 * <b>Trust boundary:</b>
	 * The provided {@link PropertyChanges} must be validated and applied against the current
	 * repository state. Implementations must not assume that the changes can be applied without
	 * conflicts or inconsistencies.
	 * <p>
	 * Upon successful completion, a new branch exists in the repository representing the proposed
	 * changes, and a corresponding change request is created in the system. A creation event must
	 * be recorded for audit and history tracking.
	 *
	 * @param changes the property changes to submit, must not be {@code null}
	 * @return the created change request, never {@literal null}
	 */
	ChangeRequest submit(PropertyChanges changes);

	/**
	 * Merges the given {@link ChangeRequest} into its target profile.
	 * <p>
	 * This operation performs a coordinated modification of the underlying repository, applying the
	 * changes represented by the change request branch onto the target profile. The merge is executed
	 * against the current repository state and must ensure that no conflicting or outdated changes are
	 * introduced.
	 * <p>
	 * Implementations are responsible for:
	 * <ul>
	 *     <li>Validating that the change request is in a mergeable state</li>
	 *     <li>Verifying that the base revision is still aligned with the current target revision</li>
	 *     <li>Performing the state repository merge operation</li>
	 *     <li>Updating the repository reference to reflect the new revision</li>
	 *     <li>Cleaning up the change request branch after a successful merge</li>
	 * </ul>
	 * <p>
	 * <b>Concurrency and locking:</b> This operation must be executed under a write lock scoped to
	 * the target profile. The lock must cover the entire critical section, including reading the
	 * current revision, validating the base revision, performing the merge, and updating repository
	 * state. Failure to do so may result in race conditions between concurrent merges or discard operations.
	 * <p>
	 * <b>Base revision validation:</b> The implementation must verify that the change request's base
	 * revision matches the current revision of the target profile before attempting the merge. If the base
	 * revision is outdated, the operation must fail with an appropriate error rather than relying on the
	 * repository to resolve the situation implicitly.
	 * <p>
	 * <b>Conflict detection:</b> If the merge operation results in conflicts, the implementation must abort
	 * the merge and report the conflict. A change request with conflicts must not be merged.
	 * <p>
	 * <b>Idempotency:</b> The operation should be safe to retry. If the change request has already been
	 * merged, the implementation should treat the operation as a no-op or return the already resulting
	 * head revision.
	 * <p>
	 * Upon successful completion, the {@link ChangeRequest} is considered merged and should transition
	 * to a terminal state. A corresponding merge event must be recorded in the change request history.
	 *
	 * @param changeRequest the change request to merge
	 * @return the result of the change request merge operation, never {@literal null}
	 */
	ApplyResult merge(ChangeRequest changeRequest);

	/**
	 * Discards the given {@link ChangeRequest} by removing its associated branch from the repository.
	 * <p>
	 * This operation terminates the lifecycle of a change request without applying its changes to the
	 * target profile. It removes the underlying changeset branch and ensures that no residual state
	 * remains in the repository.
	 * <p>
	 * Implementations are responsible for:
	 * <ul>
	 *     <li>Validating that the change request is still open</li>
	 *     <li>Deleting the corresponding branch from the repository if it exists</li>
	 *     <li>Ensuring that no concurrent merge operation is in progress</li>
	 * </ul>
	 * <p>
	 * <b>Concurrency and locking:</b> This operation must be executed under a write lock scoped to the
	 * target profile. Even though it does not modify the main branch, it mutates repository state and
	 * must be protected against concurrent merge or discard operations affecting the same profile.
	 * <p>
	 * <b>Idempotency:</b> The operation must be safe to retry. If the change request has already been
	 * discarded or the branch no longer exists, the implementation should treat the operation as a
	 * no-op and complete successfully.
	 * <p>
	 * <b>Branch existence:</b> The implementation must verify whether the branch exists before attempting
	 * deletion. Missing branches must not cause the operation to fail, as this may occur in retry or
	 * recovery scenarios.
	 * <p>
	 * <b>State validation:</b> Only change requests in an open state may be discarded. Attempts to discard
	 * already merged or already discarded change requests should be rejected or treated as no-ops
	 * depending on the desired strictness.
	 * <p>
	 * Upon successful completion, the {@link ChangeRequest} transitions to a discarded state and a
	 * corresponding discard event must be recorded.
	 *
	 * @param changeRequest the change request to discard
	 * @return the discarded change request, never {@literal null}
	 * @throws IllegalStateException if the change request is not in an open state
	 */
	ChangeRequest discard(ChangeRequest changeRequest);

	/**
	 * Closes the vault and releases any underlying resources.
	 * <p>
	 * Implementations may close open Git repositories, file handles, or encryption resources.
	 *
	 * @throws Exception if an error occurs while releasing resources
	 */
	@Override
	void close() throws Exception;

}
