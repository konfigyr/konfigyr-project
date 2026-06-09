package com.konfigyr.identity.authorization.client;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import com.konfigyr.security.OAuthScopes;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;

import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

public class RegisteredNamespaceClientRepository implements RegisteredClientRepository {

	private final DSLContext context;
	private final AuthorizationProperties properties;

	public RegisteredNamespaceClientRepository(AuthorizationProperties properties, DSLContext context) {
		this.properties = properties;
		this.context = context;
	}

	@Override
	public void save(RegisteredClient registeredClient) {
		throw new UnsupportedOperationException("Registering OAuth clients is not supported");
	}

	@Override
	public RegisteredClient findById(String registrationId) {
		if (StringUtils.isBlank(registrationId)) {
			return null;
		}

		final EntityId id;

		try {
			id = EntityId.from(registrationId);
		} catch (IllegalArgumentException e) {
			return null;
		}

		return lookup(OAUTH_APPLICATIONS.ID.eq(id.get()));
	}

	@Override
	public RegisteredClient findByClientId(String clientId) {
		if (!NamespaceClientId.isPotentialClientId(clientId)) {
			return null;
		}
		return lookup(OAUTH_APPLICATIONS.CLIENT_ID.eq(clientId));
	}

	private RegisteredClient lookup(Condition condition) {
		return context.select(
						OAUTH_APPLICATIONS.ID,
						OAUTH_APPLICATIONS.NAME,
						OAUTH_APPLICATIONS.TYPE,
						OAUTH_APPLICATIONS.CLIENT_ID,
						OAUTH_APPLICATIONS.CLIENT_SECRET,
						OAUTH_APPLICATIONS.SCOPES,
						OAUTH_APPLICATIONS.EXPIRES_AT,
						OAUTH_APPLICATIONS.CREATED_AT
				)
				.from(OAUTH_APPLICATIONS)
				.where(condition)
				.fetchOne(this::toRegisteredClient);
	}

	private RegisteredClient toRegisteredClient(Record record) {
		final NamespaceClientType type = record.get(OAUTH_APPLICATIONS.TYPE, NamespaceClientType.class);

		return switch (type) {
			case AGENT -> fromAgent(type, record);
			case PIPELINE -> fromPipeline(type, record);
			case SERVICE_ACCOUNT -> fromServiceAccount(type, record);
		};
	}

	private RegisteredClient.Builder createRegisteredClient(NamespaceClientType type, Record record) {
		return RegisteredClient.withId(record.get(OAUTH_APPLICATIONS.ID, EntityId.class).serialize())
				.clientName(record.get(OAUTH_APPLICATIONS.NAME))
				.clientId(record.get(OAUTH_APPLICATIONS.CLIENT_ID))
				.clientIdIssuedAt(record.get(OAUTH_APPLICATIONS.CREATED_AT, Instant.class))
				.clientSecret(record.get(OAUTH_APPLICATIONS.CLIENT_SECRET))
				.clientSecretExpiresAt(record.get(OAUTH_APPLICATIONS.EXPIRES_AT, Instant.class))
				.authorizationGrantTypes(registerAuthorizationGrantTypes(type))
				.clientAuthenticationMethods(registerClientAuthenticationMethods(type))
				.tokenSettings(RegisteredClientSettings.createTokenSettings(properties, type).build())
				.scopes(registerScopes(record));
	}

	private RegisteredClient fromServiceAccount(NamespaceClientType type, Record record) {
		return createRegisteredClient(type, record)
				.clientSettings(RegisteredClientSettings.createClientSettings().build())
				.redirectUris(Set::clear)
				.postLogoutRedirectUris(Set::clear)
				.build();
	}

	private RegisteredClient fromAgent(NamespaceClientType type, Record record) {
		final ClientSettings clientSettings = RegisteredClientSettings.createClientSettings()
				.requireAuthorizationConsent(true)
				.build();

		return createRegisteredClient(type, record)
				.clientSettings(clientSettings)
				.redirectUri("http://localhost/callback")
				.redirectUri("http://127.0.0.1/callback")
				.build();
	}

	private RegisteredClient fromPipeline(NamespaceClientType type, Record record) {
		return createRegisteredClient(type, record)
				.clientSettings(RegisteredClientSettings.createClientSettings().build())
				.redirectUris(Set::clear)
				.postLogoutRedirectUris(Set::clear)
				.build();
	}

	private static Consumer<Set<String>> registerScopes(Record record) {
		final OAuthScopes scopes = OAuthScopes.parse(record.get(OAUTH_APPLICATIONS.SCOPES));

		return collection -> scopes.toAuthorities().forEach(authority -> collection.add(authority.getAuthority()));
	}

	private static Consumer<Set<AuthorizationGrantType>> registerAuthorizationGrantTypes(NamespaceClientType type) {
		final Set<AuthorizationGrantType> grantTypes = switch (type) {
			case AGENT -> Set.of(AuthorizationGrantType.AUTHORIZATION_CODE);
			case PIPELINE -> Set.of(AuthorizationGrantType.TOKEN_EXCHANGE);
			case SERVICE_ACCOUNT -> Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS);
		};

		return collection -> collection.addAll(grantTypes);
	}

	private static Consumer<Set<ClientAuthenticationMethod>> registerClientAuthenticationMethods(NamespaceClientType type) {
		final Set<ClientAuthenticationMethod> authenticationMethods;

		if (type == NamespaceClientType.AGENT) {
			authenticationMethods = Set.of(ClientAuthenticationMethod.NONE);
		} else {
			authenticationMethods = Set.of(
					ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
					ClientAuthenticationMethod.CLIENT_SECRET_POST
			);
		}

		return collection -> collection.addAll(authenticationMethods);
	}
}
