package com.konfigyr.kms;

import com.konfigyr.crypto.*;
import com.konfigyr.data.PageableExecutor;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.NamespaceNotFoundException;
import com.konfigyr.support.SearchQuery;
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

import java.time.OffsetDateTime;
import java.util.*;

import static com.konfigyr.data.tables.Keysets.KEYSETS;
import static com.konfigyr.data.tables.KmsKeysetMetadata.KMS_KEYSET_METADATA;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Slf4j
@NullMarked
@RequiredArgsConstructor
public class DefaultKeysetManager implements KeysetManager {

	private static final Marker CREATED = MarkerFactory.getMarker("KMS_KEYSET_CREATED");
	private static final Marker ROTATED = MarkerFactory.getMarker("KMS_KEYSET_ROTATED");
	private static final Marker TRANSITIONED = MarkerFactory.getMarker("KMS_KEYSET_TRANSITION");

	static final Collection<Field<?>> KEYSET_METADATA_FIELDS = List.of(
			KMS_KEYSET_METADATA.ID,
			KEYSETS.KEYSET_ALGORITHM,
			KMS_KEYSET_METADATA.STATE,
			KMS_KEYSET_METADATA.NAME,
			KMS_KEYSET_METADATA.DESCRIPTION,
			KMS_KEYSET_METADATA.TAGS,
			KMS_KEYSET_METADATA.CREATED_AT,
			KMS_KEYSET_METADATA.UPDATED_AT,
			KMS_KEYSET_METADATA.DESTROYED_AT
	);

	@SuppressWarnings("rawtypes")
	static final Converter<String, Set> tagsConverter = Converter.of(
			String.class,
			Set.class,
			StringUtils::commaDelimitedListToSet,
			StringUtils::collectionToCommaDelimitedString
	);

	static final PageableExecutor keysetMetadataExecutor = PageableExecutor.builder()
			.defaultSortField(KMS_KEYSET_METADATA.UPDATED_AT.desc())
			.sortField("name", KEYSETS.KEYSET_NAME)
			.sortField("state", KMS_KEYSET_METADATA.STATE)
			.sortField("date", KMS_KEYSET_METADATA.UPDATED_AT)
			.build();

