package com.konfigyr.artifactory.ownership;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.ddd.annotation.ValueObject;

import java.io.Serializable;

/**
 * Lightweight reference to a namespace owner in the artifactory bounded context.
 * <p>
 * This value object carries the namespace identifier and slug so ownership workflows can reference the
 * namespace without depending on the full namespace aggregate.
 *
 * @param id   namespace entity identifier
 * @param slug namespace slug used for URL construction
 * @author Vitalii Kushnir
 */
@ValueObject
public record Owner(
		@NonNull @Identity EntityId id,
		@NonNull String slug
) implements Serializable {

	public static @NonNull Owner of(EntityId id, String slug) {
		return new Owner(id, slug);
	}
}
