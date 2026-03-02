package com.konfigyr.test;

import com.konfigyr.account.Account;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.security.Principal;
import java.util.Optional;

@ToString
@NullMarked
@EqualsAndHashCode
final class TestAuthenticatedPrincipal implements AuthenticatedPrincipal, Principal {

	private final String subject;
	private final PrincipalType type;
	private @Nullable final String email;
	private @Nullable final String displayName;

	TestAuthenticatedPrincipal(Account account) {
		this.type = PrincipalType.USER_ACCOUNT;
		this.subject = account.id().serialize();
		this.email = account.email();
		this.displayName = account.displayName();
	}

	TestAuthenticatedPrincipal(String subject) {
		this.type = PrincipalType.OAUTH_CLIENT;
		this.subject = subject;
		this.email = null;
		this.displayName = "Test application: " + subject;
	}

	@Override
	public String get() {
		return subject;
	}

	@Override
	public String getName() {
		return get();
	}

	@Override
	public PrincipalType getType() {
		return type;
	}

	@Override
	public Optional<@Nullable String> getEmail() {
		return Optional.ofNullable(email);
	}

	@Override
	public Optional<@Nullable String> getDisplayName() {
		return Optional.ofNullable(displayName);
	}
}
