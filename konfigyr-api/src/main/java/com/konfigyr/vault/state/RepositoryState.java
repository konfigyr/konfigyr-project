package com.konfigyr.vault.state;

import com.konfigyr.vault.Properties;
import com.konfigyr.vault.PropertyValue;
import lombok.experimental.SuperBuilder;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.InputStreamSource;

import java.io.IOException;
import java.io.InputStream;

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
@NullMarked
@ValueObject
@SuperBuilder
public final class RepositoryState extends RepositoryVersion implements InputStreamSource {

	private final InputStreamSource contents;

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
