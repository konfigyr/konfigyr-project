package com.konfigyr.vault.state;

import lombok.EqualsAndHashCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

/**
 * Type that represents the result of attempting to apply a merge of two different changes
 * in a source control repository.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@EqualsAndHashCode(exclude = "timestamp")
public final class MergeOutcome {

	/**
	 * The name of the branch that is the target of the merge, e.g. {@code main}.
	 */
	private final @NonNull String branch;

	/**
	 * The display name of the author that attempted the merge operation.
	 */
	private final @NonNull String author;

	/**
	 * The revision identifier of the merged changes, or {@literal null} if the merge failed.
	 */
	private final @Nullable String revision;

	/**
	 * The list of conflicts that occurred during the merge, or {@literal null} if there were none.
	 */
	private final @Nullable String conflicts;

	/**
	 * The timestamp when the merge outcome was created, defaults to current time.
	 */
	private final OffsetDateTime timestamp = OffsetDateTime.now();

	private MergeOutcome(@NonNull String branch, @NonNull String author, @Nullable String revision, @Nullable String conflicts) {
		Assert.hasText(branch, "Branch name must not be empty");
		Assert.hasText(author, "Author must not be empty");
		if (revision != null && conflicts != null) {
			throw new IllegalStateException("Can create a merge outcome with both commit identifier and conflicts");
		}
		this.branch = branch;
		this.author = author;
		this.revision = revision;
		this.conflicts = conflicts;
	}

	/**
	 * Returns the unknown merge outcome. This should occur when the changes were not committed to the
	 * target repository and do not contain any conflicts.
	 *
	 * @param branch the name of the branch that is the target of the merge, must not be {@literal null}.
	 * @param author the display name of the author that attempted the merge operation, must not be {@literal null}.
	 * @return the unknown merge outcome.
	 */
	public static MergeOutcome unknown(@NonNull String branch, @NonNull String author) {
		return new MergeOutcome(branch, author, null, null);
	}

	/**
	 * Creates an applied merge outcome with the given commit identifier.
	 *
	 * @param branch the name of the branch that is the target of the merge, must not be {@literal null}.
	 * @param author the display name of the author that attempted the merge operation, must not be {@literal null}.
	 * @param commit the commit identifier of the merged changes, can be {@literal null}.
	 * @return the applied merge outcome.
	 */
	public static MergeOutcome applied(@NonNull String branch, @NonNull String author, @NonNull String commit) {
		Assert.hasText(commit, "Commit identifier must not be empty");
		return new MergeOutcome(branch, author, commit, null);
	}

	/**
	 * Creates a merge outcome with conflicts indicating that the changes were not applied to the
	 * target repository due to conflicts.
	 *
	 * @param branch the name of the branch that is the target of the merge, must not be {@literal null}.
	 * @param author the display name of the author that attempted the merge operation, must not be {@literal null}.
	 * @param conflicts the conflicting lines that occurred during the merge, can be {@literal null}.
	 * @return the merge outcome with conflicts.
	 */
	public static MergeOutcome conflicting(@NonNull String branch, @NonNull String author, @NonNull String conflicts) {
		Assert.hasText(conflicts, "Conflicts must not be empty");
		return new MergeOutcome(branch, author, null, conflicts);
	}

	public boolean isUnknown() {
		return (!isApplied() && !isConflicting());
	}

	/**
	 * The name of the branch that was the target of the merge, e.g. {@code main}.
	 *
	 * @return the branch name, never {@literal null}.
	 */
	public @NonNull String branch() {
		return branch;
	}

	/**
	 * The display name of the author that attempted the merge operation.
	 *
	 * @return the author name, never {@literal null}.
	 */
	public @NonNull String author() {
		return author;
	}

	/**
	 * Returns the commit identifier of the merged changes, or {@literal null} if the merge failed.
	 *
	 * @return the commit identifier, or {@literal null}.
	 */
	public @Nullable String revision() {
		return revision;
	}

	/**
	 * Returns the list of conflicts that occurred during the merge, or {@literal null} if there were none.
	 *
	 * @return the list of conflicts, or {@literal null}.
	 */
	public @Nullable String conflicts() {
		return conflicts;
	}

	/**
	 * The timestamp when the merge outcome was created.
	 *
	 * @return the merge outcome timestamp, never {@literal null}.
	 */
	public @NonNull OffsetDateTime timestamp() {
		return timestamp;
	}

	/**
	 * Checks whether the merge was applied successfully. When applied without errors, the
	 * commit identifier would be set.
	 *
	 * @return {@literal true} if the merge was applied successfully, {@literal false} otherwise.
	 */
	public boolean isApplied() {
		return StringUtils.hasText(revision);
	}

	/**
	 * Checks if there were conflicts during the merge.
	 *
	 * @return {@literal true} if there were conflicts, {@literal false} otherwise.
	 */
	public boolean isConflicting() {
		return StringUtils.hasText(conflicts);
	}

	@NonNull
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("MergeOutcome(")
				.append(branch);

		if (isUnknown()) {
			return builder.append(", outcome=unknown)").toString();
		}

		if (isApplied()) {
			builder.append(", revision=").append(revision);
		} else {
			builder.append(", outcome=conflict");
		}

		return builder.append(')').toString();
	}
}
