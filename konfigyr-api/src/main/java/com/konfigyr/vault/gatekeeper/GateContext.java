package com.konfigyr.vault.gatekeeper;

import com.konfigyr.entity.EntityId;
import com.konfigyr.namespace.Service;
import com.konfigyr.vault.ChangeRequestState;
import com.konfigyr.vault.Profile;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Represents the full evaluation context used by the Gatekeeper when determining whether
 * a {@link com.konfigyr.vault.ChangeRequest} can be merged.
 * <p>
 * The {@link GateContext} is intentionally designed as a <b>lazy, snapshot-backed container</b>.
 * It starts with inexpensive, already-known data (such as identifiers and lifecycle state) and
 * exposes additional information (repository state, review state) via accessors that resolve
 * data on demand.
 * <p>
 * This design ensures that:
 * <ul>
 *     <li>
 *         Expensive operations (Git access, database reads) are only performed when required.
 *     </li>
 *     <li>
 *         Each piece of data is resolved at most once per evaluation (memoization).
 *     </li>
 *     <li>
 *         All rules operate on a consistent logical snapshot within a single evaluation run.
 *     </li>
 * </ul>
 * <p>
 * The context is created by the {@link GateContextFactory} and is expected to be used only within
 * a single evaluation cycle. It must not be reused across requests or threads.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class GateContext {
	private final EntityId serviceId;
	private final EntityId profileId;
	private final EntityId changeRequestId;
	private final ChangeRequestState changeRequestState;
	private final String baseRevision;
	private final String headRevision;

	private final Lazy<Profile> profile;
	private final Lazy<Service> service;
	private final Lazy<ReviewSnapshot> review;
	private final Lazy<RepositorySnapshot> repository;

	private GateContext(Builder builder) {
		Assert.notNull(builder.serviceId, "Service identifier must not be null");
		Assert.notNull(builder.profileId, "Profile identifier must not be null");
		Assert.notNull(builder.changeRequestId, "Change request identifier must not be null");
		Assert.notNull(builder.changeRequestState, "Change request state must not be null");
		Assert.notNull(builder.baseRevision, "Base revision must not be null");
		Assert.notNull(builder.headRevision, "Head revision must not be null");
		Assert.notNull(builder.profileSnapshotProvider, "Profile snapshot provider must not be null");
		Assert.notNull(builder.serviceSnapshotProvider, "Service snapshot provider must not be null");
		Assert.notNull(builder.reviewSnapshotProvider, "Review snapshot provider must not be null");
		Assert.notNull(builder.repositorySnapshotProvider, "Repository snapshot provider must not be null");

		this.serviceId = builder.serviceId;
		this.profileId = builder.profileId;
		this.changeRequestId = builder.changeRequestId;
		this.changeRequestState = builder.changeRequestState;
		this.baseRevision = builder.baseRevision;
		this.headRevision = builder.headRevision;
		this.profile = Lazy.of(() -> builder.profileSnapshotProvider.get(this));
		this.service = Lazy.of(() -> builder.serviceSnapshotProvider.get(this));
		this.review = Lazy.of(() -> builder.reviewSnapshotProvider.get(this));
		this.repository = Lazy.of(() -> builder.repositorySnapshotProvider.get(this));
	}

	/**
	 * Returns the entity identifier of the {@link Service} that is the owner of the change request.
	 *
	 * @return the service entity identifier, never {@literal null}.
	 */
	EntityId serviceId() {
		return serviceId;
	}

	/**
	 * Returns the entity identifier of the {@link Profile} against which the change request is being
	 * created and now evaluated.
	 *
	 * @return the target profile entity identifier, never {@literal null}.
	 */
	EntityId profileId() {
		return profileId;
	}

	/**
	 * Returns the entity identifier of the {@link com.konfigyr.vault.ChangeRequest} associated with
	 * this context.
	 *
	 * @return the change request entity identifier, never {@literal null}.
	 */
	EntityId changeRequestId() {
		return changeRequestId;
	}

	/**
	 * Returns the lifecycle state of the {@link com.konfigyr.vault.ChangeRequest} associated with
	 * this context.
	 *
	 * @return the change request state, never {@literal null}.
	 */
	ChangeRequestState changeRequestState() {
		return changeRequestState;
	}

	/**
	 * Returns the base revision number the change request was created against.
	 *
	 * @return the base revision number, never {@literal null}.
	 */
	String baseRevision() {
		return baseRevision;
	}

	/**
	 * Returns the head revision number representing the change request contents.
	 *
	 * @return the head revision number, never {@literal null}.
	 */
	public String headRevision() {
		return headRevision;
	}

	/**
	 * Lazily resolves and returns the {@link Service} instance.
	 *
	 * @return the service instance, never {@literal null}.
	 */
	Service service() {
		return service.get();
	}

	/**
	 * Lazily resolves and returns the {@link Profile} instance.
	 *
	 * @return the profile instance, never {@literal null}.
	 */
	Profile profile() {
		return profile.get();
	}

	/**
	 * Lazily resolves and returns the snapshot of the {@link com.konfigyr.vault.ChangeRequest} review
	 * state.
	 * <p>
	 * The snapshot aggregates review-related information derived from the event store (history). It is
	 * resolved once and cached.
	 *
	 * @return the review snapshot, never {@literal null}.
	 */
	ReviewSnapshot reviewSnapshot() {
		return review.get();
	}

	/**
	 * Lazily resolves and returns the repository snapshot.
	 * <p>
	 * The snapshot is obtained via the snapshot provider and cached for the duration of this
	 * context instance.
	 *
	 * @return the repository snapshot, never {@literal null}.
	 */
	RepositorySnapshot repositorySnapshot() {
		return repository.get();
	}

	@Override
	public String toString() {
		return "GateContext[id=" + changeRequestId + ", service=" + serviceId + ", profile=" + profileId +
				", state=" + changeRequestState + ", base='" + baseRevision + "', head='" + headRevision + "']";
	}

	@Setter
	@Accessors(fluent = true)
	static final class Builder {
		private @Nullable EntityId serviceId;
		private @Nullable EntityId profileId;
		private @Nullable EntityId changeRequestId;
		private @Nullable ChangeRequestState changeRequestState;
		private @Nullable String baseRevision;
		private @Nullable String headRevision;
		private @Nullable SnapshotProvider<Profile> profileSnapshotProvider;
		private @Nullable SnapshotProvider<Service> serviceSnapshotProvider;
		private @Nullable SnapshotProvider<ReviewSnapshot> reviewSnapshotProvider;
		private @Nullable SnapshotProvider<RepositorySnapshot> repositorySnapshotProvider;

		GateContext build() {
			return new GateContext(this);
		}
	}

}
