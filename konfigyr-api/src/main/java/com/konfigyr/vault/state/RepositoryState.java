package com.konfigyr.vault.state;

import com.konfigyr.vault.Properties;
import com.konfigyr.vault.PropertyValue;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.InputStreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * Represents the state of the configuration that is managed by the {@link StateRepository}.
 * <p>
 * The state represents the authoritative state of the configuration for a {@link com.konfigyr.vault.Profile}
 * that is stored in a version-controlled backend (e.g., Git). The state comes with a revision identifier
 * that identifies the exact version of the configuration and with a timestamp when this revision was
 * created.
 * <p>
 * The record also contains the information about the author of the change and a brief summary of the
 * change that was last applied.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see StateRepository
 */
@Builder
@NullMarked
@ValueObject
@RequiredArgsConstructor
@ToString(of = { "revision", "summary", "timestamp" })
public final class RepositoryState implements InputStreamSource {

	private final String revision;
	private final String summary;
	private final String author;
	private final OffsetDateTime timestamp;
	private final InputStreamSource contents;

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

	/**
	 * Opens the input stream to read the state contents. The contents of the input stream contain encrypted,
	 * or sealed, configuration {@link PropertyValue values}. Usually you should be using the {@link Properties}
	 * to read and deserialize the state contents.
	 *
	 * @return the input stream containing the state, never {@literal null}
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return contents.getInputStream();
	}

}
