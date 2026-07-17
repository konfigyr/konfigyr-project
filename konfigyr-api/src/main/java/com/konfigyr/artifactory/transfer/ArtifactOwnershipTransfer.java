package com.konfigyr.artifactory.transfer;

import com.konfigyr.artifactory.Owner;
import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Two-party request to move ownership of the artifacts a namespace holds under a Maven {@code groupId}
 * to a different namespace.
 * <p>
 * A transfer records the requesting namespace ({@code to}), the current owner of the affected artifacts
 * ({@code from}), the {@code groupId} whose artifacts are in scope, and the current lifecycle
 * {@link TransferState}. The request only takes effect once the current owner explicitly accepts it;
 * see {@link ArtifactOwnershipTransfers}.
 * <p>
 * The lifecycle starts at {@link TransferState#PENDING} when the request is first submitted. The current
 * owner may accept it, transitioning it to {@link TransferState#ACCEPTED} and moving ownership of every
 * artifact it holds under the {@code groupId}, or reject it, transitioning it to
 * {@link TransferState#REJECTED}. The requesting namespace may withdraw its own request, transitioning it
 * to {@link TransferState#CANCELLED}.
 *
 * @param id          the persistent transfer identifier; never {@literal null}
 * @param groupId     the Maven group coordinate whose artifacts are being requested; never {@literal null}
 * @param from        the namespace that currently owns the affected artifacts; never {@literal null}
 * @param to          the namespace requesting the transfer of ownership; never {@literal null}
 * @param state       the current transfer lifecycle state; never {@literal null}
 * @param requestedAt timestamp when the request was submitted; {@literal null} before persistence
 * @param resolvedAt  timestamp when the request transitioned to a resolved state; {@literal null} while {@link TransferState#PENDING}
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see TransferState
 * @see ArtifactOwnershipTransfers
 */
@NullMarked
@AggregateRoot
public record ArtifactOwnershipTransfer(
	@Identity EntityId id,
	String groupId,
	Owner from,
	Owner to,
	TransferState state,
	@Nullable OffsetDateTime requestedAt,
	@Nullable OffsetDateTime resolvedAt
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 5721300984659639120L;

	/**
	 * Creates a new {@link Builder fluent builder} instance used to construct an {@link ArtifactOwnershipTransfer}.
	 *
	 * @return artifact ownership transfer builder, never {@literal null}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a new {@link Builder} pre-populated with all values from this transfer, suitable for
	 * producing a modified copy.
	 *
	 * @return artifact ownership transfer builder, never {@literal null}
	 */
	public Builder toBuilder() {
		return new Builder()
				.id(this.id)
				.groupId(this.groupId)
				.from(this.from)
				.to(this.to)
				.state(this.state)
				.requestedAt(this.requestedAt)
				.resolvedAt(this.resolvedAt);
	}

	/**
	 * Fluent builder type used to create an {@link ArtifactOwnershipTransfer}.
	 */
	public static final class Builder {

		private @Nullable EntityId id;
		private @Nullable String groupId;
		private @Nullable Owner from;
		private @Nullable Owner to;
		private @Nullable TransferState state;
		private @Nullable OffsetDateTime requestedAt;
		private @Nullable OffsetDateTime resolvedAt;

		private Builder() {
		}

		/**
		 * Specify the persistent identifier of this {@link ArtifactOwnershipTransfer}.
		 *
		 * @param id transfer identifier; may be {@literal null} before persistence
		 * @return artifact ownership transfer builder
		 */
		public Builder id(@Nullable Long id) {
			return id(id == null ? null : EntityId.from(id));
		}

		/**
		 * Specify the persistent identifier of this {@link ArtifactOwnershipTransfer}.
		 *
		 * @param id transfer identifier; may be {@literal null} before persistence
		 * @return artifact ownership transfer builder
		 */
		public Builder id(@Nullable EntityId id) {
			this.id = id;
			return this;
		}

		/**
		 * Specify the Maven group coordinate whose artifacts are being requested.
		 *
		 * @param groupId group identifier; must not be blank
		 * @return artifact ownership transfer builder
		 */
		public Builder groupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * Specify the namespace that currently owns the affected artifacts.
		 *
		 * @param from current owner namespace; must not be {@literal null}
		 * @return artifact ownership transfer builder
		 */
		public Builder from(Owner from) {
			this.from = from;
			return this;
		}

		/**
		 * Specify the namespace requesting the transfer of ownership.
		 *
		 * @param to requesting namespace; must not be {@literal null}
		 * @return artifact ownership transfer builder
		 */
		public Builder to(Owner to) {
			this.to = to;
			return this;
		}

		/**
		 * Specify the current {@link TransferState} of this request.
		 *
		 * @param state transfer lifecycle state; must not be {@literal null}
		 * @return artifact ownership transfer builder
		 */
		public Builder state(TransferState state) {
			this.state = state;
			return this;
		}

		/**
		 * Specify when this request was submitted.
		 *
		 * @param requestedAt creation timestamp; may be {@literal null}
		 * @return artifact ownership transfer builder
		 */
		public Builder requestedAt(@Nullable OffsetDateTime requestedAt) {
			this.requestedAt = requestedAt;
			return this;
		}

		/**
		 * Specify when this request was resolved.
		 *
		 * @param resolvedAt resolution timestamp; may be {@literal null}
		 * @return artifact ownership transfer builder
		 */
		public Builder resolvedAt(@Nullable OffsetDateTime resolvedAt) {
			this.resolvedAt = resolvedAt;
			return this;
		}

		/**
		 * Creates a new {@link ArtifactOwnershipTransfer} from the values set on this builder.
		 *
		 * @return artifact ownership transfer, never {@literal null}
		 * @throws IllegalArgumentException when required fields are missing or invalid
		 */
		public ArtifactOwnershipTransfer build() {
			Assert.notNull(id, "Artifact ownership transfer identifier is required");
			Assert.hasText(groupId, "Artifact ownership transfer group identifier must not be blank");
			Assert.notNull(from, "Artifact ownership transfer 'from' owner must not be null");
			Assert.notNull(to, "Artifact ownership transfer 'to' owner must not be null");
			Assert.notNull(state, "Artifact ownership transfer state must not be null");

			return new ArtifactOwnershipTransfer(id, groupId, from, to, state, requestedAt, resolvedAt);
		}
	}
}
