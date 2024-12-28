package com.konfigyr.identity.authentication.oauth;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.data.SettableRecord;
import com.konfigyr.data.converter.EncryptionConverter;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.KonfigyrIdentityKeysets;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.io.ByteArray;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.function.UnaryOperator;

import static com.konfigyr.data.tables.AccountAccessTokens.ACCOUNT_ACCESS_TOKENS;

/**
 * Implementation of the {@link OAuth2AuthorizedClientService} that would retrieve and
 * store OAuth Access Tokens in the {@link com.konfigyr.data.tables.AccountAccessTokens} database table.
 * <p>
 * Considering that the access and refresh token values are sensitive user information, they would be
 * encrypted using the {@link KeysetOperations}, based on {@link KonfigyrIdentityKeysets#AUTHORIZED_CLIENTS}
 * key before they are stored to the database.
 *
 * @author Vladimir Spasic
 * @since 1.0.
 **/
@Slf4j
@SuppressWarnings("rawtypes")
@RequiredArgsConstructor
public class AuthorizedClientService implements OAuth2AuthorizedClientService {

	private static final Marker MARKER = MarkerFactory.getMarker("OAUTH_CLIENT_SERVICE");

	private final DSLContext context;
	private final ClientRegistrationRepository clientRegistrationRepository;

	private final Converter<ByteArray, ByteArray> encryptionConverter;
	private final Converter<String, Set> scopesConverter = Converter.ofNullable(
			String.class,
			Set.class,
			StringUtils::commaDelimitedListToSet,
			StringUtils::collectionToCommaDelimitedString
	);
	private final Converter<OffsetDateTime, Instant> instantConverter = Converter.ofNullable(
			OffsetDateTime.class,
			Instant.class,
			OffsetDateTime::toInstant,
			instant -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
	);

	public AuthorizedClientService(DSLContext context, KeysetOperations operations, ClientRegistrationRepository repository) {
		this.context = context;
		this.clientRegistrationRepository = repository;
		this.encryptionConverter = EncryptionConverter.create(operations);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, label = "oauth-client-service-load")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		final EntityId account;

		try {
			account = EntityId.from(principalName);
		} catch (IllegalArgumentException ex) {
			log.debug("Attempted to load an authorized client for invalid principal name: {}", principalName);
			return null;
		}

		if (log.isDebugEnabled()) {
			log.debug("Looking up OAuth access token for [client={}, account={}]", clientRegistrationId, account);
		}

		final ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);

		if (registration == null) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
		}

		return (T) context.select(
						ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_VALUE,
						ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_ISSUED_AT,
						ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_EXPIRES_AT,
						ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_SCOPES,
						ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE,
						ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_ISSUED_AT,
						ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_EXPIRES_AT
				)
				.from(ACCOUNT_ACCESS_TOKENS)
				.where(DSL.and(
						ACCOUNT_ACCESS_TOKENS.CLIENT_REGISTRATION_ID.eq(clientRegistrationId),
						ACCOUNT_ACCESS_TOKENS.ACCOUNT_ID.eq(account.get())
				))
				.fetchOne(record -> createAuthorizedClient(record, account, registration));
	}

	@Override
	@Transactional(label = "oauth-client-service-save")
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		final EntityId account = lookupUserIdentifierForAuthentication(principal);

		final ClientRegistration registration = authorizedClient.getClientRegistration();

		log.info(MARKER, "Storing access token for OAuth Client {} and user account with identifier {}",
				registration.getRegistrationId(), account);

		final SettableRecord record = SettableRecord.of(context, ACCOUNT_ACCESS_TOKENS)
				.with(applyAccessToken(authorizedClient.getAccessToken()))
				.with(applyRefreshToken(authorizedClient.getRefreshToken()));

		context.insertInto(ACCOUNT_ACCESS_TOKENS)
				.set(ACCOUNT_ACCESS_TOKENS.CLIENT_REGISTRATION_ID, registration.getRegistrationId())
				.set(ACCOUNT_ACCESS_TOKENS.ACCOUNT_ID, account.get())
				.set(record.get())
				.onDuplicateKeyUpdate()
				.set(record.get())
				.execute();
	}

	@Override
	@Transactional(label = "oauth-client-service-remove")
	public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
		final EntityId account = EntityId.from(principalName);

		log.info(MARKER, "Removing OAuth access token for [client={}, account={}]", clientRegistrationId, account);

		context.delete(ACCOUNT_ACCESS_TOKENS)
				.where(DSL.and(
					ACCOUNT_ACCESS_TOKENS.CLIENT_REGISTRATION_ID.eq(clientRegistrationId),
					ACCOUNT_ACCESS_TOKENS.ACCOUNT_ID.eq(account.get())
				))
				.execute();
	}

	@SuppressWarnings("unchecked")
	private OAuth2AuthorizedClient createAuthorizedClient(Record record, EntityId account, ClientRegistration registration) {
		if (log.isDebugEnabled()) {
			log.debug("Creating OAuth2 access token for [client={}, account={}]", registration.getRegistrationId(), account);
		}

		final OAuth2AccessToken accessToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER,
				createTokenValue(record, ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_VALUE),
				record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_ISSUED_AT, instantConverter),
				record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_EXPIRES_AT, instantConverter),
				record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_SCOPES, scopesConverter)
		);

		// no refresh token was issued by the client, just return the access token
		if (record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE) == null) {
			return new OAuth2AuthorizedClient(registration, account.serialize(), accessToken);
		}

		final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
				createTokenValue(record, ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE),
				record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_ISSUED_AT, instantConverter),
				record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_EXPIRES_AT, instantConverter)
		);

		return new OAuth2AuthorizedClient(registration, account.serialize(), accessToken, refreshToken);
	}

	@NonNull
	private UnaryOperator<SettableRecord> applyAccessToken(@Nullable OAuth2AccessToken token) {
		if (token == null) {
			return UnaryOperator.identity();
		}

		return record -> {
			final ByteArray value = ByteArray.fromString(token.getTokenValue());

			return record.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_VALUE, value, encryptionConverter)
					.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_SCOPES, token.getScopes(), scopesConverter)
					.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_ISSUED_AT, token.getIssuedAt(), instantConverter)
					.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_EXPIRES_AT, token.getExpiresAt(), instantConverter);
		};
	}

	@NonNull
	private UnaryOperator<SettableRecord> applyRefreshToken(@Nullable OAuth2RefreshToken token) {
		if (token == null) {
			return UnaryOperator.identity();
		}

		return record -> {
			final ByteArray value = ByteArray.fromString(token.getTokenValue());

			return record.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE, value, encryptionConverter)
					.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_ISSUED_AT, token.getIssuedAt(), instantConverter)
					.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_EXPIRES_AT, token.getExpiresAt(), instantConverter);
		};
	}

	private String createTokenValue(@NonNull Record record, @NonNull Field<ByteArray> field) {
		final ByteArray value = record.get(field, encryptionConverter);
		return value == null ? null : new String(value.array(), StandardCharsets.UTF_8);
	}

	private static EntityId lookupUserIdentifierForAuthentication(@NonNull Authentication authentication) {
		if (authentication.getPrincipal() instanceof AccountIdentity identity) {
			return identity.getId();
		}

		throw new InternalAuthenticationServiceException("Failed to resolve user account identifier from "
				+ "OAuth authentication. Make sure that the user account is already created before storing "
				+ "the OAuth access and refresh tokens. Created OAuthUser needs to have an `id` attribute set");
	}

}
