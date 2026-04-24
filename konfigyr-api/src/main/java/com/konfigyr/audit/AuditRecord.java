package com.konfigyr.audit;

import com.konfigyr.entity.EntityId;
import com.konfigyr.support.SearchQuery;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * An immutable record of a domain action that was persisted in the Konfigyr audit log.
 * <p>
 * While {@link AuditEvent} represents the intent to record an action, an {@code AuditRecord} is the
 * authoritative evidence that the action was captured; it includes the original event data together
 * with the system-assigned identifier and timestamp.
 * <p>
 * Audit records provide a unified view across all auditable domains (accounts, namespaces, services,
 * KMS, invitations) for compliance, incident investigation, and operational visibility. They are
 * retrieved from the {@link AuditEventRepository} using {@link SearchQuery} criteria defined as constants
 * on this type.
 *
 * @param id unique identifier of the audit record, can't be {@literal null}.
 * @param namespaceId optional namespace context for the event, can be {@literal null}.
 * @param entityType the type of entity the event pertains to (e.g. "namespace", "service"), can't be {@literal null}.
 * @param entityId the identifier of the entity the event pertains to, can't be {@literal null}.
 * @param eventType the type of event that occurred (e.g. "namespace.renamed", "service.created"), can't be {@literal null}.
 * @param actor the actor who triggered the event, can't be {@literal null}.
 * @param details optional event-specific payload, can be {@literal null}.
 * @param createdAt timestamp when the event was persisted, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record AuditRecord(
		String id,
		@Nullable EntityId namespaceId,
		String entityType,
		EntityId entityId,
		String eventType,
		Actor actor,
		@Nullable Map<String, Object> details,
		OffsetDateTime createdAt
) implements Comparable<AuditRecord>, Serializable {

	/**
	 * Search criteria that can be used to filter audit records by the namespace identifier.
	 * <p>
	 * Note that the {@link SearchQuery#NAMESPACE} criteria contains the namespace slug,
	 * while this criteria expects the namespace {@link EntityId} directly. The controller
	 * layer is responsible for resolving the slug to an identifier before building the query.
	 */
	public static final SearchQuery.Criteria<EntityId> NAMESPACE_ID_CRITERIA =
			SearchQuery.criteria("namespace-id", EntityId.class);

	/**
	 * Search criteria that can be used to filter audit records by the type of entity
	 * the event pertains to (e.g. "namespace", "service", "kms").
	 */
	public static final SearchQuery.Criteria<String> ENTITY_TYPE_CRITERIA =
			SearchQuery.criteria("entity-type", String.class);

	/**
	 * Search criteria that can be used to filter audit records by the identifier of the
	 * entity the event pertains to.
	 */
	public static final SearchQuery.Criteria<EntityId> ENTITY_ID_CRITERIA =
			SearchQuery.criteria("entity-id", EntityId.class);

	/**
	 * Search criteria that can be used to filter audit records by the type of event
	 * that occurred (e.g. "namespace.renamed", "service.created").
	 */
	public static final SearchQuery.Criteria<String> EVENT_TYPE_CRITERIA =
			SearchQuery.criteria("event-type", String.class);

	/**
	 * Search criteria that can be used to filter audit records by the identifier of
	 * the actor that triggered the event.
	 */
	public static final SearchQuery.Criteria<String> ACTOR_ID_CRITERIA =
			SearchQuery.criteria("actor-id", String.class);

	/**
	 * Search criteria that can be used to filter audit records that were created at or
	 * after this timestamp.
	 */
	public static final SearchQuery.Criteria<OffsetDateTime> FROM_CRITERIA =
			SearchQuery.criteria("from", OffsetDateTime.class);

	/**
	 * Search criteria that can be used to filter audit records that were created at or
	 * before this timestamp.
	 */
	public static final SearchQuery.Criteria<OffsetDateTime> TO_CRITERIA =
			SearchQuery.criteria("to", OffsetDateTime.class);

	@Override
	public int compareTo(AuditRecord o) {
		return createdAt.compareTo(o.createdAt);
	}

}
