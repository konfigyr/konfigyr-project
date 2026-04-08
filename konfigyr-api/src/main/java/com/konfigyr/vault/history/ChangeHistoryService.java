package com.konfigyr.vault.history;

import com.konfigyr.data.CursorPage;
import com.konfigyr.data.CursorPageable;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.vault.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.konfigyr.data.tables.Services.SERVICES;
import static com.konfigyr.data.tables.VaultChangeHistory.VAULT_CHANGE_HISTORY;
import static com.konfigyr.data.tables.VaultProfiles.VAULT_PROFILES;
import static com.konfigyr.data.tables.VaultPropertyHistory.VAULT_PROPERTY_HISTORY;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeHistoryService implements VaultChronicle {

	static final List<Field<?>> CHANGE_HISTORY_FIELDS = List.of(
			VAULT_CHANGE_HISTORY.ID,
			VAULT_CHANGE_HISTORY.REVISION,
			VAULT_CHANGE_HISTORY.SUBJECT,
			VAULT_CHANGE_HISTORY.DESCRIPTION,
			VAULT_CHANGE_HISTORY.CHANGE_COUNT,
			VAULT_CHANGE_HISTORY.AUTHOR_NAME,
			VAULT_CHANGE_HISTORY.CREATED_AT
	);

	static final List<Field<?>> PROPERTY_HISTORY_FIELDS = List.of(
			VAULT_PROPERTY_HISTORY.CHANGE_ID,
			VAULT_CHANGE_HISTORY.REVISION,
			VAULT_PROPERTY_HISTORY.PROPERTY_NAME,
			VAULT_PROPERTY_HISTORY.CHANGE_OPERATION,
			VAULT_PROPERTY_HISTORY.OLD_VALUE_CHECKSUM,
			VAULT_PROPERTY_HISTORY.OLD_VALUE_CIPHER,
			VAULT_PROPERTY_HISTORY.NEW_VALUE_CHECKSUM,
			VAULT_PROPERTY_HISTORY.NEW_VALUE_CIPHER,
			VAULT_CHANGE_HISTORY.AUTHOR_NAME,
			VAULT_PROPERTY_HISTORY.CREATED_AT
	);

	private final DSLContext context;

	/**
	 * Records a new revision in the chronicle for the given {@link Profile}.
	 * <p>
	 * The supplied {@link ApplyResult} represent the complete set of changes applied in a
	 * single logical operation. All values must be provided in a sealed (encrypted) form.
	 * <p>
	 * Implementations must guarantee:
	 * <ul>
	 *     <li>Atomic persistence of the revision and all transitions</li>
	 *     <li>Generation of a unique revision identifier</li>
	 *     <li>Consistent timestamping across all transitions</li>
	 * </ul>
	 *
	 * @param profile the entity identifier of the profile to which the changes are applied, must not be {@code null}
	 * @param result the changes applied to the configuration state, must not be {@code null}
	 */
	@Transactional(label = "vault.commit-change-history")
	void commit(EntityId profile, ApplyResult result) {
		final ChangeHistoryOwnership ownership = lookupOwnership(profile);
		final AuthenticatedPrincipal author = result.author();

		final Long id = context.insertInto(VAULT_CHANGE_HISTORY)
				.set(
						SettableRecord.of(context, VAULT_CHANGE_HISTORY)
								.set(VAULT_CHANGE_HISTORY.ID, EntityId.generate().map(EntityId::get))
								.set(VAULT_CHANGE_HISTORY.NAMESPACE_ID, ownership.namespace())
								.set(VAULT_CHANGE_HISTORY.SERVICE_ID, ownership.service())
								.set(VAULT_CHANGE_HISTORY.PROFILE_ID, ownership.profile())
								.set(VAULT_CHANGE_HISTORY.SUBJECT, result.subject())
								.set(VAULT_CHANGE_HISTORY.DESCRIPTION, result.description())
								.set(VAULT_CHANGE_HISTORY.CHANGE_COUNT, result.changes().size())
								.set(VAULT_CHANGE_HISTORY.AUTHOR_ID, author.get())
								.set(VAULT_CHANGE_HISTORY.AUTHOR_TYPE, author.getType().name())
								.set(VAULT_CHANGE_HISTORY.AUTHOR_NAME, author.getDisplayName())
								.set(VAULT_CHANGE_HISTORY.REVISION, result.revision())
								.set(VAULT_CHANGE_HISTORY.PREVIOUS_REVISION, result.previousRevision())
								.set(VAULT_CHANGE_HISTORY.CREATED_AT, result.timestamp())
								.get()
				)
				.returning(VAULT_CHANGE_HISTORY.ID)
				.fetchOne(VAULT_CHANGE_HISTORY.ID);


		InsertValuesStepN<Record> insert = context.insertInto(VAULT_PROPERTY_HISTORY)
				.columns(List.of(
						VAULT_PROPERTY_HISTORY.CHANGE_ID,
						VAULT_PROPERTY_HISTORY.PROFILE_ID,
						VAULT_PROPERTY_HISTORY.PROPERTY_NAME,
						VAULT_PROPERTY_HISTORY.CHANGE_OPERATION,
						VAULT_PROPERTY_HISTORY.NEW_VALUE_CHECKSUM,
						VAULT_PROPERTY_HISTORY.NEW_VALUE_CIPHER,
						VAULT_PROPERTY_HISTORY.OLD_VALUE_CHECKSUM,
						VAULT_PROPERTY_HISTORY.OLD_VALUE_CIPHER,
						VAULT_PROPERTY_HISTORY.CREATED_AT
				));

		for (PropertyTransition transition : result) {
			final PropertyValue from = transition.from();
			final PropertyValue to = transition.to();

			insert = insert.values(
					id,
					ownership.profile(),
					transition.name(),
					transition.type().name(),
					to != null ? to.checksum() : null,
					to != null ? to.get() : null,
					from != null ? from.checksum() : null,
					from != null ? from.get() : null,
					result.timestamp()
			);
		}

		insert.execute();
	}

	@Override
	@Transactional(label = "vault.retrieve-change-history", readOnly = true)
	public CursorPage<ChangeHistory> fetchHistory(Profile profile, CursorPageable pageable) {
		// it is an unpaged request, return all changes for profile...
		if (pageable.isUnpaged()) {
			return CursorPage.of(
					createChangeHistoryQuery(VAULT_CHANGE_HISTORY.PROFILE_ID.eq(profile.id().get()))
							.orderBy(VAULT_CHANGE_HISTORY.CREATED_AT.desc())
							.fetch(ChangeHistoryService::toChangeHistory)
			);
		}

		final HistoryCursorToken token = decodeCursor(pageable);
		final boolean isReversed = token != null && token.reversed();
		final SortOrder sortOrder = isReversed ? SortOrder.ASC : SortOrder.DESC;

		final List<Condition> conditions = new ArrayList<>();
		conditions.add(VAULT_CHANGE_HISTORY.PROFILE_ID.eq(profile.id().get()));

		if (token != null) {
			if (sortOrder == SortOrder.DESC) {
				conditions.add(VAULT_CHANGE_HISTORY.ID.lessThan(token.identifier()));
				conditions.add(VAULT_CHANGE_HISTORY.CREATED_AT.lessThan(token.timestamp()));
			} else {
				conditions.add(VAULT_CHANGE_HISTORY.ID.greaterThan(token.identifier()));
				conditions.add(VAULT_CHANGE_HISTORY.CREATED_AT.greaterThan(token.timestamp()));
			}
		}

		final List<Record> results = createChangeHistoryQuery(DSL.and(conditions))
				.orderBy(VAULT_CHANGE_HISTORY.ID.sort(sortOrder), VAULT_CHANGE_HISTORY.CREATED_AT.sort(sortOrder))
				.limit(pageable.size() + 1)
				.fetch();

		final boolean hasMore = results.size() > pageable.size();
		final List<Record> content = hasMore ? results.subList(0, pageable.size()) : results;

		if (isReversed) {
			Collections.reverse(content);
		}

		CursorPageable nextPageable = null;
		CursorPageable previousPageable = null;

		// If we were moving forward and found 'more', we have a next page...
		// If we were moving backward, we definitely have a next page...
		if (hasMore || isReversed) {
			// Use the last element in the 'content' list to create the next token
			final Record lastItem = content.getLast();
			nextPageable = encodeNextCursor(
					lastItem.get(VAULT_CHANGE_HISTORY.ID),
					lastItem.get(VAULT_CHANGE_HISTORY.CREATED_AT),
					pageable.size()
			);
		}

		// the previous page should be specified when there was a token present...
		if (pageable.token() != null && (!isReversed || hasMore)) {
			final Record firstItem = content.getFirst();
			previousPageable = encodePreviousCursor(
					firstItem.get(VAULT_CHANGE_HISTORY.ID),
					firstItem.get(VAULT_CHANGE_HISTORY.CREATED_AT),
					pageable.size()
			);
		}

		// we have reached the end, there is no next pageable...
		return CursorPage.of(content, nextPageable, previousPageable)
				.map(ChangeHistoryService::toChangeHistory);
	}

	@Override
	@Transactional(label = "vault.examine-change-history", readOnly = true)
	public Optional<ChangeHistory> examine(Profile profile, String revision) {
		return createChangeHistoryQuery(DSL.and(
				VAULT_CHANGE_HISTORY.PROFILE_ID.eq(profile.id().get()),
				VAULT_CHANGE_HISTORY.REVISION.eq(revision)
		)).fetchOptional(ChangeHistoryService::toChangeHistory);
	}

	@Override
	@Transactional(label = "vault.trace-property-history", readOnly = true)
	public CursorPage<PropertyHistory> traceProperty(Profile profile, String propertyName, CursorPageable pageable) {
		final List<Condition> conditions = new ArrayList<>();
		conditions.add(VAULT_PROPERTY_HISTORY.PROFILE_ID.eq(profile.id().get()));
		conditions.add(VAULT_PROPERTY_HISTORY.PROPERTY_NAME.eq(propertyName));

		// it is an unpaged request, return all changes for property within a profile...
		if (pageable.isUnpaged()) {
			return CursorPage.of(
					createPropertyHistoryQuery(DSL.and(conditions))
							.orderBy(VAULT_PROPERTY_HISTORY.CREATED_AT.desc())
							.fetch(ChangeHistoryService::toPropertyHistory)
			);
		}

		final HistoryCursorToken token = decodeCursor(pageable);
		final boolean isReversed = token != null && token.reversed();
		final SortOrder sortOrder = isReversed ? SortOrder.ASC : SortOrder.DESC;

		if (token != null) {
			if (sortOrder == SortOrder.DESC) {
				conditions.add(VAULT_PROPERTY_HISTORY.CHANGE_ID.lessThan(token.identifier()));
				conditions.add(VAULT_PROPERTY_HISTORY.CREATED_AT.lessThan(token.timestamp()));
			} else {
				conditions.add(VAULT_PROPERTY_HISTORY.CHANGE_ID.greaterThan(token.identifier()));
				conditions.add(VAULT_PROPERTY_HISTORY.CREATED_AT.greaterThan(token.timestamp()));
			}
		}

		final List<Record> results = createPropertyHistoryQuery(DSL.and(conditions))
				.orderBy(VAULT_PROPERTY_HISTORY.CHANGE_ID.sort(sortOrder), VAULT_PROPERTY_HISTORY.CREATED_AT.sort(sortOrder))
				.limit(pageable.size() + 1)
				.fetch();

		final boolean hasMore = results.size() > pageable.size();
		final List<Record> content = hasMore ? results.subList(0, pageable.size()) : results;

		if (isReversed) {
			Collections.reverse(content);
		}

		CursorPageable nextPageable = null;
		CursorPageable previousPageable = null;

		// If we were moving forward and found 'more', we have a next page...
		// If we were moving backward, we definitely have a next page...
		if (hasMore || isReversed) {
			// Use the last element in the 'content' list to create the next token
			final Record lastItem = content.getLast();
			nextPageable = encodeNextCursor(
					lastItem.get(VAULT_PROPERTY_HISTORY.CHANGE_ID),
					lastItem.get(VAULT_PROPERTY_HISTORY.CREATED_AT),
					pageable.size()
			);
		}

		// the previous page should be specified when there was a token present...
		if (pageable.token() != null && (!isReversed || hasMore)) {
			final Record firstItem = content.getFirst();
			previousPageable = encodePreviousCursor(
					firstItem.get(VAULT_PROPERTY_HISTORY.CHANGE_ID),
					firstItem.get(VAULT_PROPERTY_HISTORY.CREATED_AT),
					pageable.size()
			);
		}

		// we have reached the end, there is no next pageable...
		return CursorPage.of(content, nextPageable, previousPageable)
				.map(ChangeHistoryService::toPropertyHistory);
	}

	@Override
	@Transactional(label = "vault.trace-revision-changes", readOnly = true)
	public List<PropertyHistory> traceRevision(ChangeHistory revision) {
		return createPropertyHistoryQuery(VAULT_PROPERTY_HISTORY.CHANGE_ID.eq(revision.id().get()))
				.fetch(ChangeHistoryService::toPropertyHistory);
	}

	private SelectConditionStep<Record> createChangeHistoryQuery(Condition condition) {
		return context.select(CHANGE_HISTORY_FIELDS)
				.from(VAULT_CHANGE_HISTORY)
				.where(condition);
	}

	private SelectConditionStep<Record> createPropertyHistoryQuery(Condition condition) {
		return context.select(PROPERTY_HISTORY_FIELDS)
				.from(VAULT_PROPERTY_HISTORY)
				.innerJoin(VAULT_CHANGE_HISTORY)
				.on(VAULT_CHANGE_HISTORY.ID.eq(VAULT_PROPERTY_HISTORY.CHANGE_ID))
				.where(condition);
	}

	private ChangeHistoryOwnership lookupOwnership(EntityId profile) {
		return context.select(SERVICES.NAMESPACE_ID, SERVICES.ID, VAULT_PROFILES.ID)
				.from(VAULT_PROFILES)
				.innerJoin(SERVICES)
				.on(SERVICES.ID.eq(VAULT_PROFILES.SERVICE_ID))
				.where(VAULT_PROFILES.ID.eq(profile.get()))
				.fetchOptional(ChangeHistoryOwnership::new)
				.orElseThrow(() -> new ProfileNotFoundException(profile));
	}

	@Nullable
	private static HistoryCursorToken decodeCursor(CursorPageable pageable) {
		return pageable.token() == null ? null : HistoryCursorToken.decode(pageable.token());
	}

	private static CursorPageable encodeNextCursor(long identifier, OffsetDateTime timestamp, int size) {
		return CursorPageable.of(
				HistoryCursorToken.of(identifier, timestamp).value(),
				size
		);
	}

	private static CursorPageable encodePreviousCursor(long identifier, OffsetDateTime timestamp, int size) {
		return CursorPageable.of(
				HistoryCursorToken.of(identifier, timestamp, true).value(),
				size
		);
	}

	private static ChangeHistory toChangeHistory(Record record) {
		return new ChangeHistory(
				record.get(VAULT_CHANGE_HISTORY.ID, EntityId.class),
				record.get(VAULT_CHANGE_HISTORY.REVISION),
				record.get(VAULT_CHANGE_HISTORY.SUBJECT),
				record.get(VAULT_CHANGE_HISTORY.DESCRIPTION),
				record.get(VAULT_CHANGE_HISTORY.CHANGE_COUNT),
				record.get(VAULT_CHANGE_HISTORY.AUTHOR_NAME),
				record.get(VAULT_CHANGE_HISTORY.CREATED_AT)
		);
	}

	private static PropertyHistory toPropertyHistory(Record record) {
		return new PropertyHistory(
				record.get(VAULT_CHANGE_HISTORY.REVISION),
				record.get(VAULT_PROPERTY_HISTORY.PROPERTY_NAME),
				record.get(VAULT_PROPERTY_HISTORY.CHANGE_OPERATION, PropertyTransitionType.class),
				toPropertyValue(
						record.get(VAULT_PROPERTY_HISTORY.OLD_VALUE_CHECKSUM),
						record.get(VAULT_PROPERTY_HISTORY.OLD_VALUE_CIPHER)
				),
				toPropertyValue(
						record.get(VAULT_PROPERTY_HISTORY.NEW_VALUE_CHECKSUM),
						record.get(VAULT_PROPERTY_HISTORY.NEW_VALUE_CIPHER)
				),
				record.get(VAULT_CHANGE_HISTORY.AUTHOR_NAME),
				record.get(VAULT_PROPERTY_HISTORY.CREATED_AT)
		);
	}

	private static PropertyValue toPropertyValue(@Nullable ByteArray checksum, @Nullable ByteArray cipher) {
		if (checksum == null || cipher == null) {
			return null;
		}
		return PropertyValue.sealed(cipher, checksum);
	}

	private record ChangeHistoryOwnership(long namespace, long service, long profile) {
		private ChangeHistoryOwnership(Record record) {
			this(
					record.get(SERVICES.NAMESPACE_ID),
					record.get(SERVICES.ID),
					record.get(VAULT_PROFILES.ID)
			);
		}
	}

}
