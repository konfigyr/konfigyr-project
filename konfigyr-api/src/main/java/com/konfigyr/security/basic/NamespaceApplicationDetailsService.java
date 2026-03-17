package com.konfigyr.security.basic;

import com.konfigyr.security.OAuthScopes;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static com.konfigyr.data.tables.Namespaces.NAMESPACES;
import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

@NullMarked
@RequiredArgsConstructor
public class NamespaceApplicationDetailsService implements UserDetailsService {

	private final DSLContext context;

	@Override
	public NamespaceApplicationPrincipal loadUserByUsername(String clientId) throws UsernameNotFoundException {
		return lookupApplication(clientId).orElseThrow(() ->
				new UsernameNotFoundException("Could not find namespace application with the given clientId")
		);
	}

	private Optional<NamespaceApplicationPrincipal> lookupApplication(String client) {
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
				.fetchOptional(record -> {
					final String scopes = record.get(OAUTH_APPLICATIONS.SCOPES);

					return NamespaceApplicationPrincipal.builder()
							.namespace(record.get(NAMESPACES.SLUG))
							.clientId(record.get(OAUTH_APPLICATIONS.CLIENT_ID))
							.password(record.get(OAUTH_APPLICATIONS.CLIENT_SECRET))
							.expiresAt(record.get(OAUTH_APPLICATIONS.EXPIRES_AT))
							.authorities(OAuthScopes.parse(scopes).toAuthorities())
							.build();
				});
	}
}
