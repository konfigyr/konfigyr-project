package com.konfigyr.artifactory;

import com.konfigyr.entity.EntityId;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;

/**
 * Identity of a claim holder in the artifactory bounded context.
 * <p>
 * {@code Owner} is a namespace derivative, a minimal projection carrying only the fields
 * required by ownership workflows, without pulling in the full tenant aggregate. Resolved
 * via {@link OwnerResolver}.
 *
 * @param id   entity identifier of the backing namespace
 * @param slug human-readable identifier used in URLs and log output
 * @author Vitalii Kushnir
 * @since 1.0.0
 * @see OwnerResolver
 */
@NullMarked
@ValueObject
public record Owner(@Identity EntityId id, String slug) implements Serializable {

	@Serial
	private static final long serialVersionUID = 6798118053033802100L;

}
