package com.konfigyr.security.oauth;

import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.Serial;
import java.util.Optional;

/**
 * Implementation of the {@link AuthenticatedPrincipal} interface backed by an {@link Jwt}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@ToString(of = { "subject", "type" })
@EqualsAndHashCode(of = { "subject", "type" })
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class OAuthAuthenticatedPrincipal implements AuthenticatedPrincipal {

	@Serial
	private static final long serialVersionUID = 8120423946731900333L;

	private final String subject;
	private final PrincipalType type;
	private final Jwt jwt;

	@NonNull
	@Override
	public String get() {
		return subject;
	}

	@NonNull
	@Override
	public PrincipalType getType() {
		return type;
	}

	@NonNull
	@Override
	public Optional<String> getEmail() {
		return Optional.ofNullable(jwt.getClaimAsString(StandardClaimNames.EMAIL));
	}

	@NonNull
	@Override
	public Optional<String> getDisplayName() {
		return Optional.ofNullable(jwt.getClaimAsString(StandardClaimNames.NAME));
	}

}
