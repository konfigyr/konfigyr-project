package com.konfigyr.identity.authorization.client;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.identity.authorization.AuthorizationServerScopes;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Set;
import java.util.function.Function;

public final class KonfigyrRegisteredClientRepository implements RegisteredClientRepository {

	private static final Set<AuthorizationGrantType> SUPPORTED_GRANT_TYPES = Set.of(
			AuthorizationGrantType.AUTHORIZATION_CODE,
			AuthorizationGrantType.CLIENT_CREDENTIALS,
			AuthorizationGrantType.REFRESH_TOKEN
	);

	private static final Set<ClientAuthenticationMethod> SUPPORTED_AUTHENTICATION_METHODS = Set.of(
			ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
			ClientAuthenticationMethod.CLIENT_SECRET_POST
	);

	private final RegisteredClient client;

	public KonfigyrRegisteredClientRepository(AuthorizationProperties properties) {
		client = RegisteredClient.withId("konfigyr")
				.clientName(properties.getClientName())
				.clientId(properties.getClientId())
				.clientSecret(properties.getClientSecret())
				.scopes(scopes -> AuthorizationServerScopes.get().forEach(scope -> {
					AuthorizationServerScopes.register(scopes);
					scope.getIncluded().forEach(it -> scopes.add(it.getAuthority()));
				}))
				.authorizationGrantTypes(types -> types
						.addAll(SUPPORTED_GRANT_TYPES)
				)
				.clientAuthenticationMethods(methods -> methods
						.addAll(SUPPORTED_AUTHENTICATION_METHODS)
				)
				.clientSettings(RegisteredClientSettings.createClientSettings().build())
				.tokenSettings(RegisteredClientSettings.createTokenSettings(properties).build())
				.redirectUris(uris -> properties.getRedirectUris().forEach(uri ->
						RegisteredClientSettings.mapper().from(uri).whenHasText().to(uris::add)
				))
				.postLogoutRedirectUris(uris -> properties.getPostLogoutRedirectUris().forEach(uri ->
						RegisteredClientSettings.mapper().from(uri).whenHasText().to(uris::add)
				))
				.build();
	}

	@Override
	public void save(RegisteredClient registeredClient) {
		throw new UnsupportedOperationException("Registering OAuth clients is not supported");
	}

	@Override
	public RegisteredClient findById(String id) {
		return find(RegisteredClient::getId, id);
	}

	@Override
	public RegisteredClient findByClientId(String clientId) {
		return find(RegisteredClient::getClientId, clientId);
	}

	private RegisteredClient find(Function<RegisteredClient, String> supplier, String id) {
		return supplier.apply(client).equals(id) ? client : null;
	}

}
