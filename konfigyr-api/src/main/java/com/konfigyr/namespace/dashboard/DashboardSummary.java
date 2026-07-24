package com.konfigyr.namespace.dashboard;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Rolled-up counts for a namespace, powering the namespace overview page with a single request
 * instead of one call per module.
 *
 * @param activeServices number of services managed by the namespace
 * @param members member count and, when the namespace's plan enforces one, the member limit
 * @param openChangeRequests number of open change requests across all the namespace's services
 * @param activeConfigurations number of currently active configuration properties across all the namespace's services
 * @param artifactsOwned number of artifacts owned by the namespace
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
public record DashboardSummary(
		long activeServices,
		Members members,
		long openChangeRequests,
		long activeConfigurations,
		long artifactsOwned
) implements Serializable {

	@Serial
	private static final long serialVersionUID = 3050927164630017341L;

	/**
	 * Member count and, when the namespace's plan enforces one, the member limit.
	 *
	 * @param count number of members currently in the namespace
	 * @param limit maximum number of members allowed by the namespace's plan, or {@literal null} when unlimited
	 */
	public record Members(long count, @Nullable Long limit) implements Serializable {

		@Serial
		private static final long serialVersionUID = 8318084617731072072L;
	}

}
