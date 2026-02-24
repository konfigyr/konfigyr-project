package com.konfigyr.identity.authorization;

import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityUser;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

final class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

	@Override
	public void customize(@NonNull JwtEncodingContext context) {
		if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
			final Authentication authentication = context.getPrincipal();

			if (authentication.getPrincipal() instanceof AccountIdentityUser user) {
				customizeAccessToken(user.getAccountIdentity(), context.getClaims());
			}

			if (authentication.getPrincipal() instanceof AccountIdentity identity) {
				customizeAccessToken(identity, context.getClaims());
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

	private void customizeAccessToken(@NonNull AccountIdentity identity, JwtClaimsSet.Builder claims) {
		claims.claim(StandardClaimNames.EMAIL, identity.getEmail())
				.claim(StandardClaimNames.NAME, identity.getDisplayName());
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
