package com.konfigyr.identity.authorization.client;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.security.OAuthScopes;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Collection;
import java.util.Set;

public class KonfigyrRegisteredClientRepository extends AbstractRegisteredClientRepository {

	private static final Set<AuthorizationGrantType> SUPPORTED_GRANT_TYPES = Set.of(
			AuthorizationGrantType.AUTHORIZATION_CODE,
			AuthorizationGrantType.CLIENT_CREDENTIALS,
			AuthorizationGrantType.REFRESH_TOKEN
	);

	private static final OAuthScopes SUPPORTED_SCOPES = OAuthScopes.of(OAuthScope.OPENID, OAuthScope.NAMESPACES);

	private final RegisteredClient client;

	public KonfigyrRegisteredClientRepository(AuthorizationProperties properties) {
		super(properties);

		client = createRegisteredClient("konfigyr")
				.clientName(properties.getClientName())
				.clientId(properties.getClientId())
				.clientSecret(properties.getClientSecret())
				.scopes(scopes -> SUPPORTED_SCOPES.forEach(scope -> {
					scopes.add(scope.getAuthority());
					scope.getIncluded().forEach(it -> scopes.add(it.getAuthority()));
				}))
				.clientSettings(createClientSettings().build())
				.tokenSettings(createTokenSettings().build())
				.build();
	}

	@Override
	public RegisteredClient findById(String id) {
		return client.getId().equals(id) ? client : null;
	}

	@Override
	public RegisteredClient findByClientId(String clientId) {
		return client.getClientId().equals(clientId) ? client : null;
	}

	@NonNull
	@Override
	protected Collection<AuthorizationGrantType> getAuthorizationGrantTypes() {
		return SUPPORTED_GRANT_TYPES;
	}

}
