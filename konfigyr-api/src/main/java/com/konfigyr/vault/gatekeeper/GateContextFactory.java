package com.konfigyr.vault.gatekeeper;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.ServiceNotFoundException;
import com.konfigyr.namespace.Services;
import com.konfigyr.vault.ChangeRequestNotFoundException;
import com.konfigyr.vault.ChangeRequestState;
import com.konfigyr.vault.ProfileManager;
import com.konfigyr.vault.ProfileNotFoundException;
import com.konfigyr.vault.state.StateRepositoryFactory;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

import static com.konfigyr.data.tables.VaultProfiles.VAULT_PROFILES;
import static com.konfigyr.data.tables.VaultChangeRequests.VAULT_CHANGE_REQUESTS;

/**
 * Factory responsible for constructing {@link GateContext} instances.
 * <p>
 * The factory assembles the minimal required information to start evaluation, wiring in
 * the appropriate {@link SnapshotProvider}s that will supply repository and review data
 * lazily.
 * <p>
 * It deliberately avoids performing heavy operations such as conflict checks or history
 * aggregation. Those concerns are delegated to snapshot providers and only executed when
 * explicitly required by a gate.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see GateContext
 */
@RequiredArgsConstructor
class GateContextFactory {

	private final DSLContext context;
	private final Services services;
	private final ProfileManager profiles;
	private final StateRepositoryFactory stateRepositoryFactory;

	/**
	 * Creates a new {@link GateContext} for the given change request identifier.
	 * <p>
	 * The returned context is ready for evaluation by the {@link ChangeRequestGatekeeper}.
	 *
	 * @param changeRequestId change request identifier for which to create the context
	 * @return the gate context for the change request, never {@literal null}.
	 */
	@Transactional(readOnly = true, label = "vault.gatekeeper.context-factory")
	GateContext create(EntityId changeRequestId) {
		return retrieveChangeRequestContextBuilder(changeRequestId)
				.serviceSnapshotProvider(ctx -> services.get(ctx.serviceId())
						.orElseThrow(() -> new ServiceNotFoundException(ctx.serviceId()))
				)
				.profileSnapshotProvider(ctx -> profiles.get(ctx.profileId())
						.orElseThrow(() -> new ProfileNotFoundException(ctx.profileId()))
				)
				.repositorySnapshotProvider(new RepositorySnapshot.Provider(stateRepositoryFactory))
				.reviewSnapshotProvider(new ReviewSnapshot.Provider(context))
				.build();
	}

	private GateContext.Builder retrieveChangeRequestContextBuilder(EntityId changeRequestId) {
		return context.select(
				VAULT_PROFILES.ID,
				VAULT_PROFILES.SERVICE_ID,
				VAULT_CHANGE_REQUESTS.ID,
				VAULT_CHANGE_REQUESTS.STATE,
				VAULT_CHANGE_REQUESTS.BASE_REVISION,
				VAULT_CHANGE_REQUESTS.HEAD_REVISION
		)
				.from(VAULT_CHANGE_REQUESTS)
				.innerJoin(VAULT_PROFILES)
				.on(VAULT_PROFILES.ID.eq(VAULT_CHANGE_REQUESTS.PROFILE_ID))
				.where(VAULT_CHANGE_REQUESTS.ID.eq(changeRequestId.get()))
				.fetchOptional(record -> new GateContext.Builder()
						.profileId(record.get(VAULT_PROFILES.ID, EntityId.class))
						.serviceId(record.get(VAULT_PROFILES.SERVICE_ID, EntityId.class))
						.changeRequestId(record.get(VAULT_CHANGE_REQUESTS.ID, EntityId.class))
						.changeRequestState(record.get(VAULT_CHANGE_REQUESTS.STATE, ChangeRequestState.class))
						.baseRevision(record.get(VAULT_CHANGE_REQUESTS.BASE_REVISION))
						.headRevision(record.get(VAULT_CHANGE_REQUESTS.HEAD_REVISION))
				).orElseThrow(() -> new ChangeRequestNotFoundException(changeRequestId));
	}

}
