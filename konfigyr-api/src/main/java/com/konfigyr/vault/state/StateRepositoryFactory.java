package com.konfigyr.vault.state;

import com.konfigyr.namespace.Service;
import org.jmolecules.ddd.annotation.Factory;
import org.jspecify.annotations.NullMarked;

/**
 * Factory for creating and retrieving {@link StateRepository} instances associated with
 * the given {@link Service}.
 * <p>
 * The {@link StateRepositoryFactory} acts as the entry point for accessing the persistent
 * state backing a service. It abstracts the underlying storage and versioning mechanism,
 * allowing callers to obtain a repository handle without being coupled to a specific
 * implementation.
 * <p>
 * Implementations may choose to back the {@link StateRepository} using a variety of mechanisms,
 * including but not limited to version-controlled storage systems, file-based structures, or
 * other forms of state persistence. The factory is responsible for ensuring that the appropriate
 * repository instance is provided for the given service context.
 * <p>
 * The lifecycle of repositories is managed through this factory. The {@link #create(Service)} method
 * is intended for provisioning a new repository for a service, while {@link #get(Service)} provides
 * access to an already initialized repository.
 * <p>
 * Implementations are expected to handle concerns such as repository initialization, caching, and
 * reuse of repository instances. They must also ensure that repeated calls to {@link #get(Service)}
 * return a consistent and usable repository handle.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see StateRepository
 */
@Factory
@NullMarked
public interface StateRepositoryFactory {

	/**
	 * Creates and initializes a new {@link StateRepository} for the given service.
	 * <p>
	 * This method is typically invoked when a {@link Service} is first provisioned or when its
	 * backing state repository does not yet exist. Implementations are expected to perform any
	 * necessary setup to make the repository ready for use.
	 * <p>
	 * If a repository for the given service already exists, implementations may either return
	 * the existing instance or fail, depending on the desired semantics.
	 *
	 * @param service the service for which the repository should be created, can't be {@literal null}
	 * @return a newly created and initialized state repository, never {@link null}
	 * @throws RepositoryStateException if repository can not be created for the given service
	 */
	StateRepository create(Service service);

	/**
	 * Retrieves an existing {@link StateRepository} for the given service.
	 * <p>
	 * This method provides access to a previously created repository and is expected to return
	 * a fully initialized and ready-to-use instance.
	 * <p>
	 * Implementations may cache repository instances or resolve them dynamically, but must ensure
	 * that the returned repository reflects the current state associated with the service.
	 *
	 * @param service the service whose repository should be retrieved, can't be {@literal null}
	 * @return the existing state repository for the service, never {@link null}
	 * @throws RepositoryStateException if no repository exists for the given service
	 */
	StateRepository get(Service service);

}
