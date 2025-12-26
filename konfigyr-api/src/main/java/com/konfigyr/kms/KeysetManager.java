package com.konfigyr.kms;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.Service;
import org.jmolecules.event.annotation.DomainEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.Set;

/**
 * Interface that is responsible for the lifecycle of Data Encryption Keys (DEKs) used
 * by Projects and the Identity Provider.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Service
@NullMarked
public interface KeysetManager {

	/**
	 * Performs a lookup of available {@link KeysetMetadata} that are matching the
	 * filter {@link SearchQuery} filter criteria.
	 *
	 * @param query search query, can't be {@literal null}.
	 * @return matching keyset metadata page, never {@literal null}.
	 */
	Page<KeysetMetadata> find(SearchQuery query);

	/**
	 * Retrieves the current metadata for a specific Keyset by its unique identifier.
	 *
	 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
	 * @return matching metadata or empty, never {@literal null}
	 */
	Optional<KeysetMetadata> get(EntityId id);

	/**
	 * Retrieves the current metadata for a specific Keyset by its unique identifier that belongs
	 * to the specific {@link Namespace}.
	 *
	 * @param namespace The unique identifier of the namespace, can't be {@literal null}.
	 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
	 * @return matching metadata or empty, never {@literal null}
	 */
	Optional<KeysetMetadata> get(EntityId namespace, EntityId id);

	/**
	 * Retrieves the {@link KeysetOperations} for a specific Keyset by its unique identifier.
	 *
	 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
	 * @return the keyset operations, never {@literal null}
	 * @throws KeysetNotFoundException when no such keyset exists.
	 */
	KeysetOperations operations(EntityId id);

	/**
	 * Retrieves the {@link KeysetOperations} for a specific Keyset by its unique identifier that belongs
	 * to the specific {@link Namespace}.
	 *
	 * @param namespace The unique identifier of the namespace, can't be {@literal null}.
	 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
	 * @return the keyset operations, never {@literal null}
	 * @throws KeysetNotFoundException when no such keyset exists for a namespace.
	 */
	KeysetOperations operations(EntityId namespace, EntityId id);

	/**
	 * Provisions a new {@link com.konfigyr.crypto.Keyset} for a specific {@link Namespace}.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Created} event
	 * when a new Keyset was successfully created.
	 *
	 * @param definition The definition of the Keyset to be created, can't be {@literal null}.
	 * @return The newly created Keyset metadata, never {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-created")
	KeysetMetadata create(KeysetMetadataDefinition definition);

	/**
	 * Updates the description or tags for a specific keyset metadata.
	 *
	 * @param id The identifier of the keyset to update, can't be {@literal null}.
	 * @param description The new description for the keyset, can be {@literal null}.
	 * @param tags The new tags for the keyset, can be {@literal null}.
	 * @return The updated keyset metadata, never {@literal null}.
	 */
	KeysetMetadata update(EntityId id, @Nullable String description, @Nullable Set<String> tags);

	/**
	 * Transitions the keyset to the new target state. Implementations of this interface should
	 * publish these events when transition is successful:
	 * <ul>
	 *     <li>{@link KeysetManagementEvent.Activated} - when a disabled keyset is reactivated</li>
	 *     <li>{@link KeysetManagementEvent.Disabled} - when active keyset is disabled</li>
	 *     <li>{@link KeysetManagementEvent.Removed} - when keyset is scheduled for destruction</li>
	 * </ul>
	 *
	 * @param id The identifier of the keyset to transition, can't be {@literal null}.
	 * @param state The target state of the keyset, can't be {@literal null}.
	 * @return The updated keyset metadata, never {@literal null}.
	 */
	KeysetMetadata transition(EntityId id, KeysetMetadataState state);

	/**
	 * Rotates the keyset by adding a new primary key that would be used for cryptographic operations.
	 * <p>
	 * The previous primary key remains available for decryption, or signature verification, but the new key
	 * will be used for all future encryption.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Created} event
	 * when a new Keyset was successfully created.
	 *
	 * @param id The identifier of the keyset to rotate, can't be {@literal null}.
	 * @return The rotated keyset metadata, never {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-rotated")
	KeysetMetadata rotate(EntityId id);

	/**
	 * Destroys the keyset and all associated data. This is a permanent operation and cannot be undone.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Destroyed} event
	 * when a new Keyset was successfully created.
	 *
	 * @param id The identifier of the keyset to delete, can't be {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-destroyed")
	void delete(EntityId id);

}
