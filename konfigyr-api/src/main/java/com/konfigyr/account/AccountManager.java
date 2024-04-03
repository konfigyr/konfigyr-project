package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Interface that defines a contract to be used when dealing with {@link Account accounts}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
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
	 * The implementations of this interface should publish an {@link AccountRegisteredEvent}
	 * when an {@link Account} was successfully registered.
	 *
	 * @param registration account data used for registration, can't be {@literal null}
	 * @return created account, never {@literal null}
	 * @throws AccountExistsException when the account that should be created already exists
	 * @throws AccountException when there is an unexpected exception while creating the account
	 */
	@NonNull
	Account create(@NonNull AccountRegistration registration);

}
