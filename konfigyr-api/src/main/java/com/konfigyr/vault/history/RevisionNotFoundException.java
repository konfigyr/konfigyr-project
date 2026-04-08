package com.konfigyr.vault.history;

import com.konfigyr.vault.VaultException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown when a {@link com.konfigyr.vault.ChangeHistory} does not exist.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class RevisionNotFoundException extends VaultException {

	@Serial
	private static final long serialVersionUID = 30756960861487345L;

	/**
	 * Create new instance of the {@link RevisionNotFoundException} when there are no
	 * {@link com.konfigyr.vault.ChangeHistory change history} for a given profile and revision number.
	 *
	 * @param profile profile name slug, can't be {@code null}
	 * @param revision the revision number, can't be {@code null}
	 */
	public RevisionNotFoundException(@NonNull String profile, @NonNull String revision) {
		super(HttpStatus.NOT_FOUND, "Could not find a change history with the following revision: %s within a %s Profile"
				.formatted(revision, profile));
	}

}
