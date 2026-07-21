package com.konfigyr.kms;

import com.konfigyr.crypto.*;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.support.SearchQuery;
import com.konfigyr.support.Slug;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

import static com.konfigyr.data.tables.Keysets.KEYSETS;
import static com.konfigyr.data.tables.KeysetKeys.KEYSET_KEYS;
import static com.konfigyr.data.tables.KmsKeysetMetadata.KMS_KEYSET_METADATA;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Slf4j
@NullMarked
@RequiredArgsConstructor
class DefaultKeysetManager implements KeysetManager {

	private static final Marker CREATED = MarkerFactory.getMarker("KMS_KEYSET_CREATED");
	private static final Marker ROTATED = MarkerFactory.getMarker("KMS_KEYSET_ROTATED");
	private static final Marker TRANSITIONED = MarkerFactory.getMarker("KMS_KEYSET_TRANSITION");

	static final Collection<Field<?>> KEYSET_METADATA_FIELDS = List.of(
			KMS_KEYSET_METADATA.ID,
			KEYSET_KEYS.KEY_STATUS,
			KEYSET_KEYS.KEY_ALGORITHM,
			KMS_KEYSET_METADATA.NAME,
			KMS_KEYSET_METADATA.DESCRIPTION,
			KMS_KEYSET_METADATA.TAGS,
			KEYSETS.ROTATION_INTERVAL,
			KEYSETS.DESTRUCTION_GRACE_PERIOD,
			KMS_KEYSET_METADATA.CREATED_AT,
			KMS_KEYSET_METADATA.UPDATED_AT
	);

	static final Collection<Field<?>> KEYS_METADATA_FIELDS = List.of(
			KEYSET_KEYS.KEY_ID,
			KEYSET_KEYS.KEY_STATUS,
			KEYSET_KEYS.KEY_ALGORITHM,
			KEYSET_KEYS.KEY_PRIMARY,
			KEYSET_KEYS.CREATED_AT,
			KEYSET_KEYS.INITIALIZED_AT,
			KEYSET_KEYS.EXPIRES_AT,
			KEYSET_KEYS.DESTRUCTION_SCHEDULED_AT,
			KEYSET_KEYS.DESTROYED_AT
	);

	static final Converter<Long, Duration> durationConverter = Converter.of(
			Long.class,
			Duration.class,
			Duration::ofMillis,
			Duration::toMillis
	);

	@SuppressWarnings("rawtypes")
	static final Converter<String, Set> tagsConverter = Converter.of(
			String.class,
			Set.class,
			StringUtils::commaDelimitedListToSet,
			StringUtils::collectionToCommaDelimitedString
	);

	static final Converter<String, KeysetMetadataAlgorithm> keysetMetadataAlgorithmConverter = Converter.from(
			String.class,
			KeysetMetadataAlgorithm.class,
			KeysetMetadataAlgorithm::fromAlgorithmName
	);

	static final Converter<String, KeysetMetadataState> keysetMetadataStateConverter = Converter.from(
			String.class,
			KeysetMetadataState.class,
			state -> KeysetMetadataState.valueOf(KeyStatus.valueOf(state.toUpperCase()))
	);

	static final PageableExecutor keysetMetadataExecutor = PageableExecutor.builder()
			.defaultSortField(KMS_KEYSET_METADATA.UPDATED_AT.desc())
			.sortField("name", KEYSETS.KEYSET_NAME)
			.sortField("date", KMS_KEYSET_METADATA.UPDATED_AT)
			.build();

