package com.konfigyr.identity.authorization.client;

import com.konfigyr.data.converter.JsonbConverter;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.security.NamespaceApplicationSettings;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import com.konfigyr.security.OAuthScopes;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

public class RegisteredNamespaceClientRepository implements RegisteredClientRepository {

	private static final List<String> DEFAULT_AGENT_REDIRECT_URIS = List.of(
			"http://localhost/callback",
			"http://127.0.0.1/callback"
	);

	private final DSLContext context;
	private final AuthorizationProperties properties;
	private final Converter<JSONB, NamespaceApplicationSettings> settingsConverter;

	public RegisteredNamespaceClientRepository(AuthorizationProperties properties, DSLContext context, JsonMapper jsonMapper) {
		this.properties = properties;
		this.context = context;
		this.settingsConverter = JsonbConverter.create(jsonMapper, NamespaceApplicationSettings.class);
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
						OAUTH_APPLICATIONS.CLIENT_ID,
						OAUTH_APPLICATIONS.CLIENT_SECRET,
						OAUTH_APPLICATIONS.SCOPES,
						OAUTH_APPLICATIONS.EXPIRES_AT,
						OAUTH_APPLICATIONS.CREATED_AT,
						OAUTH_APPLICATIONS.SETTINGS
				)
				.from(OAUTH_APPLICATIONS)
				.where(condition)
				.fetchOne(this::toRegisteredClient);
	}

	private RegisteredClient toRegisteredClient(Record record) {
		final NamespaceClientId clientId = NamespaceClientId.parse(record.get(OAUTH_APPLICATIONS.CLIENT_ID));

		return switch (clientId.type()) {
			case AGENT -> fromAgent(clientId, record);
			case WORKLOAD -> fromWorkload(clientId, record);
			case SERVICE_ACCOUNT -> fromServiceAccount(clientId, record);
		};
	}

	private RegisteredClient.Builder createRegisteredClient(NamespaceClientId clientId, Record record) {
		return RegisteredClient.withId(record.get(OAUTH_APPLICATIONS.ID, EntityId.class).serialize())
				.clientName(record.get(OAUTH_APPLICATIONS.NAME))
				.clientId(clientId.get())
				.clientIdIssuedAt(record.get(OAUTH_APPLICATIONS.CREATED_AT, Instant.class))
				.authorizationGrantTypes(registerAuthorizationGrantTypes(clientId.type()))
				.clientAuthenticationMethods(registerClientAuthenticationMethods(clientId.type()))
				.tokenSettings(RegisteredClientSettings.createTokenSettings(properties, clientId.type()).build())
				.scopes(registerScopes(record));
	}

	private RegisteredClient fromServiceAccount(NamespaceClientId clientId, Record record) {
		return createRegisteredClient(clientId, record)
				.clientSecret(record.get(OAUTH_APPLICATIONS.CLIENT_SECRET))
				.clientSecretExpiresAt(record.get(OAUTH_APPLICATIONS.EXPIRES_AT, Instant.class))
				.clientSettings(RegisteredClientSettings.createClientSettings(clientId).build())
				.redirectUris(Set::clear)
				.postLogoutRedirectUris(Set::clear)
				.build();
	}

	private RegisteredClient fromAgent(NamespaceClientId clientId, Record record) {
		final ClientSettings clientSettings = RegisteredClientSettings.createClientSettings(clientId)
				.requireAuthorizationConsent(true)
				.build();

		final List<String> redirectUris = resolveAgentRedirectUris(
				record.get(OAUTH_APPLICATIONS.SETTINGS, settingsConverter)
		);

		final RegisteredClient.Builder builder = createRegisteredClient(clientId, record)
				.clientSettings(clientSettings);

		redirectUris.forEach(builder::redirectUri);

		return builder.build();
	}

	private RegisteredClient fromWorkload(NamespaceClientId clientId, Record record) {
		final ClientSettings.Builder clientSettingsBuilder = RegisteredClientSettings.createClientSettings(clientId);

		applyWorkloadSettings(clientSettingsBuilder,
				record.get(OAUTH_APPLICATIONS.SETTINGS, settingsConverter)
		);

		return createRegisteredClient(clientId, record)
				.clientSettings(clientSettingsBuilder.build())
				.redirectUris(Set::clear)
				.postLogoutRedirectUris(Set::clear)
				.build();
	}

	private static List<String> resolveAgentRedirectUris(NamespaceApplicationSettings settings) {
		if (settings instanceof NamespaceApplicationSettings.AgentSettings(List<String> redirectUris)) {
			return redirectUris;
		}
		return DEFAULT_AGENT_REDIRECT_URIS;
	}

	private static void applyWorkloadSettings(ClientSettings.Builder builder, NamespaceApplicationSettings settings) {
		if (settings instanceof NamespaceApplicationSettings.WorkloadSettings(String issuerUri, String subjectPattern)) {
			RegisteredClientSettings.mapper().from(issuerUri)
					.whenHasText()
					.to(uri -> builder.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, uri));

			RegisteredClientSettings.mapper().from(subjectPattern)
					.whenHasText()
					.to(pattern -> builder.setting(NamespaceClientSettingNames.WORKLOAD_SUBJECT_PATTERN, pattern));
		}
	}

	private static Consumer<Set<String>> registerScopes(Record record) {
		final OAuthScopes scopes = OAuthScopes.parse(record.get(OAUTH_APPLICATIONS.SCOPES));

		return collection -> scopes.toAuthorities().forEach(authority -> collection.add(authority.getAuthority()));
	}

	private static Consumer<Set<AuthorizationGrantType>> registerAuthorizationGrantTypes(NamespaceClientType type) {
		final Set<AuthorizationGrantType> grantTypes = switch (type) {
			case AGENT -> Set.of(AuthorizationGrantType.AUTHORIZATION_CODE);
			case WORKLOAD -> Set.of(AuthorizationGrantType.TOKEN_EXCHANGE);
			case SERVICE_ACCOUNT -> Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS);
		};

		return collection -> collection.addAll(grantTypes);
	}

	private static Consumer<Set<ClientAuthenticationMethod>> registerClientAuthenticationMethods(NamespaceClientType type) {
		final Set<ClientAuthenticationMethod> authenticationMethods = switch (type) {
			case AGENT, WORKLOAD -> Set.of(ClientAuthenticationMethod.NONE);
			case SERVICE_ACCOUNT -> Set.of(
					ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
					ClientAuthenticationMethod.CLIENT_SECRET_POST
			);
		};

		return collection -> collection.addAll(authenticationMethods);
	}
}
