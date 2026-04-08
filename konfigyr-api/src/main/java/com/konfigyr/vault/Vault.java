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
	 * This method is typically used for profiles that allow direct modification without approval.
	 * <p>
	 * Implementations may:
	 * <ul>
	 *     <li>Create a temporary changeset branch</li>
	 *     <li>Apply changes</li>
	 *     <li>Merge into the target profile branch</li>
	 *     <li>Create a commit with appropriate metadata</li>
	 * </ul>
	 *
	 * @param changes the property changes to apply, must not be {@literal null}
	 * @return the result of the property change application, never {@literal null}
	 */
	ApplyResult apply(PropertyChanges changes);

	/**
	 * Submits the given {@link PropertyChanges} for approval.
	 * <p>
	 * This method applies the changes to a temporary changeset branch and creates a corresponding
	 * change request record in the database. The changes are not immediately merged into the
	 * target {@link Profile}.
	 *
	 * @param changes the property changes to submit, must not be {@code null}
	 * @return this vault instance
	 */
	Vault submit(PropertyChanges changes);

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
