package com.konfigyr.namespace.dashboard;

import com.konfigyr.feature.Features;
import com.konfigyr.feature.LimitedFeatureValue;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.namespace.NamespaceFeatures;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NullMarked;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.Artifacts.ARTIFACTS;
import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;
import static com.konfigyr.data.tables.Services.SERVICES;
import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;
import static com.konfigyr.data.tables.VaultProperties.VAULT_PROPERTIES;

/**
 * Computes the {@link DashboardSummary rolled-up counts} shown on a namespace's overview page.
 * <p>
 * Reads state directly via jOOQ against the tables owned by other modules rather than depending on
 * their manager or repository interfaces, thus becoming a single place where namespace-wide count is
 * retrieved without introducing a dependency on those modules.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@RequiredArgsConstructor
public class Dashboards {

	static final Field<Long> COUNT = DSL.countLarge();

	private final DSLContext context;
	private final Features features;

	/**
	 * Computes the {@link DashboardSummary} for the given {@link Namespace}.
	 *
	 * @param namespace the namespace for which the summary is computed, can't be {@literal null}
	 * @return the computed dashboard summary, never {@literal null}
	 */
	@Transactional(readOnly = true, label = "namespace.dashboard-summary")
	public DashboardSummary summary(Namespace namespace) {
		final long id = namespace.id().get();

		return new DashboardSummary(
				countServices(id),
				memberSummary(namespace),
				countOpenChangeRequests(id),
				countActiveProperties(id),
				countArtifacts(id)
		);
	}

	private long countServices(long namespaceId) {
		return selectCount(SERVICES, SERVICES.NAMESPACE_ID.eq(namespaceId));
	}

	private DashboardSummary.Members memberSummary(Namespace namespace) {
		final long count = selectCount(NAMESPACE_MEMBERS, NAMESPACE_MEMBERS.NAMESPACE_ID.eq(namespace.id().get()));

		final Long limit = features.get(namespace.slug(), NamespaceFeatures.MEMBERS_COUNT)
				.filter(LimitedFeatureValue::isLimited)
				.map(LimitedFeatureValue::get)
				.orElse(null);

		return new DashboardSummary.Members(count, limit);
	}

	private long countOpenChangeRequests(long namespaceId) {
		// "OPEN" mirrors the persisted name of com.konfigyr.vault.ChangeRequestState.OPEN; the literal is
		// used here instead of the enum to avoid a namespace -> vault module dependency for a single value.
		return selectCountFrom(VAULT_CHANGE_REQUESTS)
				.innerJoin(SERVICES)
				.on(SERVICES.ID.eq(VAULT_CHANGE_REQUESTS.SERVICE_ID))
				.where(
						SERVICES.NAMESPACE_ID.eq(namespaceId),
						VAULT_CHANGE_REQUESTS.STATE.eq("OPEN")
				)
				.fetchOptional(COUNT)
				.orElse(0L);
	}

	private long countActiveProperties(long namespaceId) {
		return selectCount(VAULT_PROPERTIES, VAULT_PROPERTIES.NAMESPACE_ID.eq(namespaceId));
	}

	private long countArtifacts(long namespaceId) {
		return selectCount(ARTIFACTS, ARTIFACTS.NAMESPACE_ID.eq(namespaceId));
	}

	private SelectJoinStep<? extends Record> selectCountFrom(Table<?> table) {
		return context.select(COUNT).from(table);
	}

	private long selectCount(Table<?> table, Condition condition) {
		return selectCountFrom(table)
				.where(condition)
				.fetchOptional(COUNT)
				.orElse(0L);
	}

}
