package com.konfigyr.identity.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.data.converter.EncryptionConverter;
import com.konfigyr.data.converter.JsonConverter;
import com.konfigyr.data.converter.MessageDigestConverter;
import com.konfigyr.io.ByteArray;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.konfigyr.data.tables.OauthAuthorizations.OAUTH_AUTHORIZATIONS;
import static com.konfigyr.data.tables.OauthAuthorizationsConsents.OAUTH_AUTHORIZATIONS_CONSENTS;

/**
 * Implementation of an {@link AuthorizationService} that uses jOOQ for {@link OAuth2Authorization} persistence.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see OAuth2AuthorizationService
 * @see OAuth2Authorization
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class DefaultAuthorizationService implements AuthorizationService {

	static OAuth2TokenType AUTHORIZATION_STATE_TOKEN_TYPE = new OAuth2TokenType(OAuth2ParameterNames.STATE);
	static OAuth2TokenType AUTHORIZATION_CODE_TOKEN_TYPE = new OAuth2TokenType(OAuth2ParameterNames.CODE);
	static OAuth2TokenType OIDC_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

	private final DSLContext context;
	private final ApplicationEventPublisher publisher;

	/* jOOQ Converters */
	private final EncryptionConverter encryptionConverter;
	private final Converter<String, Map> attributeConverter;
	private final Converter<ByteArray, ByteArray> hashingConverter;
	private final Converter<String, RegisteredClient> registeredClientConverter;
	private final Converter<OffsetDateTime, Instant> instantConverter = Converter.of(
			OffsetDateTime.class,
			Instant.class,
			OffsetDateTime::toInstant,
			instant -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
	);
	private final Converter<String, AuthorizationGrantType> grantTypeConverter = Converter.of(
			String.class,
			AuthorizationGrantType.class,
			AuthorizationGrantType::new,
			AuthorizationGrantType::getValue
	);
	private final Converter<String, Set> scopesConverter = Converter.of(
			String.class,
			Set.class,
			StringUtils::commaDelimitedListToSet,
			StringUtils::collectionToCommaDelimitedString
	);

	public DefaultAuthorizationService(
			DSLContext context, ApplicationEventPublisher publisher, ObjectMapper mapper,
			KeysetOperations operations, RegisteredClientRepository repository
	) {
		this.context = context;
		this.publisher = publisher;
		this.attributeConverter = JsonConverter.create(mapper, Map.class);
		this.hashingConverter = MessageDigestConverter.create("BLAKE2s-256", BouncyCastleProvider.PROVIDER_NAME);
		this.encryptionConverter = EncryptionConverter.create(operations);
		this.registeredClientConverter = Converter.fromNullable(String.class, RegisteredClient.class, repository::findByClientId);
	}

	@Override
	@Transactional(label = "authorization-service.save-authorization")
	public void save(OAuth2Authorization authorization) {
		Assert.notNull(authorization, "OAuth2 Authorization cannot be null");

		log.debug(
				"Attempting to save OAuth2 authorization: [id={}, client={}, grant_type={}, complete={}]",
				authorization.getId(),
				authorization.getRegisteredClientId(),
				authorization.getAuthorizationGrantType().getValue(),
				authorization.getAccessToken() != null
		);

		final SettableRecord record = SettableRecord.of(context, OAUTH_AUTHORIZATIONS)
				.set(OAUTH_AUTHORIZATIONS.REGISTERED_CLIENT_ID, authorization.getRegisteredClientId())
				.set(OAUTH_AUTHORIZATIONS.PRINCIPAL_NAME, authorization.getPrincipalName())
				.set(OAUTH_AUTHORIZATIONS.AUTHORIZATION_GRANT_TYPE, authorization.getAuthorizationGrantType(), grantTypeConverter)
				.set(OAUTH_AUTHORIZATIONS.AUTHORIZED_SCOPES, authorization.getAuthorizedScopes(), scopesConverter)
				.set(OAUTH_AUTHORIZATIONS.ATTRIBUTES, authorization.getAttributes(), attributeConverter)
				.set(OAUTH_AUTHORIZATIONS.STATE, (String) authorization.getAttribute(OAuth2ParameterNames.STATE))
				.with(applyAuthorizationCode(authorization))
				.with(applyAccessToken(authorization))
				.with(applyRefreshToken(authorization))
				.with(applyIdToken(authorization));

		context.insertInto(OAUTH_AUTHORIZATIONS)
				.set(OAUTH_AUTHORIZATIONS.ID, authorization.getId())
				.set(record.get())
				.onDuplicateKeyUpdate()
				.set(record.get())
				.execute();

		log.debug(
				"OAuth2 authorization has been saved: [id={}, client={}, grant_type={}, complete={}]",
				authorization.getId(),
				authorization.getRegisteredClientId(),
				authorization.getAuthorizationGrantType().getValue(),
				authorization.getAccessToken() != null
		);

		publisher.publishEvent(new AuthorizationEvent.Stored(authorization));
	}

	@Override
	@Transactional(label = "authorization-service.save-consent")
	public void save(OAuth2AuthorizationConsent consent) {
		Assert.notNull(consent, "OAuth2 Authorization consent cannot be null");

		log.debug(
				"Attempting to save OAuth2 authorization consent: [client={}, principal={}, scopes={}]",
				consent.getRegisteredClientId(),
				consent.getPrincipalName(),
				consent.getScopes()
		);

		final SettableRecord record = SettableRecord.of(context, OAUTH_AUTHORIZATIONS_CONSENTS)
				.set(OAUTH_AUTHORIZATIONS_CONSENTS.AUTHORITIES, consent.getScopes(), scopesConverter)
				.set(OAUTH_AUTHORIZATIONS_CONSENTS.TIMESTAMP, OffsetDateTime.now(ZoneOffset.UTC));

		context.insertInto(OAUTH_AUTHORIZATIONS_CONSENTS)
				.set(OAUTH_AUTHORIZATIONS_CONSENTS.REGISTERED_CLIENT_ID, consent.getRegisteredClientId())
				.set(OAUTH_AUTHORIZATIONS_CONSENTS.PRINCIPAL_NAME, consent.getPrincipalName())
				.set(record.get())
				.onDuplicateKeyUpdate()
				.set(record.get())
				.execute();

		log.debug(
				"OAuth2 authorization consent has been granted: [client={}, principal={}, scopes={}]",
				consent.getRegisteredClientId(),
				consent.getPrincipalName(),
				consent.getScopes()
		);

		publisher.publishEvent(new AuthorizationConsentEvent.Granted(consent));
	}

	@Override
	@Transactional(readOnly = true, label = "authorization-service.find-authorization-by-id")
	public OAuth2Authorization findById(String id) {
		Assert.hasText(id, "OAuth2 Authorization identifier cannot be empty");

		log.debug("Looking up OAuth2 Authorization with identifier: {}", id);

		return lookupAuthorization(OAUTH_AUTHORIZATIONS.ID.eq(id));
	}

	@Override
	@Transactional(readOnly = true, label = "authorization-service.find-authorization-by-token")
	public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType type) {
		Assert.hasText(token, "OAuth2 Authorization token value cannot be empty");

		final Condition condition;

		if (type == null) {
			final ByteArray hash = hashingConverter.to(ByteArray.fromString(token));

			condition = DSL.or(
					OAUTH_AUTHORIZATIONS.STATE.eq(token),
					OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_VALUE.eq(token),
					OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_HASH.eq(hash),
					OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_HASH.eq(hash),
					OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_HASH.eq(hash)
			);
		} else if (AUTHORIZATION_STATE_TOKEN_TYPE.equals(type)) {
			condition = OAUTH_AUTHORIZATIONS.STATE.eq(token);
		} else if (AUTHORIZATION_CODE_TOKEN_TYPE.equals(type)) {
			condition = OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_VALUE.eq(token);
		} else if (OAuth2TokenType.ACCESS_TOKEN.equals(type)) {
			condition = OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_HASH.eq(hashingConverter.to(ByteArray.fromString(token)));
		} else if (OIDC_TOKEN_TYPE.equals(type)) {
			condition = OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_HASH.eq(hashingConverter.to(ByteArray.fromString(token)));
		} else if (OAuth2TokenType.REFRESH_TOKEN.equals(type)) {
			condition = OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_HASH.eq(hashingConverter.to(ByteArray.fromString(token)));
		} else {
			return null;
		}

		log.debug("Looking up OAuth2 Authorization for token type: {}", type == null ? null : type.getValue());

		return lookupAuthorization(condition);
	}

	@Override
	@Transactional(readOnly = true, label = "authorization-service.find-consent-by-id")
	public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
		Assert.hasText(registeredClientId, "Registered client identifier cannot be empty");
		Assert.hasText(principalName, "Principal name cannot be empty");

		log.debug("Looking up OAuth2 Authorization consent for: [client={}, principal={}]",
				registeredClientId, principalName);

		return context.select(
					OAUTH_AUTHORIZATIONS_CONSENTS.REGISTERED_CLIENT_ID,
					OAUTH_AUTHORIZATIONS_CONSENTS.PRINCIPAL_NAME,
					OAUTH_AUTHORIZATIONS_CONSENTS.AUTHORITIES
				)
				.from(OAUTH_AUTHORIZATIONS_CONSENTS)
				.where(DSL.and(
						OAUTH_AUTHORIZATIONS_CONSENTS.REGISTERED_CLIENT_ID.eq(registeredClientId),
						OAUTH_AUTHORIZATIONS_CONSENTS.PRINCIPAL_NAME.eq(principalName)
				))
				.fetchOne(this::createAuthorizationConsent);
	}

	@Override
	@Transactional(label = "authorization-service.remove-authorization")
	public void remove(OAuth2Authorization authorization) {
		Assert.notNull(authorization, "OAuth2 Authorization cannot be null");

		long count = context.deleteFrom(OAUTH_AUTHORIZATIONS)
				.where(OAUTH_AUTHORIZATIONS.ID.eq(authorization.getId()))
				.execute();

		if (count > 0) {
			log.debug(
					"OAuth2 authorization has been removed: [id={}, client={}, grant_type={}, count={}]",
					authorization.getId(),
					authorization.getRegisteredClientId(),
					authorization.getAuthorizationGrantType().getValue(),
					count
			);

			publisher.publishEvent(new AuthorizationEvent.Revoked(authorization));
		} else {
			log.debug(
					"OAuth2 authorization could not be removed as it does not exist: [id={}, client={}, grant_type={}]",
					authorization.getId(),
					authorization.getRegisteredClientId(),
					authorization.getAuthorizationGrantType().getValue()
			);
		}
	}

	@Override
	@Transactional(label = "authorization-service.remove-consent")
	public void remove(OAuth2AuthorizationConsent consent) {
		Assert.notNull(consent, "OAuth2 Authorization consent cannot be null");

		long count = context.deleteFrom(OAUTH_AUTHORIZATIONS_CONSENTS)
				.where(DSL.and(
						OAUTH_AUTHORIZATIONS_CONSENTS.REGISTERED_CLIENT_ID.eq(consent.getRegisteredClientId()),
						OAUTH_AUTHORIZATIONS_CONSENTS.PRINCIPAL_NAME.eq(consent.getPrincipalName())
				))
				.execute();

		if (count > 0) {
			log.debug(
					"OAuth2 authorization consent has been removed: [client={}, principal={}, count={}]",
					consent.getRegisteredClientId(),
					consent.getPrincipalName(),
					count
			);

			publisher.publishEvent(new AuthorizationConsentEvent.Revoked(consent));
		} else {
			log.debug(
					"OAuth2 authorization consent could not be removed as it does not exist: [client={}, principal={}]",
					consent.getRegisteredClientId(),
					consent.getPrincipalName()
			);
		}
	}

	@Scheduled(cron = "${konfigyr.authorization.cleanup.cron:0 * * * * *}")
	@Transactional(label = "authorization-service.cleanup-expired-authorizations")
	void cleanup() {
		final OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

		log.debug("Running OAuth2 authorization cleanup operation with timestamp: {}", timestamp);

		final List<String> authorizations = context.deleteFrom(OAUTH_AUTHORIZATIONS)
				.where(DSL.greatest(
						OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_EXPIRES_AT,
						OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_EXPIRES_AT,
						OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_EXPIRES_AT,
						OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_EXPIRES_AT
				).le(timestamp))
				.returning(OAUTH_AUTHORIZATIONS.ID)
				.fetch(OAUTH_AUTHORIZATIONS.ID);

		if (CollectionUtils.isEmpty(authorizations)) {
			return;
		}

		log.info("Expired OAuth2 authorization have been removed: [count={}]", authorizations.size());

		authorizations.forEach(authorization -> publisher.publishEvent(
				new AuthorizationEvent.Revoked(authorization))
		);
	}

	@Async
	@DomainEventHandler(name = "authorization-consent-revoked", namespace = "authorization")
	@TransactionalEventListener(classes = AuthorizationConsentEvent.Revoked.class)
	void onConsentRevoked(@NonNull AuthorizationConsentEvent.Revoked event) {
		final OAuth2AuthorizationConsent consent = event.consent();
		remove(consent.getPrincipalName(), consent.getRegisteredClientId());
	}

	void remove(String registeredClientId, String principalName) {
		Assert.hasText(principalName, "Principal name cannot be null");
		Assert.hasText(registeredClientId, "Client registration identifier cannot be null");

		log.info("Revoking OAuth2 Authorizations for: [client={}, principal={}]", registeredClientId, principalName);

		final List<String> authorizations = context.deleteFrom(OAUTH_AUTHORIZATIONS)
				.where(DSL.and(
						OAUTH_AUTHORIZATIONS.REGISTERED_CLIENT_ID.eq(registeredClientId),
						OAUTH_AUTHORIZATIONS.PRINCIPAL_NAME.eq(principalName)
				))
				.returning(OAUTH_AUTHORIZATIONS.ID)
				.fetch(OAUTH_AUTHORIZATIONS.ID);

		if (CollectionUtils.isEmpty(authorizations)) {
			return;
		}

		log.info("Removed OAuth authorizations as OAuth consent was revoked: [client={}, principal={}, count={}]",
				registeredClientId, principalName, authorizations.size());

		authorizations.forEach(authorization -> publisher.publishEvent(
				new AuthorizationEvent.Revoked(authorization))
		);
	}

	@Nullable
	private OAuth2Authorization lookupAuthorization(@NonNull Condition condition) {
		return context.select(
					OAUTH_AUTHORIZATIONS.ID,
					OAUTH_AUTHORIZATIONS.REGISTERED_CLIENT_ID,
					OAUTH_AUTHORIZATIONS.PRINCIPAL_NAME,
					OAUTH_AUTHORIZATIONS.AUTHORIZATION_GRANT_TYPE,
					OAUTH_AUTHORIZATIONS.AUTHORIZED_SCOPES,
					OAUTH_AUTHORIZATIONS.ATTRIBUTES,
					OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_VALUE,
					OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_ISSUED_AT,
					OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_EXPIRES_AT,
					OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_METADATA,
					OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_VALUE,
					OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_SCOPES,
					OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_ISSUED_AT,
					OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_EXPIRES_AT,
					OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_METADATA,
					OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_VALUE,
					OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_ISSUED_AT,
					OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_EXPIRES_AT,
					OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_METADATA,
					OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_VALUE,
					OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_ISSUED_AT,
					OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_EXPIRES_AT,
					OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_METADATA
				)
				.from(OAUTH_AUTHORIZATIONS)
				.where(condition)
				.fetchOne(this::createAuthorization);
	}

	@NonNull
	private UnaryOperator<SettableRecord> applyAuthorizationCode(@NonNull OAuth2Authorization authorization) {
		return apply(
				() -> authorization.getToken(OAuth2AuthorizationCode.class),
				(record, token) -> record
						.set(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_VALUE, token.getToken().getTokenValue())
						.set(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_ISSUED_AT, token.getToken().getIssuedAt(), instantConverter)
						.set(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_EXPIRES_AT, token.getToken().getExpiresAt(), instantConverter)
						.set(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_METADATA, token.getMetadata(), attributeConverter)
		);
	}

	@NonNull
	private UnaryOperator<SettableRecord> applyAccessToken(@NonNull OAuth2Authorization authorization) {
		final ByteArray context = ByteArray.fromString(authorization.getId());

		return apply(
				authorization::getAccessToken,
				(record, token) -> {
					final ByteArray value = ByteArray.fromString(token.getToken().getTokenValue());

					return record.set(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_VALUE, value, encryptionConverter.with(context))
							.set(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_HASH, value, hashingConverter)
							.set(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_ISSUED_AT, token.getToken().getIssuedAt(), instantConverter)
							.set(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_EXPIRES_AT, token.getToken().getExpiresAt(), instantConverter)
							.set(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_SCOPES, token.getToken().getScopes(), scopesConverter)
							.set(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_METADATA, token.getMetadata(), attributeConverter);
				}
		);
	}

	@NonNull
	private UnaryOperator<SettableRecord> applyRefreshToken(@NonNull OAuth2Authorization authorization) {
		final ByteArray context = ByteArray.fromString(authorization.getId());

		return apply(
				authorization::getRefreshToken,
				(record, token) -> {
					final ByteArray value = ByteArray.fromString(token.getToken().getTokenValue());

					return record.set(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_VALUE, value, encryptionConverter.with(context))
							.set(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_HASH, value, hashingConverter)
							.set(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_ISSUED_AT, token.getToken().getIssuedAt(), instantConverter)
							.set(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_EXPIRES_AT, token.getToken().getExpiresAt(), instantConverter)
							.set(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_METADATA, token.getMetadata(), attributeConverter);
				}
		);
	}

	@NonNull
	private UnaryOperator<SettableRecord> applyIdToken(@NonNull OAuth2Authorization authorization) {
		final ByteArray context = ByteArray.fromString(authorization.getId());

		return apply(
				() -> authorization.getToken(OidcIdToken.class),
				(record, token) -> {
					final ByteArray value = ByteArray.fromString(token.getToken().getTokenValue());

					return record.set(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_VALUE, value, encryptionConverter.with(context))
							.set(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_HASH, value, hashingConverter)
							.set(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_ISSUED_AT, token.getToken().getIssuedAt(), instantConverter)
							.set(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_EXPIRES_AT, token.getToken().getExpiresAt(), instantConverter)
							.set(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_METADATA, token.getMetadata(), attributeConverter);
				}
		);
	}

	@SuppressWarnings("unchecked")
	private OAuth2Authorization createAuthorization(@NonNull Record record) {
		final String id = record.get(OAUTH_AUTHORIZATIONS.ID);
		Assert.hasText(id, "OAuth authorization must have a unique identifier");

		final RegisteredClient client = record.get(OAUTH_AUTHORIZATIONS.REGISTERED_CLIENT_ID, registeredClientConverter);
		Assert.notNull(client, () -> "Registered client not found for authorization: " + id);

		final OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(client)
				.id(id)
				.principalName(record.get(OAUTH_AUTHORIZATIONS.PRINCIPAL_NAME))
				.authorizationGrantType(record.get(OAUTH_AUTHORIZATIONS.AUTHORIZATION_GRANT_TYPE, grantTypeConverter))
				.authorizedScopes(record.get(OAUTH_AUTHORIZATIONS.AUTHORIZED_SCOPES, scopesConverter))
				.attributes(consumeAttributes(record, OAUTH_AUTHORIZATIONS.ATTRIBUTES));

		if (record.get(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_VALUE) != null) {
			builder.token(
					new OAuth2AuthorizationCode(
							record.get(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_VALUE),
							record.get(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_ISSUED_AT, instantConverter),
							record.get(OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_EXPIRES_AT, instantConverter)
					),
					consumeAttributes(record, OAUTH_AUTHORIZATIONS.AUTHORIZATION_CODE_METADATA)
			);
		}

		if (record.get(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_VALUE) != null) {
			builder.token(
					new OAuth2AccessToken(
							OAuth2AccessToken.TokenType.BEARER,
							createTokenValue(record, OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_VALUE),
							record.get(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_ISSUED_AT, instantConverter),
							record.get(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_EXPIRES_AT, instantConverter),
							record.get(OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_SCOPES, scopesConverter)
					),
					consumeAttributes(record, OAUTH_AUTHORIZATIONS.ACCESS_TOKEN_METADATA)
			);
		}

		if (record.get(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_VALUE) != null) {
			final Map<String, Object> metadata = createAttributes(record, OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_METADATA);

			builder.token(
					new OidcIdToken(
							createTokenValue(record, OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_VALUE),
							record.get(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_ISSUED_AT, instantConverter),
							record.get(OAUTH_AUTHORIZATIONS.OIDC_ID_TOKEN_EXPIRES_AT, instantConverter),
							(Map<String, Object>) metadata.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME)
					),
					attrs -> attrs.putAll(metadata)
			);
		}

		if (record.get(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_VALUE) != null) {
			builder.token(
					new OAuth2RefreshToken(
							createTokenValue(record, OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_VALUE),
							record.get(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_ISSUED_AT, instantConverter),
							record.get(OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_EXPIRES_AT, instantConverter)
					),
					consumeAttributes(record, OAUTH_AUTHORIZATIONS.REFRESH_TOKEN_METADATA)
			);
		}

		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private OAuth2AuthorizationConsent createAuthorizationConsent(@NonNull Record record) {
		final OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent.withId(
				record.get(OAUTH_AUTHORIZATIONS_CONSENTS.REGISTERED_CLIENT_ID),
				record.get(OAUTH_AUTHORIZATIONS_CONSENTS.PRINCIPAL_NAME)
		);

		final Set<String> scopes = record.get(OAUTH_AUTHORIZATIONS_CONSENTS.AUTHORITIES, scopesConverter);

		if (!CollectionUtils.isEmpty(scopes)) {
			scopes.forEach(builder::scope);
		}

		return builder.build();
	}

	private Consumer<Map<String, Object>> consumeAttributes(@NonNull Record record, @NonNull Field<String> field) {
		return attributes -> attributes.putAll(createAttributes(record, field));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> createAttributes(@NonNull Record record, @NonNull Field<String> field) {
		final Map<String, Object> value = record.get(field, attributeConverter);
		return value == null ? Collections.emptyMap() : value;
	}

	private String createTokenValue(@NonNull Record record, @NonNull Field<ByteArray> field) {
		final String id = record.get(OAUTH_AUTHORIZATIONS.ID);
		Assert.hasText(id, "Authorization identifier can not be null");

		final ByteArray value = record.get(field, encryptionConverter.with(id));
		return value == null ? null : new String(value.array(), StandardCharsets.UTF_8);
	}

	@NonNull
	private static <T extends OAuth2Token> UnaryOperator<SettableRecord> apply(
			@NonNull Supplier<OAuth2Authorization.Token<T>> supplier,
			@NonNull BiFunction<SettableRecord, OAuth2Authorization.Token<T>, SettableRecord> operator
	) {
		final OAuth2Authorization.Token<T> token = supplier.get();

		if (token == null || token.getToken() == null) {
			return UnaryOperator.identity();
		}

		return record -> operator.apply(record, token);
	}

}
