package com.konfigyr.integration;

import com.konfigyr.entity.EntityId;
import com.konfigyr.data.PageableExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Optional;

import static com.konfigyr.data.tables.Integrations.INTEGRATIONS;
import static com.konfigyr.data.tables.Namespaces.NAMESPACES;

@Slf4j
@RequiredArgsConstructor
class DefaultIntegrationManager implements IntegrationManager {

	static final PageableExecutor executor = PageableExecutor.builder()
			.defaultSortField(INTEGRATIONS.CREATED_AT.desc())
			.sortField("date", INTEGRATIONS.CREATED_AT)
			.sortField("provider", INTEGRATIONS.PROVIDER)
			.build();

	private final DSLContext context;

	@NonNull
	@Override
	public Page<Integration> find(@NonNull EntityId namespace, @NonNull Pageable pageable) {
		final Condition condition = INTEGRATIONS.NAMESPACE_ID.eq(namespace.get());

		return executor.execute(
				createIntegrationsQuery(condition),
				DefaultIntegrationManager::integration,
				pageable,
				() -> context.fetchCount(createIntegrationsQuery(condition))
		);
	}
	@NonNull
	@Override
	public Page<Integration> find(@NonNull String namespace, @NonNull Pageable pageable) {
		final Condition condition = NAMESPACES.SLUG.eq(namespace);

		return executor.execute(
				createIntegrationsQuery(condition),
				DefaultIntegrationManager::integration,
				pageable,
				() -> context.fetchCount(createIntegrationsQuery(condition))
		);
	}

	@NonNull
	@Override
	public Optional<Integration> get(@NonNull  EntityId namespace, @NonNull  EntityId id) {
		return createIntegrationsQuery(DSL.and(
				INTEGRATIONS.ID.eq(id.get()),
				INTEGRATIONS.NAMESPACE_ID.eq(namespace.get())
		)).fetchOptional(DefaultIntegrationManager::integration);
	}

	@NonNull
	@Override
	public Optional<Integration> get(@NonNull  String namespace, @NonNull  EntityId id) {
		return createIntegrationsQuery(DSL.and(
				INTEGRATIONS.ID.eq(id.get()),
				NAMESPACES.SLUG.eq(namespace)
		)).fetchOptional(DefaultIntegrationManager::integration);
	}

	@NonNull
	private SelectConditionStep<? extends Record> createIntegrationsQuery(Condition condition) {
		return context
				.select(
						INTEGRATIONS.ID,
						INTEGRATIONS.NAMESPACE_ID,
						INTEGRATIONS.TYPE,
						INTEGRATIONS.PROVIDER,
						INTEGRATIONS.PROVIDER_REFERENCE,
						INTEGRATIONS.CREATED_AT,
						INTEGRATIONS.UPDATED_AT)
				.from(INTEGRATIONS)
				.innerJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(INTEGRATIONS.NAMESPACE_ID))
				.where(condition);
	}

	private static Integration integration(Record record) {
		return Integration.builder()
				.id(record.get(INTEGRATIONS.ID))
				.namespace(record.get(INTEGRATIONS.NAMESPACE_ID))
				.type(record.get(INTEGRATIONS.TYPE))
				.provider(record.get(INTEGRATIONS.PROVIDER))
				.reference(record.get(INTEGRATIONS.PROVIDER_REFERENCE))
				.createdAt(record.get(INTEGRATIONS.CREATED_AT))
				.updatedAt(record.get(INTEGRATIONS.UPDATED_AT))
				.build();
	}
}