	private final DSLContext context;
	private final KeysetStore keysetStore;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true, label = "kms.search")
	public Page<KeysetMetadata> find(Namespace namespace, SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.id().get()));

		query.term().ifPresent(term -> conditions.add(DSL.or(
				KEYSETS.KEYSET_NAME.containsIgnoreCase(term),
				KEYSET_KEYS.KEY_ALGORITHM.containsIgnoreCase(term),
				KMS_KEYSET_METADATA.TAGS.containsIgnoreCase(term),
				KMS_KEYSET_METADATA.DESCRIPTION.containsIgnoreCase(term)
		)));

		query.criteria(KeysetMetadata.ID_CRITERIA).ifPresent(id -> conditions.add(
				KMS_KEYSET_METADATA.ID.eq(id.get())
		));

		query.criteria(KeysetMetadata.ALGORITHM_CRITERIA).ifPresent(algorithm -> conditions.add(
				KEYSET_KEYS.KEY_ALGORITHM.eq(algorithm.get().name())
		));

		query.criteria(KeysetMetadata.STATE_CRITERIA).ifPresent(state -> conditions.add(
				KEYSET_KEYS.KEY_STATUS.in(state.getKeyStatuses())
		));

		if (log.isDebugEnabled()) {
			log.debug("Fetching namespace for conditions: {}", conditions);
		}

		return keysetMetadataExecutor.execute(
				this::createKeysetMetadataQuery,
				() -> DSL.and(conditions),
				DefaultKeysetManager::toKeysetMetadata,
				query.pageable()
		);
	}

	@Override
	@Transactional(readOnly = true, label = "kms.find-by-id")
	public Optional<KeysetMetadata> get(Namespace namespace, EntityId id) {
		return createKeysetMetadataQuery()
				.where(DSL.and(
						KMS_KEYSET_METADATA.ID.eq(id.get()),
						KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.id().get())
				))
				.fetchOptional(DefaultKeysetManager::toKeysetMetadata);
	}

	@Override
	@Transactional(readOnly = true, label = "kms.operations-by-namespace")
	public KeysetOperations operations(Namespace namespace, EntityId id) {
		return operations(DSL.and(
				KMS_KEYSET_METADATA.ID.eq(id.get()),
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.id().get())
		)).orElseThrow(() -> new KeysetNotFoundException(id));
	}

	@Override
	@Transactional(readOnly = true, label = "kms.keys-by-id")
	public List<KeyMetadata> keys(KeysetMetadata keyset) {
		return createKeyMetadataQuery()
				.where(KMS_KEYSET_METADATA.ID.eq(keyset.id().get()))
				.orderBy(KEYSET_KEYS.CREATED_AT.desc())
				.fetch(DefaultKeysetManager::toKeyMetadata);
	}

	@Override
	@Transactional(label = "kms.create")
	@Observed(name = "konfigyr.kms.keyset.create")
	public KeysetMetadata create(Namespace namespace, KeysetMetadataDefinition definition) {
		log.debug("Creating KMS keyset with: {}", definition);

		assertUniqueNamespaceKeysetMetadata(namespace, definition);

		final Keyset keyset = keysetStore.create(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID,
				toKeysetDefinition(namespace, definition));

		log.debug("Attempting to store KMS keyset metadata for: [keyset={}, definition={}]", keyset, definition);

		final Record record = SettableRecord.of(context, KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.ID, EntityId.generate().map(EntityId::get))
				.set(KMS_KEYSET_METADATA.NAMESPACE_ID, namespace.id().get())
				.set(KMS_KEYSET_METADATA.KEYSET_ID, keyset.getName())
				.set(KMS_KEYSET_METADATA.NAME, definition.name())
				.set(KMS_KEYSET_METADATA.DESCRIPTION, definition.description())
				.set(KMS_KEYSET_METADATA.TAGS, definition.tags(), tagsConverter)
				.get();

		final Long id = context.insertInto(KMS_KEYSET_METADATA)
				.set(record)
				.returning(KMS_KEYSET_METADATA.ID)
				.fetchOne(KMS_KEYSET_METADATA.ID);

		Assert.state(id != null, "Failed to insert new keyset metadata record");

		log.info(CREATED, "Successfully created the keyset metadata with: (id={}, namespace={}, name={}, keyset={}, algorithm={})",
				id, namespace.id(), definition.name(), keyset.getName(), definition.algorithm());

		eventPublisher.publishEvent(new KeysetManagementEvent.Created(EntityId.from(id), namespace.id()));

		return get(namespace, EntityId.from(id)).orElseThrow(
				() -> new IllegalStateException("Failed to lookup newly created keyset metadata record with id: " + id)
		);
	}

	@Override
	public KeysetMetadata update(KeysetMetadata keyset, @Nullable String description, @Nullable Set<String> tags) {
		log.debug("Attempting to update keyset metadata with: [keyset={}, description={}, tags={}]",
				keyset.id(), description, tags);

		if (keyset.state() != KeysetMetadataState.ACTIVE) {
			throw new InactiveKeysetException(keyset.id());
		}

		context.update(KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.DESCRIPTION, description)
				.set(KMS_KEYSET_METADATA.TAGS.convert(tagsConverter), tags)
				.set(KMS_KEYSET_METADATA.UPDATED_AT, OffsetDateTime.now())
				.execute();

		return lookup(keyset.id());
	}

	@Override
	@Transactional(label = "kms.transition")
	@Observed(name = "konfigyr.kms.keyset.transition")
	public KeysetMetadata transition(Namespace namespace, KeyOperation operation) {
		log.debug("Attempting to execute the following key operation: {}", operation);

		final KeysetInformation information = lookupKeysetInformation(DSL.and(
				KMS_KEYSET_METADATA.ID.eq(operation.keyset().get()),
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.id().get())
		)).orElseThrow(() -> new KeysetNotFoundException(operation.keyset));

		try {
			switch (operation) {
				case KeyOperation.DeactivateKey deactivate -> keysetStore.disable(information.name(), deactivate.key());
				case KeyOperation.ReactivateKey reactivate -> keysetStore.enable(information.name(), reactivate.key());
				case KeyOperation.CompromiseKey compromise -> keysetStore.compromise(information.name(), compromise.key());
				case KeyOperation.RestoreKey restore -> keysetStore.cancelDestruction(information.name(), restore.key());
				case KeyOperation.DestroyKey destroy -> keysetStore.scheduleDestruction(information.name(), destroy.key());
			}
		} catch (CryptoException.InvalidKeyStatusTransitionException ex) {
			if (ex.getCurrentStatus() == ex.getAttemptedStatus()) {
				return lookup(operation.keyset());
			}

			throw new KeysetTransitionException(operation.keyset(), information.state(), KeysetMetadataState.valueOf(ex.getAttemptedStatus()));
		}

		context.update(KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.UPDATED_AT, OffsetDateTime.now())
				.where(KMS_KEYSET_METADATA.ID.eq(information.id().get()))
				.execute();

		final KeysetManagementEvent event = switch (operation) {
			case KeyOperation.ReactivateKey reactivate -> new KeysetManagementEvent.Reactivated(reactivate, information.namespace());
			case KeyOperation.DeactivateKey deactivate -> new KeysetManagementEvent.Deactivated(deactivate, information.namespace());
			case KeyOperation.CompromiseKey compromise -> new KeysetManagementEvent.Compromised(compromise, information.namespace());
			case KeyOperation.RestoreKey restore -> new KeysetManagementEvent.Restored(restore, information.namespace());
			case KeyOperation.DestroyKey destroy -> new KeysetManagementEvent.Destroyed(destroy, information.namespace());
		};

		eventPublisher.publishEvent(event);

		log.info(TRANSITIONED, "Successfully performed keyset transition operation: {}", operation);

		return lookup(operation.keyset);
	}

	@Override
	@Transactional(label = "kms.rotate")
	@Observed(name = "konfigyr.kms.keyset.rotate")
	public KeysetMetadata rotate(Namespace namespace, EntityId id) {
		return rotate(namespace, id, null);
	}

	@Override
	@Transactional(label = "kms.rotate")
	@Observed(name = "konfigyr.kms.keyset.rotate")
	public KeysetMetadata rotate(Namespace namespace, EntityId id, @Nullable KeysetMetadataAlgorithm algorithm) {
		return lookupKeysetInformation(namespace, id)
				.map(information -> rotate(information, algorithm == null ? information.algorithm() : algorithm))
				.orElseThrow(() -> new KeysetNotFoundException(id));
	}

	private KeysetMetadata rotate(KeysetInformation information, KeysetMetadataAlgorithm algorithm) {
		log.debug("Attempting to rotate keyset with: {}", information);

		keysetStore.rotate(information.name(), KeyDefinition.builder()
				.primary(true)
				.algorithm(algorithm.get())
				.rotationInterval(information.rotationInterval())
				.build());

		context.update(KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.UPDATED_AT, OffsetDateTime.now())
				.execute();

		log.info(ROTATED, "Successfully rotated keyset: {}", information);

		eventPublisher.publishEvent(new KeysetManagementEvent.Rotated(information.id(), information.namespace()));

		return lookup(information.id());
	}

	@Override
	@Transactional(label = "kms.delete")
	@Observed(name = "konfigyr.kms.keyset.delete")
	public void delete(Namespace namespace, EntityId id) {
		final KeysetInformation information = lookupKeysetInformation(namespace, id)
				.orElseThrow(() -> new KeysetNotFoundException(id));

		context.deleteFrom(KMS_KEYSET_METADATA)
				.where(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.execute();

		keysetStore.remove(information.name());

		eventPublisher.publishEvent(new KeysetManagementEvent.Deleted(id, information.namespace()));
	}

	// used internally to retrieve the metadata from the db, usually after a successful update...
	private KeysetMetadata lookup(EntityId id) {
		return createKeysetMetadataQuery()
				.where(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.fetchOptional(DefaultKeysetManager::toKeysetMetadata)
				.orElseThrow(() -> new IllegalStateException("Failed to lookup keyset metadata with id: " + id.get()));
	}

	private Optional<KeysetOperations> operations(Condition condition) {
		return lookupKeysetInformation(condition).map(information -> {
			if (information.isActive()) {
				return KeysetOperations.of(() -> keysetStore.read(information.name()));
			}
			throw new InactiveKeysetException(information.id());
		});
	}

	private void assertUniqueNamespaceKeysetMetadata(Namespace namespace, KeysetMetadataDefinition definition) {
		final Condition condition = DSL.and(
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.id().get()),
				KMS_KEYSET_METADATA.NAME.eq(definition.name())
		);

		if (context.fetchExists(KMS_KEYSET_METADATA, condition)) {
			throw new KeysetExistsException(namespace, definition);
		}
	}

	private SelectWhereStep<Record> createKeysetMetadataQuery() {
		return context.select(KEYSET_METADATA_FIELDS)
				.from(KMS_KEYSET_METADATA)
				.innerJoin(KEYSETS)
				.on(KEYSETS.KEYSET_NAME.eq(KMS_KEYSET_METADATA.KEYSET_ID))
				.innerJoin(KEYSET_KEYS)
				.on(KEYSET_KEYS.KEYSET_NAME.eq(KEYSETS.KEYSET_NAME).and(KEYSET_KEYS.KEY_PRIMARY.isTrue()))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(KMS_KEYSET_METADATA.NAMESPACE_ID));
	}

	private SelectWhereStep<Record> createKeyMetadataQuery() {
		return context.select(KEYS_METADATA_FIELDS)
				.from(KMS_KEYSET_METADATA)
				.innerJoin(KEYSETS)
				.on(KEYSETS.KEYSET_NAME.eq(KMS_KEYSET_METADATA.KEYSET_ID))
				.innerJoin(KEYSET_KEYS)
				.on(KEYSET_KEYS.KEYSET_NAME.eq(KEYSETS.KEYSET_NAME));
	}

	private Optional<KeysetInformation> lookupKeysetInformation(Namespace namespace, EntityId id) {
		return lookupKeysetInformation(DSL.and(
				KMS_KEYSET_METADATA.ID.eq(id.get()),
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.id().get())
		));
	}

	private Optional<KeysetInformation> lookupKeysetInformation(Condition condition) {
		return context.select(
					KMS_KEYSET_METADATA.ID,
					KMS_KEYSET_METADATA.NAMESPACE_ID,
					KEYSETS.KEYSET_NAME,
					KEYSETS.ROTATION_INTERVAL,
					KEYSET_KEYS.KEY_STATUS,
					KEYSET_KEYS.KEY_ALGORITHM
				)
				.from(KMS_KEYSET_METADATA)
				.innerJoin(KEYSETS)
				.on(KEYSETS.KEYSET_NAME.eq(KMS_KEYSET_METADATA.KEYSET_ID))
				.innerJoin(KEYSET_KEYS)
				.on(KEYSET_KEYS.KEYSET_NAME.eq(KEYSETS.KEYSET_NAME).and(KEYSET_KEYS.KEY_PRIMARY.isTrue()))
				.where(condition)
				.fetchOptional(KeysetInformation::from);
	}

	private static String formatKeysetName(Namespace namespace, String name) {
		return  "%019d-%s".formatted(namespace.id().get(), Slug.slugify(name));
	}

	private static KeysetDefinition toKeysetDefinition(Namespace namespace, KeysetMetadataDefinition definition) {
		return KeysetDefinition.builder()
				.name(formatKeysetName(namespace, definition.name()))
				.purpose(definition.algorithm().purpose())
				.algorithm(definition.algorithm().get())
				.rotationInterval(definition.rotationInterval())
				.build();
	}

	@SuppressWarnings("unchecked")
	private static KeysetMetadata toKeysetMetadata(Record record) {
		return KeysetMetadata.builder()
				.id(record.get(KMS_KEYSET_METADATA.ID, EntityId.class))
				.algorithm(record.get(KEYSET_KEYS.KEY_ALGORITHM, keysetMetadataAlgorithmConverter))
				.state(record.get(KEYSET_KEYS.KEY_STATUS, keysetMetadataStateConverter))
				.name(record.get(KMS_KEYSET_METADATA.NAME))
				.description(record.get(KMS_KEYSET_METADATA.DESCRIPTION))
				.tags(record.get(KMS_KEYSET_METADATA.TAGS, tagsConverter))
				.rotationInterval(record.get(KEYSETS.ROTATION_INTERVAL))
				.destructionGracePeriod(record.get(KEYSETS.DESTRUCTION_GRACE_PERIOD))
				.createdAt(record.get(KMS_KEYSET_METADATA.CREATED_AT))
				.updatedAt(record.get(KMS_KEYSET_METADATA.UPDATED_AT))
				.build();
	}

	private static KeyMetadata toKeyMetadata(Record record) {
		return KeyMetadata.builder()
				.id(record.get(KEYSET_KEYS.KEY_ID))
				.algorithm(record.get(KEYSET_KEYS.KEY_ALGORITHM, keysetMetadataAlgorithmConverter))
				.status(record.get(KEYSET_KEYS.KEY_STATUS, KeyStatus.class))
				.isPrimary(record.get(KEYSET_KEYS.KEY_PRIMARY))
				.createdAt(record.get(KEYSET_KEYS.CREATED_AT))
				.initializedAt(record.get(KEYSET_KEYS.INITIALIZED_AT))
				.expiresAt(record.get(KEYSET_KEYS.EXPIRES_AT))
				.destructionScheduledAt(record.get(KEYSET_KEYS.DESTRUCTION_SCHEDULED_AT))
				.destroyedAt(record.get(KEYSET_KEYS.DESTROYED_AT))
				.build();
	}

	/**
	 * Record that contains the basic information about the {@link Keyset} and the {@link KeysetMetadata}
	 * that is managed by the KMS.
	 *
	 * @param id the unique identifier of the {@link KeysetMetadata}
	 * @param namespace the unique identifier of the {@link com.konfigyr.namespace.Namespace}
	 * @param name the name of the {@link Keyset}
	 * @param state the current state of the primary key in the keyset
	 * @param algorithm the algorithm used by the primary key in the keyset
	 * @param rotationInterval the rotation interval in milliseconds for keys in the keyset
	 */
	private record KeysetInformation(
			EntityId id,
			EntityId namespace,
			String name,
			KeysetMetadataState state,
			KeysetMetadataAlgorithm algorithm,
			@Nullable Duration rotationInterval
	) {

		static KeysetInformation from(Record record) {
			return new KeysetInformation(
					record.get(KMS_KEYSET_METADATA.ID, EntityId.class),
					record.get(KMS_KEYSET_METADATA.NAMESPACE_ID, EntityId.class),
					record.get(KEYSETS.KEYSET_NAME),
					record.get(KEYSET_KEYS.KEY_STATUS, keysetMetadataStateConverter),
					record.get(KEYSET_KEYS.KEY_ALGORITHM, keysetMetadataAlgorithmConverter),
					record.get(KEYSETS.ROTATION_INTERVAL, durationConverter)
			);
		}

		boolean isActive() {
			return state == KeysetMetadataState.ACTIVE;
		}

	}
}
