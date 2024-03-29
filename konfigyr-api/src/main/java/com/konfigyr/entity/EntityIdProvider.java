package com.konfigyr.entity;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Service provider interface that provides concrete implementations of the {@link EntityId}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see EntityId
 **/
public interface EntityIdProvider extends Ordered {

	/**
	 * Method that generates a new unique {@link EntityId} value.
	 * <p>
	 * In case the concrete implementation of the {@link EntityId} can not be generated,
	 * this method should return {@literal null}.
	 *
	 * @return generated entity identifier or {@literal null} if one can not be generated
	 * by this service provider implementation.
	 */
	@Nullable
	default EntityId generate() {
		return null;
	}

	/**
	 * Crates a {@link EntityId} from its natural long value that is used internally
	 * within the Konfigyr application.
	 *
	 * @param id natural identifier
	 * @return entity identifier, never {@literal null}
	 * @throws IllegalArgumentException if the identifier value is invalid
	 */
	@NonNull
	EntityId create(long id);

	/**
	 * Crates a {@link EntityId} from its serialized value that should be used externally
	 * outside the Konfigyr application scope.
	 *
	 * @param hash serialized identifier value, can't be {@literal null}
	 * @return entity identifier, never {@literal null}
	 * @throws IllegalArgumentException if the serialized identifier value is blank or invalid
	 */
	@NonNull
	EntityId create(@NonNull String hash);

}
