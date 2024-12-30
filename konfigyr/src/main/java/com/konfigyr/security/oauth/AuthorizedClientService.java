package com.konfigyr.security.oauth;

import com.konfigyr.crypto.KeysetOperations;
import com.konfigyr.entity.EntityId;
import com.konfigyr.io.ByteArray;
import com.konfigyr.security.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Converter;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.konfigyr.data.tables.AccountAccessTokens.ACCOUNT_ACCESS_TOKENS;

/**
 * Implementation of the {@link OAuth2AuthorizedClientService} that would retrieve and
 * store OAuth Access Tokens in the {@link com.konfigyr.data.tables.AccountAccessTokens} database table.
 * <p>
 * Considering that the access and refresh token values are sensitive user information, they would be
 * encrypted using the {@link KeysetOperations}, based on {@link OAuthKeysets#ACCESS_TOKEN} key before
 * they are stored to the database.
 *
 * @author Vladimir Spasic
 * @since 1.0.
 **/
@Slf4j
@RequiredArgsConstructor
public class AuthorizedClientService implements OAuth2AuthorizedClientService {

	private static final Marker MARKER = MarkerFactory.getMarker("OAUTH_CLIENT_SERVICE");

	/**
	 * Converter that converts of {@link ZoneOffset} to {@link Instant}, and vice versa, using the
	 * {@link ZoneOffset#UTC UTC offset} thus making sure that the OAuth access token timestamps
	 * are written in the database with UTC time zone.
	 */
	private static final Converter<OffsetDateTime, Instant> INSTANT_CONVERTER = Converter.ofNullable(
			OffsetDateTime.class,
			Instant.class,
			OffsetDateTime::toInstant,
			instant -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
	);

	private final DSLContext context;
	private final KeysetOperations keyset;
	private final ClientRegistrationRepository clientRegistrationRepository;

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, label = "oauth-client-service-load")
	public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
		final EntityId account = EntityId.from(principalName);

		if (log.isDebugEnabled()) {
			log.debug("Looking up OAuth access token for [client={}, account={}]", clientRegistrationId, account);
		}

		final ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);

		if (registration == null) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
		}

		return (T) context.select(ACCOUNT_ACCESS_TOKENS.fields())
				.from(ACCOUNT_ACCESS_TOKENS)
				.where(DSL.and(
						ACCOUNT_ACCESS_TOKENS.CLIENT_REGISTRATION_ID.eq(clientRegistrationId),
						ACCOUNT_ACCESS_TOKENS.ACCOUNT_ID.eq(account.get())
				))
				.fetchOne(record -> map(record, account, registration));
	}

	@Override
	@Transactional(label = "oauth-client-service-save")
	public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
		final EntityId account = lookupUserIdentifierForAuthentication(principal);

		final ClientRegistration registration = authorizedClient.getClientRegistration();
		final OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		final OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

		log.info(MARKER, "Storing access token for OAuth Client {} and user account with identifier {}",
				registration.getRegistrationId(), account);

		final Instant refreshTokenIssuedAt = refreshToken != null ? refreshToken.getIssuedAt() : null;
		final Instant refreshTokenExpiresAt = refreshToken != null ? refreshToken.getExpiresAt() : null;
		final String accessTokenScopes = StringUtils.collectionToDelimitedString(accessToken.getScopes(), ",");
		final ByteArray accessTokenValue = encrypt(accessToken);
		final ByteArray refreshTokenValue = encrypt(refreshToken);

		context.insertInto(ACCOUNT_ACCESS_TOKENS)
				.set(ACCOUNT_ACCESS_TOKENS.CLIENT_REGISTRATION_ID, registration.getRegistrationId())
				.set(ACCOUNT_ACCESS_TOKENS.ACCOUNT_ID, account.get())
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_VALUE, accessTokenValue)
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_SCOPES, accessTokenScopes)
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_ISSUED_AT, INSTANT_CONVERTER.to(accessToken.getIssuedAt()))
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_EXPIRES_AT, INSTANT_CONVERTER.to(accessToken.getExpiresAt()))
				.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE, refreshTokenValue)
				.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_ISSUED_AT, INSTANT_CONVERTER.to(refreshTokenIssuedAt))
				.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_EXPIRES_AT, INSTANT_CONVERTER.to(refreshTokenExpiresAt))
				.onDuplicateKeyUpdate()
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_VALUE, accessTokenValue)
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_SCOPES, accessTokenScopes)
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_ISSUED_AT, INSTANT_CONVERTER.to(accessToken.getIssuedAt()))
				.set(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_EXPIRES_AT, INSTANT_CONVERTER.to(accessToken.getExpiresAt()))
				.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE, refreshTokenValue)
				.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_ISSUED_AT, INSTANT_CONVERTER.to(refreshTokenIssuedAt))
				.set(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_EXPIRES_AT, INSTANT_CONVERTER.to(refreshTokenExpiresAt))
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

	private OAuth2AuthorizedClient map(Record record, EntityId account, ClientRegistration registration) {
		Assert.state(record.get(ACCOUNT_ACCESS_TOKENS.ACCOUNT_ID) == account.get(),
				"Retrieved OAuth access token account entity identifier does not match the principal");

		if (log.isDebugEnabled()) {
			log.debug("Creating OAuth2 access token for [client={}, account={}]", registration.getRegistrationId(), account);
		}

		final OAuth2AccessToken accessToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER,
				decrypt(record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_VALUE)),
				record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_ISSUED_AT, INSTANT_CONVERTER),
				record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_EXPIRES_AT, INSTANT_CONVERTER),
				StringUtils.commaDelimitedListToSet(record.get(ACCOUNT_ACCESS_TOKENS.ACCESS_TOKEN_SCOPES))
		);

		// no refresh token was issued by the client, just return the access token
		if (record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE) == null) {
			return new OAuth2AuthorizedClient(registration, account.serialize(), accessToken);
		}

		final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
				decrypt(record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_VALUE)),
				record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_ISSUED_AT, INSTANT_CONVERTER),
				record.get(ACCOUNT_ACCESS_TOKENS.REFRESH_TOKEN_EXPIRES_AT, INSTANT_CONVERTER)
		);

		return new OAuth2AuthorizedClient(registration, account.serialize(), accessToken, refreshToken);
	}

	private String decrypt(ByteArray data) {
		if (data == null) {
			return null;
		}

		final ByteArray bytes = keyset.decrypt(data);
		return new String(bytes.array(), StandardCharsets.UTF_8);
	}

	private ByteArray encrypt(AbstractOAuth2Token token) {
		if (token == null || token.getTokenValue() == null) {
			return null;
		}

		final ByteArray bytes = ByteArray.fromString(token.getTokenValue(), StandardCharsets.UTF_8);

		return keyset.encrypt(bytes);
	}

	private static EntityId lookupUserIdentifierForAuthentication(Authentication authentication) {
		if (authentication.getPrincipal() instanceof AccountPrincipal account) {
			return account.getId();
		}

		throw new InternalAuthenticationServiceException("Failed to resolve user account identifier from "
				+ "OAuth authentication. Make sure that the user account is already created before storing "
				+ "the OAuth access and refresh tokens. Created OAuthUser needs to have an `id` attribute set");
	}

}
