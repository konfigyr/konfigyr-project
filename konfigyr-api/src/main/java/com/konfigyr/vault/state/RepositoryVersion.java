package com.konfigyr.vault.state;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.ClassUtils;

import java.time.OffsetDateTime;

/**
 * Represents an immutable snapshot of a repository state at a specific point in time.
 * <p>
 * This class captures the metadata associated with a specific revision, including unique
 * identification, authorship, descriptive summaries, and temporal information.
 * <p>
 * It serves as a historical metadata record of changes applied to a configuration profile.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
@ValueObject
@SuperBuilder
@EqualsAndHashCode
public class RepositoryVersion {

	protected final String revision;
	protected final String summary;
	protected final String author;
	protected final OffsetDateTime timestamp;

	/**
	 * Returns the revision identifier of the state and it's contents.
	 *
	 * @return the revision identifier, never {@literal null}
	 */
	public String revision() {
		return revision;
	}

	/**
	 * Returns the name of the author of the last change that was applied to the {@link com.konfigyr.vault.Profile}
	 * configiration state.
	 *
	 * @return the last author, never {@literal null}
	 */
	public String author() {
		return author;
	}

	/**
	 * Returns the summary of the last change that was applied to the {@link com.konfigyr.vault.Profile}.
	 *
	 * @return the last change summary, never {@literal null}
	 */
	public String summary() {
		return summary;
	}

	/**
	 * Returns the timestamp when the last change was applied to the {@link com.konfigyr.vault.Profile}.
	 *
	 * @return the last change timestamp, never {@literal null}
	 */
	public OffsetDateTime timestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return ClassUtils.getShortName(getClass())
				+ "[revision='" + revision + "', summary='" + summary + "', timestamp=" + timestamp + ']';
	}
}
