package com.konfigyr.vault;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a configuration state merge operation into a profile branch results in a merge conflict.
 * It represents an optimistic concurrency conflict. It does <strong>not</strong> indicate repository
 * corruption or an invalid repository state.
 * <p>
 * This exception indicates that the profile branch was modified concurrently between the time the
 * operation started and the time the merge was attempted. This may happen during {@code apply(...)} when
 * merging the temporary changeset branch into the target profile branch or during the {@code merge(...)}
 * when merging an approved changeset into the target profile branch.
 * <p>
 * When this exception is thrown:
 * <ul>
 *     <li>The target profile branch remains unchanged</li>
 *     <li>The merge operation is aborted</li>
 *     <li>The caller is expected to refresh the state and retry</li>
 * </ul>
 * <p>
 * Individual occurrences should not trigger monitoring alerts, as conflicts are a normal outcome in
 * concurrent systems. However, unusually high conflict rates may indicate coordination issues
 * or insufficient locking at the infrastructure level.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class ConflictingProfileStateException extends VaultException {

	private final Profile profile;
	private final String conflicts;

	/**
	 * Creates a new {@code ConflictingProfileStateException}.
	 *
	 * @param profile the profile that was modified concurrently.
	 * @param conflicts the conflicts that occurred.
	 */
	public ConflictingProfileStateException(@NonNull Profile profile, @NonNull String conflicts) {
		super(HttpStatus.CONFLICT, "Configuration state conflicts occurred while applying changes to profile: " + profile.name());
		this.profile = profile;
		this.conflicts = conflicts;
		getBody().setProperty("conflicts", conflicts);
	}

	/**
	 * Returns the profile that was the subject of the configuration state conflict.
	 *
	 * @return the profile, never {@literal null}.
	 */
	@NonNull
	public Profile getProfile() {
		return profile;
	}

	/**
	 * Returns the diff as a {@code String} that describes the conflicts that occurred when attempting
	 * to update the profile configuration state.
	 *
	 * @return the conflicts, never {@literal null}.
	 */
	@NonNull
	public String getConflicts() {
		return conflicts;
	}

	@Override
	public Object @Nullable [] getDetailMessageArguments() {
		return new Object[] { profile.name() };
	}
}
