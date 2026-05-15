package com.konfigyr.identity.authorization.client;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authorization.AuthorizationProperties;
import com.konfigyr.security.OAuthScopes;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import static com.konfigyr.data.tables.OauthApplications.OAUTH_APPLICATIONS;

public class RegisteredNamespaceClientRepository extends AbstractRegisteredClientRepository {

	private static final Set<AuthorizationGrantType> SUPPORTED_GRANT_TYPES = Set.of(
			AuthorizationGrantType.CLIENT_CREDENTIALS
	);

	private final DSLContext context;

	public RegisteredNamespaceClientRepository(AuthorizationProperties properties, DSLContext context) {
		super(properties);
		this.context = context;
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
		if (StringUtils.isBlank(clientId) || !clientId.startsWith("kfg-")) {
			return null;
		}
		return lookup(OAUTH_APPLICATIONS.CLIENT_ID.eq(clientId));
	}

	@NonNull
	@Override
	protected Collection<AuthorizationGrantType> getAuthorizationGrantTypes() {
		return SUPPORTED_GRANT_TYPES;
	}

	private RegisteredClient lookup(Condition condition) {
		return context.select(
						OAUTH_APPLICATIONS.ID,
						OAUTH_APPLICATIONS.NAME,
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
		final Collection<? extends GrantedAuthority> authorities = OAuthScopes.parse(record.get(OAUTH_APPLICATIONS.SCOPES))
				.toAuthorities();

		return createRegisteredClient(record.get(OAUTH_APPLICATIONS.ID, EntityId.class).serialize())
				.clientName(record.get(OAUTH_APPLICATIONS.NAME))
				.clientId(record.get(OAUTH_APPLICATIONS.CLIENT_ID))
				.clientIdIssuedAt(record.get(OAUTH_APPLICATIONS.CREATED_AT, Instant.class))
				.clientSecret(record.get(OAUTH_APPLICATIONS.CLIENT_SECRET))
				.clientSecretExpiresAt(record.get(OAUTH_APPLICATIONS.EXPIRES_AT, Instant.class))
				.scopes(it -> authorities.stream().map(GrantedAuthority::getAuthority).forEach(it::add))
				.tokenSettings(createTokenSettings().build())
				.clientSettings(createClientSettings().build())
				.redirectUris(Set::clear)
				.postLogoutRedirectUris(Set::clear)
				.build();
	}
}
