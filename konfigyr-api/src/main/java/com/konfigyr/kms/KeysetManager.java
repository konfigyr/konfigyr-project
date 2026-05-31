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

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The primary entry point for the Key Management Service (KMS) Konfigyr domain. This service provides
 * centralized lifecycle management, storage, and cryptographic execution for {@code Keysets}.
 * <p>
 * This domain builds upon the Konfigyr Crypto library and extends the {@link com.konfigyr.crypto.Keyset Keysets}
 * concept by providing additional metadata, security, and governance features. For more information see
 * the <a href="https://github.com/konfigyr/konfigyr-crypto/">Konfigyr Crypto documentation</a>.
 * <p>
 * The Konfigyr KMS is designed for multi-tenant environments where security, auditability
 * and logical isolation per {@link Namespace} are mandatory. Every operation is tied to a {@code Namespace}
 * and automatically generates an audit log event for compliance (SOC2/HIPAA).
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Service
@NullMarked
public interface KeysetManager {

	/**
	 * Performs a lookup of available {@link KeysetMetadata} within the given {@link Namespace}
	 * that are matching the filter {@link SearchQuery} filter criteria.
	 *
	 * @param namespace The namespace that owns the keysets, can't be {@literal null}.
	 * @param query search query, can't be {@literal null}.
	 * @return matching keyset metadata page, never {@literal null}.
	 */
	Page<KeysetMetadata> find(Namespace namespace, SearchQuery query);

	/**
	 * Retrieves the current metadata for a specific Keyset by its unique identifier
	 * that belongs to the specific {@link Namespace}.
	 *
	 * @param namespace The namespace that owns the keyset, can't be {@literal null}.
	 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
	 * @return matching metadata or empty, never {@literal null}
	 */
	Optional<KeysetMetadata> get(Namespace namespace, EntityId id);

	/**
	 * Retrieves the {@link KeysetOperations} for a specific Keyset by its unique identifier
	 * that belongs to the specific {@link Namespace}.
	 *
	 * @param namespace The namespace that owns the keyset, can't be {@literal null}.
	 * @param id The unique identifier of the keyset metadata, can't be {@literal null}.
	 * @return the keyset operations, never {@literal null}
	 * @throws KeysetNotFoundException when no such keyset exists for a namespace.
	 */
	KeysetOperations operations(Namespace namespace, EntityId id);

	/**
	 * Retrieves the {@link KeyMetadata keys} for a specific Keyset.
	 *
	 * @param keyset The keyset metadata for which to retrieve the keys, can't be {@literal null}.
	 * @return the key metadata contained within the keyset, never {@literal null}
	 * @throws KeysetNotFoundException when no such keyset exists.
	 */
	List<KeyMetadata> keys(KeysetMetadata keyset);

	/**
	 * Provisions a new {@link com.konfigyr.crypto.Keyset} for a specific {@link Namespace}.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Created} event
	 * when a new Keyset was successfully created.
	 *
	 * @param namespace The namespace that would own the keyset, can't be {@literal null}.
	 * @param definition The definition of the Keyset to be created, can't be {@literal null}.
	 * @return The newly created Keyset metadata, never {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-created")
	KeysetMetadata create(Namespace namespace, KeysetMetadataDefinition definition);

	/**
	 * Updates the description or tags for a specific keyset metadata.
	 *
	 * @param keyset The keyset to update, can't be {@literal null}.
	 * @param description The new description for the keyset, can be {@literal null}.
	 * @param tags The new tags for the keyset, can be {@literal null}.
	 * @return The updated keyset metadata, never {@literal null}.
	 */
	KeysetMetadata update(KeysetMetadata keyset, @Nullable String description, @Nullable Set<String> tags);

	/**
	 * Transitions the keyset to the new target state. Implementations of this interface should
	 * publish these events when transition is successful:
	 * <ul>
	 *     <li>{@link KeysetManagementEvent.Reactivated} - when a disabled key is reactivated</li>
	 *     <li>{@link KeysetManagementEvent.Deactivated} - when a key is deactivated</li>
	 *     <li>{@link KeysetManagementEvent.Compromised} - when a key marked as compromised</li>
	 *     <li>{@link KeysetManagementEvent.Restored} - when scheduled key destruction was canceled</li>
	 *     <li>{@link KeysetManagementEvent.Destroyed} - when a key is scheduled for destruction</li>
	 * </ul>
	 *
	 * @param namespace The namespace that would own the key and keyset, can't be {@literal null}.
	 * @param operation The key transition operation to be performed, can't be {@literal null}
	 * @return The updated keyset metadata, never {@literal null}.
	 */
	KeysetMetadata transition(Namespace namespace, KeyOperation operation);

	/**
	 * Rotates the keyset by adding a new primary key that would be used for cryptographic operations.
	 * <p>
	 * The previous primary key remains available for decryption, or signature verification, but the new key
	 * will be used for all future encryption.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Created} event
	 * when a new Keyset was successfully created.
	 *
	 * @param namespace The namespace that owns the keyset, can't be {@literal null}.
	 * @param id The identifier of the keyset to rotate, can't be {@literal null}.
	 * @return The rotated keyset metadata, never {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-rotated")
	KeysetMetadata rotate(Namespace namespace, EntityId id);

	/**
	 * Rotates the keyset by adding a new primary key with a specified {@link KeysetMetadataAlgorithm}
	 * that would be used for cryptographic operations.
	 * <p>
	 * The previous primary key remains available for decryption, or signature verification, but the new key
	 * will be used for all future encryption.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Created} event
	 * when a new Keyset was successfully created.
	 *
	 * @param namespace The namespace that owns the keyset, can't be {@literal null}.
	 * @param id The identifier of the keyset to rotate, can't be {@literal null}.
	 * @param algorithm the algorithm to use for the new key, can be {@literal null}.
	 * @return The rotated keyset metadata, never {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-rotated")
	KeysetMetadata rotate(Namespace namespace, EntityId id, @Nullable KeysetMetadataAlgorithm algorithm);

	/**
	 * Destroys the keyset and all associated data. This is a permanent operation and cannot be undone.
	 * <p>
	 * The implementations of this interface should publish an {@link KeysetManagementEvent.Deleted} event
	 * when a new Keyset was successfully created.
	 *
	 * @param namespace the namespace that owns the keyset, can't be {@literal null}.
	 * @param id The identifier of the keyset to delete, can't be {@literal null}.
	 */
	@DomainEventPublisher(publishes = "kms.keyset-destroyed")
	void delete(Namespace namespace, EntityId id);

}
