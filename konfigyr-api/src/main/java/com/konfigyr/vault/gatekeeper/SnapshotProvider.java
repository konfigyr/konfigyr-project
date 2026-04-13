package com.konfigyr.vault.gatekeeper;

import org.jspecify.annotations.NullMarked;

/**
 * Abstraction for lazily providing snapshot data required during evaluation.
 * <p>
 * A {@link SnapshotProvider} is responsible for retrieving and assembling a specific view
 * of the system (e.g., repository state or review state) based on the {@link GateContext}.
 * <p>
 * Providers are typically backed by external systems such as Git or the database and may
 * perform expensive operations. They are therefore invoked lazily and their results cached
 * within the context.
 *
 * @param <T> the context type
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@FunctionalInterface
interface SnapshotProvider<T> {

	/**
	 * Resolves a snapshot for the given context.
	 *
	 * @param context the evaluation context
	 * @return the resolved snapshot
	 */
	T get(GateContext context);
}
