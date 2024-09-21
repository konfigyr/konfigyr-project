package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Account accounts}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@Repository
public interface AccountManager {

	/**
	 * Returns am {@link Account} with the given {@link EntityId identifier}. If the {@link Account}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param id account entity identifier, can't be {@literal null}
	 * @return matching account or empty, never {@literal null}
	 */
	@NonNull
	Optional<Account> findById(@NonNull EntityId id);

	/**
	 * Returns am {@link Account} with the given email address. If the {@link Account}
	 * does not exist an empty {@link Optional} would be returned.
	 *
	 * @param email account email address, can't be {@literal null}
	 * @return matching account or empty, never {@literal null}
	 */
	@NonNull
	Optional<Account> findByEmail(@NonNull String email);

	/**
	 * Attempts to register a new {@link Account} using the data present in the
	 * {@link AccountRegistration}.
	 * <p>
	 * The implementations of this interface should publish an {@link AccountEvent.Registered}
	 * when an {@link Account} was successfully registered.
	 *
	 * @param registration account data used for registration, can't be {@literal null}
	 * @return created account, never {@literal null}
	 * @throws AccountExistsException when the account that should be created already exists
	 * @throws AccountException when there is an unexpected exception while creating the account
	 */
	@NonNull
	@DomainEventPublisher(publishes = "accounts.registered")
	Account create(@NonNull AccountRegistration registration);

	/**
	 * Retrieves all {@link Memberships} for an {@link Account} with a given {@link EntityId entity identifier}.
	 *
	 * @param id account entity identifier, can't be {@literal null}
	 * @return account memberships or empty, never {@literal null}
	 * @throws AccountNotFoundException when {@link Account} does not exist
	 */
	@NonNull
	Memberships findMemberships(@NonNull EntityId id);

	/**
	 * Updates the {@link Account} and publishes the {@link AccountEvent.Updated} event when successful.
	 *
	 * @param account account to be updated, can't be {@literal null}
	 * @return updated account, never {@literal null}
	 * @throws AccountNotFoundException when {@link Account} does not exist
	 * @throws AccountException when there is an unexpected exception while updating the account
	 */
	@NonNull
	@DomainEventPublisher(publishes = "accounts.updated")
	Account update(@NonNull Account account);

	/**
	 * Deletes the {@link Account} with a given {@link EntityId entity identifier}.
	 * <p>
	 * The implementations of this interface should publish an {@link AccountEvent.Deleted}
	 * when an {@link Account} was successfully removed from the system.
	 *
	 * @param id account entity identifier, can't be {@literal null}
	 * @throws AccountNotFoundException when {@link Account} does not exist
	 * @throws AccountException when there is an unexpected exception while deleting the account
	 */
	@DomainEventPublisher(publishes = "accounts.deleted")
	void delete(@NonNull EntityId id);

}
