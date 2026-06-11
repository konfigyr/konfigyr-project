package com.konfigyr.identity.authorization.workload;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.identity.authorization.issuer.TrustedIssuer;
import com.konfigyr.identity.authorization.issuer.TrustedIssuerRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.*;

/**
 * An {@link AuthenticationProvider} implementation for the OAuth 2.0 Token Exchange Grant
 * for namespace Workload Identity applications.
 * <p>
 * Validates the {@code subject_token} as an external OIDC JWT whose issuer is resolved
 * through the {@link TrustedIssuerRepository}. The provider is appended after the default
 * {@code OAuth2TokenExchangeAuthenticationProvider} in the chain so that its error codes
 * take precedence: when the default provider fails first its exception is stored as
 * {@code lastException}; this provider then either succeeds (breaking the chain) or throws
 * its own exception which overwrites the default one.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@NullMarked
class WorkloadTokenExchangeAuthenticationProvider implements AuthenticationProvider {

	private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

	private static final String JWT_TOKEN_TYPE_URI = "urn:ietf:params:oauth:token-type:jwt";
	private static final String ID_TOKEN_TYPE_URI = "urn:ietf:params:oauth:token-type:id_token";
	private static final Set<String> SUPPORTED_TOKEN_TYPES = Set.of(JWT_TOKEN_TYPE_URI, ID_TOKEN_TYPE_URI);

	private final TrustedIssuerRepository trustedIssuerRepository;
	private final OAuth2AuthorizationService authorizationService;
	private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

	WorkloadTokenExchangeAuthenticationProvider(
			TrustedIssuerRepository trustedIssuerRepository,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator
	) {
		this.trustedIssuerRepository = trustedIssuerRepository;
		this.authorizationService = authorizationService;
		this.tokenGenerator = tokenGenerator;
	}

	@Nullable
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final OAuth2TokenExchangeAuthenticationToken tokenExchange = (OAuth2TokenExchangeAuthenticationToken) authentication;

		final OAuth2ClientAuthenticationToken clientPrincipal = resolveAuthenticatedClient(tokenExchange);
		final RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

		if (registeredClient == null) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
		}

		if (log.isDebugEnabled()) {
			log.debug("Processing OAuth2 token exchange for: [client_id={}, subject_token_type={}, requested_token_type={}]",
					registeredClient.getClientId(), tokenExchange.getSubjectTokenType(), tokenExchange.getRequestedTokenType());
		}

		validateSubjectTokenType(tokenExchange);

		final TrustedIssuer trustedIssuer = lookupTrustedIssuer(registeredClient);

		if (log.isDebugEnabled()) {
			log.debug("Found a matching trusted issuer for the OAuth2 client: [client_id={}, issuer={}]",
					registeredClient.getClientId(), trustedIssuer);
		}

		final Jwt subjectJwt = decodeSubjectToken(trustedIssuer, tokenExchange);

		if (!hasAllowedAudience(trustedIssuer, subjectJwt)) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_REQUEST,
					"Subject token audience does not match any of the required audiences",
					ERROR_URI
			));
		}

		if (!hasMatchingSubject(registeredClient, subjectJwt)) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_REQUEST,
					"Subject token sub claim does not match the required pattern",
					ERROR_URI
			));
		}

		final DefaultOAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
				.registeredClient(registeredClient)
				.principal(clientPrincipal)
				.authorizationServerContext(AuthorizationServerContextHolder.getContext())
				.authorizedScopes(resolveScopes(registeredClient, tokenExchange.getScopes()))
				.tokenType(OAuth2TokenType.ACCESS_TOKEN)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.authorizationGrant(tokenExchange)
				.build();

		final OAuth2Token generatedToken = tokenGenerator.generate(tokenContext);

		if (generatedToken == null) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.SERVER_ERROR,
					"Unexpected error occurred while generating the access token",
					ERROR_URI
			));
		}

		if (log.isDebugEnabled()) {
			log.debug("Generated access token for workload client: {}", registeredClient.getClientId());
		}

		final OAuth2AccessToken accessToken = new OAuth2AccessToken(
				OAuth2AccessToken.TokenType.BEARER,
				generatedToken.getTokenValue(),
				generatedToken.getIssuedAt(),
				generatedToken.getExpiresAt(),
				tokenContext.getAuthorizedScopes()
		);

		final OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
				.principalName(Objects.requireNonNull(subjectJwt.getSubject(), registeredClient::getClientId))
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.authorizedScopes(tokenContext.getAuthorizedScopes())
				.attribute(Principal.class.getName(), clientPrincipal)
				.token(accessToken, metadata -> {
					if (generatedToken instanceof ClaimAccessor claimAccessor) {
						metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims());
					}
					metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
					metadata.put(OAuth2TokenFormat.class.getName(),
							registeredClient.getTokenSettings().getAccessTokenFormat().getValue());
				});

		authorizationService.save(authorizationBuilder.build());

		if (log.isDebugEnabled()) {
			log.debug("Saved workload token exchange authorization for principal: {}", subjectJwt.getSubject());
		}

		return new OAuth2AccessTokenAuthenticationToken(
				registeredClient, clientPrincipal, accessToken, null,
				Map.of(OAuth2ParameterNames.ISSUED_TOKEN_TYPE, tokenExchange.getRequestedTokenType()));
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2TokenExchangeAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private TrustedIssuer lookupTrustedIssuer(RegisteredClient registeredClient) {
		final EntityId namespace = registeredClient.getClientSettings().getSetting(
				NamespaceClientSettingNames.NAMESPACE
		);

		if (namespace == null) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
		}

		final String issuerUri = registeredClient.getClientSettings().getSetting(
				NamespaceClientSettingNames.WORKLOAD_ISSUER_URI
		);

		if (!StringUtils.hasText(issuerUri)) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_CLIENT,
					"Trusted issuer is not specified for this OAuth2 Client",
					ERROR_URI
			));
		}

		final TrustedIssuer trustedIssuer = trustedIssuerRepository.lookup(namespace, issuerUri);

		if (trustedIssuer == null) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_CLIENT,
					"Can't find any trusted issuer for this OAuth2 Client that matches this issuer URI: %s".formatted(issuerUri),
					ERROR_URI
			));
		}

		return trustedIssuer;
	}

	private static Jwt decodeSubjectToken(TrustedIssuer issuer, OAuth2TokenExchangeAuthenticationToken exchange) {
		final JwtDecoder decoder;

		if (StringUtils.hasText(issuer.jwksUri())) {
			decoder = NimbusJwtDecoder.withJwkSetUri(issuer.jwksUri()).build();
		} else {
			decoder = JwtDecoders.fromIssuerLocation(issuer.issuerUri());
		}

		try {
			return decoder.decode(exchange.getSubjectToken());
		} catch (JwtException ex) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_REQUEST,
					"Invalid subject_token: " + ex.getMessage(),
					ERROR_URI
			), ex);
		}
	}

	private static boolean hasAllowedAudience(TrustedIssuer issuer, Jwt jwt) {
		if (CollectionUtils.isEmpty(issuer.allowedAudiences())) {
			return true;
		}

		final List<String> audience = jwt.getAudience();

		if (CollectionUtils.isEmpty(audience)) {
			return false;
		}

		return issuer.allowedAudiences().stream().anyMatch(audience::contains);
	}

	private static boolean hasMatchingSubject(RegisteredClient registeredClient, Jwt jwt) {
		final String subjectPattern = registeredClient.getClientSettings().getSetting(
				NamespaceClientSettingNames.WORKLOAD_SUBJECT_PATTERN
		);

		if (!StringUtils.hasText(subjectPattern)) {
			return true;
		}

		final String subject = jwt.getSubject();
		return StringUtils.hasText(subject) && subject.matches(subjectPattern);
	}

	private static void validateSubjectTokenType(OAuth2TokenExchangeAuthenticationToken exchange) {
		if (!SUPPORTED_TOKEN_TYPES.contains(exchange.getSubjectTokenType())) {
			throw new OAuth2AuthenticationException(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_REQUEST,
					"Unsupported subject_token_type: " + exchange.getSubjectTokenType(),
					ERROR_URI
			));
		}
	}

	private static OAuth2ClientAuthenticationToken resolveAuthenticatedClient(Authentication authentication) {
		if (authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken principal && principal.isAuthenticated()) {
			return principal;
		}
		throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
	}

	private static Set<String> resolveScopes(RegisteredClient registeredClient, Set<String> requestedScopes) {
		final Set<String> scopes = CollectionUtils.isEmpty(requestedScopes)
				? registeredClient.getScopes()
				: requestedScopes;

		for (final String scope : scopes) {
			if (!registeredClient.getScopes().contains(scope)) {
				throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
			}
		}

		return new LinkedHashSet<>(scopes);
	}

}
