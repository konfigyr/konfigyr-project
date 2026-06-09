package com.konfigyr.identity.authorization;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityUser;
import com.konfigyr.security.KonfigyrClaimNames;
import com.konfigyr.security.NamespaceClientId;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	static final JwsAlgorithm SIGNING_ALGORITHM = SignatureAlgorithm.PS256;
	static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

	private final List<String> audiences;

	TokenCustomizer(List<String> audiences) {
		this.audiences = Collections.unmodifiableList(audiences);
	}

	@Override
	public void customize(@NonNull JwtEncodingContext context) {
		// always use PS256 for signing the JWS
		context.getJwsHeader().algorithm(SIGNING_ALGORITHM);

		if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
			context.getClaims().audience(audiences);

			Optional.ofNullable(context.getRegisteredClient())
					.map(RegisteredClient::getClientId)
					.flatMap(NamespaceClientId::tryParse)
					.map(NamespaceClientId::namespace)
					.map(EntityId::serialize)
					.ifPresent(namespace -> context.getClaims().claim(KonfigyrClaimNames.NAMESPACE, namespace));

			final Authentication authentication = context.getPrincipal();
			final Set<String> authorizedScopes = context.getAuthorizedScopes();

			if (authentication.getPrincipal() instanceof AccountIdentityUser user) {
				customizeAccessToken(user.getAccountIdentity(), authorizedScopes, context.getClaims());
			}

			if (authentication.getPrincipal() instanceof AccountIdentity identity) {
				customizeAccessToken(identity, authorizedScopes, context.getClaims());
			}

			if (authentication instanceof OAuth2ClientAuthenticationToken client && client.getRegisteredClient() != null) {
				customizeAccessToken(client.getRegisteredClient(), context.getClaims());
			}
		}

		if (ID_TOKEN_TOKEN_TYPE.equals(context.getTokenType())) {
			final Authentication authentication = context.getPrincipal();

			if (authentication.getPrincipal() instanceof AccountIdentityUser user) {
				customizeIdToken(user.getAccountIdentity(), context.getClaims());
			}

			if (authentication.getPrincipal() instanceof AccountIdentity identity) {
				customizeIdToken(identity, context.getClaims());
			}
		}
	}

	private void customizeAccessToken(@NonNull AccountIdentity identity, Set<String> authorizedScopes, JwtClaimsSet.Builder claims) {
		if (authorizedScopes.contains(OidcScopes.OPENID)) {
			claims.claim(StandardClaimNames.EMAIL, identity.getEmail())
					.claim(StandardClaimNames.NAME, identity.getDisplayName());
		}

		if (authorizedScopes.contains(OidcScopes.EMAIL)) {
			claims.claim(StandardClaimNames.EMAIL, identity.getEmail());
		}
	}

	private void customizeAccessToken(@NonNull RegisteredClient registeredClient, JwtClaimsSet.Builder claims) {
		claims.claim(StandardClaimNames.NAME, registeredClient.getClientName());
	}

	private void customizeIdToken(@NonNull AccountIdentity identity, JwtClaimsSet.Builder claims) {
		claims.claim("oid", identity.getUsername())
				.claim(StandardClaimNames.EMAIL, identity.getEmail())
				.claim(StandardClaimNames.NAME, identity.getDisplayName())
				.claim(StandardClaimNames.PICTURE, identity.getAvatar().get());
	}
}
