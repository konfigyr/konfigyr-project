package com.konfigyr.security.basic;

import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import lombok.*;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.security.Principal;
import java.util.Optional;


/**
 * Implementation of the {@link AuthenticatedPrincipal} interface used for basic authentication.
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@RequiredArgsConstructor
@EqualsAndHashCode(of = { "clientId" })
@ToString(of = { "clientId", "namespace" })
public final class BasicAuthenticatedPrincipal implements AuthenticatedPrincipal, Principal {

	@Serial
	private static final long serialVersionUID = 2390087892807966815L;

	private final String clientId;
	@Getter
	private final String namespace;

	@NonNull
	@Override
	public String get() {
		return clientId;
	}

	@NonNull
	@Override
	public PrincipalType getType() {
		return PrincipalType.OAUTH_CLIENT;
	}

	@NonNull
	@Override
	public Optional<String> getEmail() {
		return Optional.empty();
	}

	@NonNull
	@Override
	public Optional<String> getDisplayName() {
		return Optional.of(namespace + "-" + clientId);
	}

	@Override
	public String getName() {
		return clientId;
	}
}
