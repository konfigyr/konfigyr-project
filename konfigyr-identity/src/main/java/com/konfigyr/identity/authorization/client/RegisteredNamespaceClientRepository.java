package com.konfigyr.identity.authorization.client;

import com.konfigyr.identity.authorization.AuthorizationProperties;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

public class RegisteredNamespaceClientRepository extends AbstractRegisteredClientRepository {

	private static final Set<AuthorizationGrantType> SUPPORTED_GRANT_TYPES = Set.of(
			AuthorizationGrantType.CLIENT_CREDENTIALS, AuthorizationGrantType.REFRESH_TOKEN
	);

	private final DSLContext context;

	public RegisteredNamespaceClientRepository(AuthorizationProperties properties, DSLContext context) {
		super(properties);
		this.context = context;
	}

	@Override
	public RegisteredClient findById(String id) {
		return lookup(id);
	}

	@Override
	public RegisteredClient findByClientId(String clientId) {
		return lookup(clientId);
	}

	@NonNull
	@Override
	protected Collection<AuthorizationGrantType> getAuthorizationGrantTypes() {
		return SUPPORTED_GRANT_TYPES;
	}

	private RegisteredClient lookup(String id) {
		if (StringUtils.isBlank(id) || !id.startsWith("kfg-")) {
			return null;
		}

		return context.select(
						OAUTH_APPLICATIONS.NAME,
						OAUTH_APPLICATIONS.CLIENT_ID,
						OAUTH_APPLICATIONS.CLIENT_SECRET,
						OAUTH_APPLICATIONS.SCOPES,
						OAUTH_APPLICATIONS.EXPIRES_AT,
						OAUTH_APPLICATIONS.CREATED_AT
				)
				.from(OAUTH_APPLICATIONS)
				.where(OAUTH_APPLICATIONS.CLIENT_ID.eq(id))
				.fetchOne(this::toRegisteredClient);
	}

	private RegisteredClient toRegisteredClient(Record record) {
		return createRegisteredClient(record.get(OAUTH_APPLICATIONS.CLIENT_ID))
				.clientName(record.get(OAUTH_APPLICATIONS.NAME))
				.clientId(record.get(OAUTH_APPLICATIONS.CLIENT_ID))
				.clientIdIssuedAt(record.get(OAUTH_APPLICATIONS.CREATED_AT, Instant.class))
				.clientSecret(record.get(OAUTH_APPLICATIONS.CLIENT_SECRET))
				.clientSecretExpiresAt(record.get(OAUTH_APPLICATIONS.EXPIRES_AT, Instant.class))
				.scope(record.get(OAUTH_APPLICATIONS.SCOPES))
				.tokenSettings(createTokenSettings().build())
				.clientSettings(createClientSettings().build())
				.redirectUris(Set::clear)
				.postLogoutRedirectUris(Set::clear)
				.build();
	}
}
