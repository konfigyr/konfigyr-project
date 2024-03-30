package com.konfigyr.entity;

import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Class to be extended by all entity, or domain, events. It is intentionally mad as an abstract
 * type as we do not want to publish generic events directly.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
@EqualsAndHashCode
public abstract class EntityEvent implements Serializable {

	@Serial
	private static final long serialVersionUID = -8261726551620666194L;

	/**
	 * The {@link EntityId} of the entity for which the event occurred.
	 */
	protected final EntityId id;

	/**
	 * The {@link Instant timestamp} when the event occurred.
	 */
	protected final Instant timestamp;

	/**
	 * Create a new {@link EntityEvent} with the {@link EntityId} of the entity for which
	 * the event should be created and published.
	 *
	 * @param id the entity identifier for which the event is associated, never {@literal null}
	 */
	protected EntityEvent(EntityId id) {
		Assert.notNull(id, "Entity identifier for the entity event can not be null");

		this.id = id;
		this.timestamp = Instant.now();
	}

	/**
	 * Returns the {@link EntityId} of the entity for which the event occurred.
	 *
	 * @return entity identifier, never {@literal null}
	 */
	@NonNull
	public EntityId id() {
		return id;
	}

	/**
	 * Returns the {@link Instant} when the event occurred.
	 *
	 * @return event timestamp, never {@literal null}
	 */
	@NonNull
	public Instant timestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + id + ", timestamp=" + timestamp + ']';
	}

}
