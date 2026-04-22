package com.konfigyr.audit;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;

/**
 * Identifies who performed a domain action captured in the audit log.
 * <p>
 * An {@code Actor} is a reusable value object shared by both the write-side {@link AuditEvent}
 * and the read-side {@link AuditRecord}, providing a consistent representation of the principal
 * responsible for an audited operation.
 *
 * @param id unique identifier of the actor, can't be {@literal null}.
 * @param type classification of the actor (e.g. "user", "system", "oauth-client"), can't be {@literal null}.
 * @param name human-readable display name of the actor, can't be {@literal null}.
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record Actor(String id, String type, String name) implements Serializable {

	@Serial
	private static final long serialVersionUID = 4293031633369809224L;

}
