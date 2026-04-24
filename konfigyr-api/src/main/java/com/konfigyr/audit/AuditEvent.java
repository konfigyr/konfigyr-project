package com.konfigyr.audit;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.AuthenticatedPrincipal;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a domain action to be recorded in the Konfigyr audit log.
 * <p>
 * An {@code AuditEvent} is the write-side representation used to report that something happened
 * within the platform; a namespace was renamed, a service was created, a member was added, and so on.
 * It captures what happened, to which entity, and who initiated it.
 * <p>
 * Use the {@link #builder()} API to construct instances from domain event handlers or service methods.
 * Once built, pass the event to the {@link AuditEventRepository} for persistence. The resulting persisted
 * entry is represented by {@link AuditRecord}.
 *
 * @param namespaceId optional namespace context for the event, can be {@literal null}.
 * @param entityType the type of entity the event pertains to (e.g. "namespace", "service"), can't be {@literal null}.
 * @param entityId the identifier of the entity the event pertains to, can't be {@literal null}.
 * @param eventType the type of event that occurred (e.g. "namespace.renamed", "service.created"), can't be {@literal null}.
 * @param actor the actor who triggered the event, can't be {@literal null}.
 * @param details optional event-specific payload, can be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record AuditEvent(
		@Nullable EntityId namespaceId,
		String entityType,
		EntityId entityId,
		String eventType,
		Actor actor,
		@Nullable Map<String, Object> details
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6397546134191608344L;

	/**
	 * Creates a new builder for constructing {@link AuditEvent} instances.
	 *
	 * @return a new {@link Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for constructing {@link AuditEvent} instances.
	 */
	public static final class Builder {
		private @Nullable EntityId namespaceId;
		private @Nullable String entityType;
		private @Nullable EntityId entityId;
		private @Nullable String eventType;
		private @Nullable Actor actor;

		private final Map<String, @Nullable Object> details;

		private Builder() {
			this.details = new LinkedHashMap<>();
		}

		/**
		 * Sets the namespace context for the event.
		 *
		 * @param namespaceId the namespace identifier
		 * @return this builder instance
		 */
		public Builder namespace(@Nullable EntityId namespaceId) {
			this.namespaceId = namespaceId;
			return this;
		}

		/**
		 * Sets the type of entity the event pertains to.
		 *
		 * @param entityType the entity type
		 * @return this builder instance
		 */
		public Builder entityType(String entityType) {
			this.entityType = entityType;
			return this;
		}

		/**
		 * Sets the identifier of the entity the event pertains to.
		 *
		 * @param entityId the entity identifier
		 * @return this builder instance
		 */
		public Builder entityId(EntityId entityId) {
			this.entityId = entityId;
			return this;
		}

		/**
		 * Sets the type of event that occurred.
		 *
		 * @param eventType the event type
		 * @return this builder instance
		 */
		public Builder eventType(String eventType) {
			this.eventType = eventType;
			return this;
		}

		/**
		 * Sets the actor who triggered the event based on the given {@link AuthenticatedPrincipal}.
		 *
		 * @param principal the authenticated principal
		 * @return this builder instance
		 */
		public Builder actor(AuthenticatedPrincipal principal) {
			return actor(new Actor(principal.get(), principal.getType().name(),
					principal.getDisplayName().orElseGet(principal)));
		}

		/**
		 * Sets the actor who triggered the event.
		 *
		 * @param actor the actor
		 * @return this builder instance
		 */
		public Builder actor(Actor actor) {
			this.actor = actor;
			return this;
		}

		/**
		 * Sets the optional event-specific payload.
		 *
		 * @param key the event detail key
		 * @param value the event detail value
		 * @return this builder instance
		 */
		public Builder details(String key, @Nullable Object value) {
			this.details.put(key, value);
			return this;
		}

		/**
		 * Sets the optional event-specific payload.
		 *
		 * @param details the event details
		 * @return this builder instance
		 */
		public Builder details(Map<String, @Nullable Object> details) {
			this.details.putAll(details);
			return this;
		}

		/**
		 * Builds and returns a new {@link AuditEvent} instance.
		 *
		 * @return a new {@link AuditEvent}
		 * @throws NullPointerException if any required field is null
		 */
		public AuditEvent build() {
			Assert.notNull(entityId, "Audit entity identifier must not be null");
			Assert.notNull(entityType, "Audit entity type must not be null");
			Assert.notNull(eventType, "Audit event type must not be null");
			Assert.notNull(actor, "Audit actor must not be null");
			return new AuditEvent(namespaceId, entityType, entityId, eventType, actor,
					Collections.unmodifiableMap(details));
		}
	}

}
