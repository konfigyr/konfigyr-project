package com.konfigyr.security.basic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.konfigyr.namespace.Namespace;
import com.konfigyr.security.AuthenticatedPrincipal;
import com.konfigyr.security.PrincipalType;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static lombok.Builder.Default;

/**
 * Implementation of the {@link UserDetails}, {@link AuthenticatedPrincipal} interface used for Basic authentication
 * of applications within a namespace.
 * <p>
 * It is used when Config clients access the configuration server
 * using Basic authentication with {@code username} and {@code password}.
 * <p>
 * This is a temporary solution and is expected to be replaced with OAuth2-based
 * client authentication once Spring Cloud Config clients provide built-in
 * support for OAuth2.
 *
 * @author Mila Zarkovic
 * @since 1.0.0
 */
@Value
@Builder
@NullMarked
@EqualsAndHashCode(of = { "clientId" })
@ToString(of = { "clientId", "namespace", "expiresAt", "authorities" })
public class NamespaceApplicationPrincipal implements UserDetails, AuthenticatedPrincipal {

	@Serial
	private static final long serialVersionUID = 2390087892807966815L;

	/**
	 * Name of the {@link Namespace} that owns this application, can't be {@literal null}
	 */
	String namespace;

	/**
	 * Public identifier of the client associated with this application, can't be {@literal null}
	 */
	String clientId;

	/**
	 * Application's secret, can't be {@literal null}
	 */
	String password;

	/**
	 * Expiration timestamp for this application’s credentials
	 */
	@Nullable OffsetDateTime expiresAt;

	/**
	 * Authorities which this application has, can't be {@literal null}
	 */
	@Default
	Collection<? extends GrantedAuthority> authorities = Set.of();

	@Override
	public String get() {
		return clientId;
	}

	@Override
	public PrincipalType getType() {
		return PrincipalType.OAUTH_CLIENT;
	}

	@Override
	public Optional<String> getEmail() {
		return Optional.empty();
	}

	@Override
	public Optional<String> getDisplayName() {
		return Optional.of(namespace + "-" + clientId);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@Override
	@JsonIgnore
	public String getUsername() {
		return clientId;
	}

	@Override
	@JsonIgnore
	public boolean isCredentialsNonExpired() {
		return expiresAt == null || expiresAt.isAfter(OffsetDateTime.now());
	}
}
