package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * Exception thrown when {@link Account} could not be found.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 **/
public class AccountNotFoundException extends AccountException {
	@Serial
	private static final long serialVersionUID = -3017953990661566250L;

	public AccountNotFoundException(@NonNull EntityId id) {
		this("Failed to find account with entity identifier: " + id);
	}

	public AccountNotFoundException(String message) {
		super(message);
	}
}
