package com.konfigyr.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.Optional;

/**
 * An entity identifier, or {@link EntityId}, defines should a unique identifier for an entity be
 * structured withing the Konfigyr application.
 * <p>
 * The identifier should come in two distinct forms, the first is the natural identifier represented
 * as a {@link Long} type. The natural identifier is usually a database primary key value and should
 * not be used outside the Konfigyr application scope or published through an API.
 * <p>
 * The second form is the external one and can be communicated and shared with other systems, usually
 * through an API. Given the fact that this {@link EntityId} form is shared with others, it is important
 * to be aware that any changes to this form need to be backwards compatible. Otherwise, the system having
 * an older version of the identifier would no longer be able to access the entity.
 * <p>
 * Implementation how an {@link EntityId} is created is defined by the {@link EntityIdProvider} SPI.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see EntityIdProvider
 **/
public interface EntityId extends Comparable<EntityId>, Serializable {

	/**
	 * Generates a new unique {@link EntityId} for an entity. If the {@link EntityIdProvider} provides
	 * this support the method would always return an {@link Optional} with the value, otherwise an
	 * empty {@link Optional} would be returned.
	 *
	 * @return generated entity identifier or empty.
	 */
	@NonNull
	static Optional<EntityId> generate() {
		return EntityIdFactory.getInstance().create();
	}

	/**
	 * Creates a new {@link EntityId} from its internal value.
	 *
	 * @param id internal value of the entity identifier
	 * @return Entity identifier, never {@literal null}
	 * @throws IllegalArgumentException if the external value is invalid
	 */
	@NonNull
	static EntityId from(long id) {
		return from((Long) id);
	}

	/**
	 * Creates a new {@link EntityId} from its internal value.
	 *
	 * @param id internal value of the entity identifier
	 * @return Entity identifier, never {@literal null}
	 * @throws IllegalArgumentException if the external value is invalid
	 */
	@NonNull
	static EntityId from(@NonNull Long id) {
		return EntityIdFactory.getInstance().create(id);
	}

	/**
	 * Creates a new {@link EntityId} from its external value.
	 *
	 * @param hash external value of the entity identifier
	 * @return Entity identifier, never {@literal null}
	 * @throws IllegalArgumentException if the external value is invalid
	 */
	@NonNull
	@JsonCreator
	static EntityId from(@NonNull String hash) {
		return EntityIdFactory.getInstance().create(hash);
	}

	/**
	 * Returns the internal value of the {@link EntityId}.
	 *
	 * @return the internal value
	 */
	long id();

	/**
	 * Returns the serialized external value of the {@link EntityId}.
	 *
	 * @return the serialized external value, never {@literal null}
	 */
	@NonNull
	@JsonValue
	String serialize();

}