	private final DSLContext context;
	private final KeysetStore keysetStore;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true, label = "kms.search")
	public Page<KeysetMetadata> find(SearchQuery query) {
		final List<Condition> conditions = new ArrayList<>();

		query.term().map(term -> "%" + term + "%").ifPresent(term -> conditions.add(DSL.or(
				KEYSETS.KEYSET_NAME.likeIgnoreCase(term),
				KEYSETS.KEYSET_ALGORITHM.likeIgnoreCase(term),
				KMS_KEYSET_METADATA.TAGS.likeIgnoreCase(term),
				KMS_KEYSET_METADATA.DESCRIPTION.likeIgnoreCase(term)
		)));

		query.criteria(SearchQuery.NAMESPACE).ifPresent(namespace -> conditions.add(
				NAMESPACES.SLUG.eq(namespace)
		));

		query.criteria(KeysetMetadata.ID_CRITERIA).ifPresent(id -> conditions.add(
				KMS_KEYSET_METADATA.ID.eq(id.get())
		));

		query.criteria(KeysetMetadata.ALGORITHM_CRITERIA).ifPresent(algorithm -> conditions.add(
				KEYSETS.KEYSET_ALGORITHM.eq(algorithm)
		));

		query.criteria(KeysetMetadata.STATE_CRITERIA).ifPresent(state -> conditions.add(
				KMS_KEYSET_METADATA.STATE.eq(state.name())
		));

		if (log.isDebugEnabled()) {
			log.debug("Fetching namespace for conditions: {}", conditions);
		}

		return keysetMetadataExecutor.execute(
				createKeysetMetadataQuery(DSL.and(conditions)),
				DefaultKeysetManager::toKeysetMetadata,
				query.pageable(),
				() -> context.fetchCount(createKeysetMetadataQuery(DSL.and(conditions)))
		);
	}

	@Override
	@Transactional(readOnly = true, label = "kms.find-by-id")
	public Optional<KeysetMetadata> get(EntityId id) {
		return createKeysetMetadataQuery(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.fetchOptional(DefaultKeysetManager::toKeysetMetadata);
	}

	@Override
	@Transactional(readOnly = true, label = "kms.find-by-namespace")
	public Optional<KeysetMetadata> get(EntityId namespace, EntityId id) {
		return createKeysetMetadataQuery(DSL.and(
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.get()),
				KMS_KEYSET_METADATA.ID.eq(id.get())
		)).fetchOptional(DefaultKeysetManager::toKeysetMetadata);
	}

	@Override
	@Transactional(readOnly = true, label = "kms.operations-by-id")
	public KeysetOperations operations(EntityId id) {
		return operations(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.orElseThrow(() -> new KeysetNotFoundException(id));
	}

	@Override
	@Transactional(readOnly = true, label = "kms.operations-by-namespace")
	public KeysetOperations operations(EntityId namespace, EntityId id) {
		return operations(DSL.and(
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(namespace.get()),
				KMS_KEYSET_METADATA.ID.eq(id.get())
		)).orElseThrow(() -> new KeysetNotFoundException(id));
	}

	@Override
	@Transactional(label = "kms.create")
	public KeysetMetadata create(KeysetMetadataDefinition definition) {
		log.debug("Creating KMS keyset with: {}", definition);

		assertUniqueNamespaceKeysetMetadata(definition);

		final Keyset keyset = keysetStore.create(CryptoProperties.PROVIDER_NAME, CryptoProperties.KEK_ID,
				definition.toKeysetDefinition());

		log.debug("Attempting to store KMS keyset metadata for: [keyset={}, definition={}]", keyset, definition);

		final Record record = SettableRecord.of(context, KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.ID, EntityId.generate().map(EntityId::get))
				.set(KMS_KEYSET_METADATA.NAMESPACE_ID, definition.namespace().get())
				.set(KMS_KEYSET_METADATA.KEYSET_ID, keyset.getName())
				.set(KMS_KEYSET_METADATA.STATE, KeysetMetadataState.ACTIVE.name())
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
				id, definition.namespace(), definition.name(), keyset.getName(), definition.algorithm());

		eventPublisher.publishEvent(new KeysetManagementEvent.Created(
				EntityId.from(id), definition.namespace()
		));

		return get(EntityId.from(id)).orElseThrow(
				() -> new IllegalStateException("Failed to lookup newly created keyset metadata record with id: " + id)
		);
	}

	@Override
	public KeysetMetadata update(EntityId id, @Nullable String description, @Nullable Set<String> tags) {
		final KeysetInformation information = lookupKeysetInformation(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.orElseThrow(() -> new KeysetNotFoundException(id));

		log.debug("Attempting to update keyset metadata with: [keyset={}, description={}, tags={}]",
				id, description, tags);

		if (!information.isActive()) {
			throw new InactiveKeysetException(id);
		}

		context.update(KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.DESCRIPTION, description)
				.set(KMS_KEYSET_METADATA.TAGS.convert(tagsConverter), tags)
				.set(KMS_KEYSET_METADATA.UPDATED_AT, OffsetDateTime.now())
				.execute();

		return lookup(id);
	}

	@Override
	@Transactional(label = "kms.transition")
	public KeysetMetadata transition(EntityId id, KeysetMetadataState state) {
		if (KeysetMetadataState.DESTROYED == state) {
			throw new IllegalArgumentException("Can not transition keyset metadata to destroyed state");
		}

		log.debug("Attempting to transition keyset metadata with id: {} to state: {}", id, state);

		final KeysetInformation information = lookupKeysetInformation(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.orElseThrow(() -> new KeysetNotFoundException(id));

		log.debug("Found keyset metadata that should be transitioned with: [id={}, state={}, target={}]",
				id, information.state(), state);

		if (information.isDestroyed()) {
			throw new KeysetTransitionException(id, information.state(), state);
		}

		if (information.state() == state) {
			return lookup(id);
		}

		if (!information.state().canTransitionTo(state)) {
			throw new KeysetTransitionException(id, information.state(), state);
		}

		context.update(KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.STATE, state.name())
				.set(KMS_KEYSET_METADATA.UPDATED_AT, OffsetDateTime.now())
				.where(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.execute();

		log.info(TRANSITIONED, "Successfully transitioned keyset metadata with id: {} to state: {}", id, state);

		switch (state) {
			case ACTIVE -> eventPublisher.publishEvent(
					new KeysetManagementEvent.Activated(id, information.namespace())
			);
			case INACTIVE -> eventPublisher.publishEvent(
					new KeysetManagementEvent.Disabled(id, information.namespace())
			);
			case PENDING_DESTRUCTION -> eventPublisher.publishEvent(
					new KeysetManagementEvent.Removed(id, information.namespace())
			);
			default -> log.warn("Unexpected state transition detected: [keyset={}, target={}]", information, state);
		}

		return lookup(id);
	}

	@Override
	@Transactional(label = "kms.rotate")
	public KeysetMetadata rotate(EntityId id) {
		final KeysetInformation information = lookupKeysetInformation(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.orElseThrow(() -> new KeysetNotFoundException(id));

		log.debug("Attempting to rotate keyset metadata: {}", information);

		if (!information.isActive()) {
			throw new InactiveKeysetException(id);
		}

		try {
			final Keyset keyset = keysetStore.read(information.name());
			keysetStore.rotate(keyset);
		} catch (CryptoException ex) {
			throw new KeysetManagementException("Keyset store failed to rotate keyset with id: " + id, ex);
		} catch (Exception ex) {
			throw new KeysetManagementException("Unexpected error occurred while rotating keyset with id: " + id, ex);
		}

		context.update(KMS_KEYSET_METADATA)
				.set(KMS_KEYSET_METADATA.UPDATED_AT, OffsetDateTime.now())
				.execute();

		log.info(ROTATED, "Successfully rotated keyset: {}", information);

		eventPublisher.publishEvent(new KeysetManagementEvent.Rotated(id, information.namespace()));

		return lookup(id);
	}

	@Override
	@Transactional(label = "kms.delete")
	public void delete(EntityId id) {
		final KeysetInformation information = lookupKeysetInformation(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.orElseThrow(() -> new KeysetNotFoundException(id));

		context.deleteFrom(KMS_KEYSET_METADATA)
				.where(KMS_KEYSET_METADATA.ID.eq(id.get()))
				.execute();

		keysetStore.remove(information.name());

		eventPublisher.publishEvent(new KeysetManagementEvent.Destroyed(id, information.namespace()));
	}

	// used internally to retrieve the metadata from the db, usually after a successful update...
	private KeysetMetadata lookup(EntityId id) {
		return createKeysetMetadataQuery(KMS_KEYSET_METADATA.ID.eq(id.get()))
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

	private void assertUniqueNamespaceKeysetMetadata(KeysetMetadataDefinition definition) {
		// check if the namespace exists before we attempt to validate unique KMS keyset metadata
		if (!context.fetchExists(NAMESPACES, NAMESPACES.ID.eq(definition.namespace().get()))) {
			throw new NamespaceNotFoundException(definition.namespace());
		}

		final Condition condition = DSL.and(
				KMS_KEYSET_METADATA.NAMESPACE_ID.eq(definition.namespace().get()),
				KMS_KEYSET_METADATA.NAME.eq(definition.name())
		);

		if (context.fetchExists(KMS_KEYSET_METADATA, condition)) {
			throw new KeysetExistsException(definition);
		}
	}

	private SelectConditionStep<Record> createKeysetMetadataQuery(Condition condition) {
		return context.select(KEYSET_METADATA_FIELDS)
				.from(KMS_KEYSET_METADATA)
				.innerJoin(KEYSETS)
				.on(KEYSETS.KEYSET_NAME.eq(KMS_KEYSET_METADATA.KEYSET_ID))
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(KMS_KEYSET_METADATA.NAMESPACE_ID))
				.where(condition);
	}

	private Optional<KeysetInformation> lookupKeysetInformation(Condition condition) {
		return context.select(
					KMS_KEYSET_METADATA.ID,
					KMS_KEYSET_METADATA.NAMESPACE_ID,
					KMS_KEYSET_METADATA.STATE,
					KEYSETS.KEYSET_NAME
				)
				.from(KMS_KEYSET_METADATA)
				.innerJoin(KEYSETS)
				.on(KEYSETS.KEYSET_NAME.eq(KMS_KEYSET_METADATA.KEYSET_ID))
				.where(condition)
				.fetchOptional(KeysetInformation::from);
	}

	@SuppressWarnings("unchecked")
	private static KeysetMetadata toKeysetMetadata(Record record) {
		return KeysetMetadata.builder()
				.id(record.get(KMS_KEYSET_METADATA.ID, EntityId.class))
				.algorithm(record.get(KEYSETS.KEYSET_ALGORITHM))
				.state(record.get(KMS_KEYSET_METADATA.STATE, KeysetMetadataState.class))
				.name(record.get(KMS_KEYSET_METADATA.NAME))
				.description(record.get(KMS_KEYSET_METADATA.DESCRIPTION))
				.tags(record.get(KMS_KEYSET_METADATA.TAGS, tagsConverter))
				.createdAt(record.get(KMS_KEYSET_METADATA.CREATED_AT))
				.updatedAt(record.get(KMS_KEYSET_METADATA.UPDATED_AT))
				.destroyedAt(record.get(KMS_KEYSET_METADATA.DESTROYED_AT))
				.build();
	}

	/**
	 * Record that contains the basic information about the {@link Keyset} and the {@link KeysetMetadata}
	 * that is managed by the KMS.
	 *
	 * @param id the unique identifier of the {@link KeysetMetadata}
	 * @param namespace the unique identifier of the {@link com.konfigyr.namespace.Namespace}
	 * @param name the name of the {@link Keyset}
	 * @param state the current state of the {@link KeysetMetadata}
	 */
	private record KeysetInformation(EntityId id, EntityId namespace, String name, KeysetMetadataState state) {

		static KeysetInformation from(Record record) {
			return new KeysetInformation(
					record.get(KMS_KEYSET_METADATA.ID, EntityId.class),
					record.get(KMS_KEYSET_METADATA.NAMESPACE_ID, EntityId.class),
					record.get(KEYSETS.KEYSET_NAME),
					record.get(KMS_KEYSET_METADATA.STATE, KeysetMetadataState.class)
			);
		}

		boolean isActive() {
			return state == KeysetMetadataState.ACTIVE;
		}

		boolean isDestroyed() {
			return state == KeysetMetadataState.DESTROYED;
		}

	}
}
