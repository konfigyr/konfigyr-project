package com.konfigyr.security.provider;

import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import com.konfigyr.security.basic.BasicAuthenticatedPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

@Slf4j
@RequiredArgsConstructor
public class ConfigClientAuthenticationProvider implements AuthenticationProvider, InitializingBean {

	private final DSLContext context;
	private final PasswordEncoder passwordEncoder;

	@Nullable
	@Override
	public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
		Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
				"Authentication must be an instance of UsernamePasswordAuthenticationToken");

		final String clientId = authentication.getName();
		final Object credentials = authentication.getCredentials();

		if (clientId == null || credentials == null) {
			throw new BadCredentialsException("Invalid credentials. Credentials can not be null.");
		}

		final Application application = lookupApplication(clientId);

		if (application == null) {
			throw new BadCredentialsException("Invalid credentials. Could not find application " + clientId);
		}

		if (application.isExpired()) {
			throw new CredentialsExpiredException("Config client credentials expired.");
		}

		if (!passwordEncoder.matches(credentials.toString(), application.secret())) {
			throw new BadCredentialsException("Invalid config client credentials. Passwords do not match.");
		}

		log.debug("Config client successfully authenticated for application {}", application.client());

		return new UsernamePasswordAuthenticationToken(
				new BasicAuthenticatedPrincipal(application.client(), application.namespace()), null, application.authorities()
		);
	}

	@Override
	public boolean supports(@NonNull Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.context, "DSL context must be set");
		Assert.notNull(this.passwordEncoder, "Password encoder must be set");
	}

	private Application lookupApplication(String client) {
		return context.select(
						OAUTH_APPLICATIONS.CLIENT_ID,
						OAUTH_APPLICATIONS.CLIENT_SECRET,
						OAUTH_APPLICATIONS.EXPIRES_AT,
						OAUTH_APPLICATIONS.SCOPES,
						NAMESPACES.SLUG
				)
				.from(OAUTH_APPLICATIONS)
				.leftJoin(NAMESPACES)
				.on(NAMESPACES.ID.eq(OAUTH_APPLICATIONS.NAMESPACE_ID))
				.where(OAUTH_APPLICATIONS.CLIENT_ID.eq(client))
				.fetchOneInto(Application.class);
	}

	record Application(@NonNull String client, @NonNull String secret, OffsetDateTime expires, String scopes, @NonNull String namespace) {
		boolean isExpired() {
			return expires != null && expires.isBefore(OffsetDateTime.now());
		}

		@NonNull
		Collection<? extends GrantedAuthority> authorities() {
			return OAuthScopes.parse(scopes).toAuthorities();
		}
	}
}
