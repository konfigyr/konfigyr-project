package com.konfigyr.identity.authorization;

import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityUser;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

final class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

	@Override
	public void customize(@NonNull JwtEncodingContext context) {
		if (ID_TOKEN_TOKEN_TYPE.equals(context.getTokenType())) {
			final Authentication authentication = context.getPrincipal();

			if (authentication.getPrincipal() instanceof AccountIdentityUser user) {
				customize(user.getAccountIdentity(), context.getClaims());
			}

			if (authentication.getPrincipal() instanceof AccountIdentity identity) {
				customize(identity, context.getClaims());
			}
		}
	}

	private void customize(@NonNull AccountIdentity identity, JwtClaimsSet.Builder claims) {
		claims.claim("oid", identity.getUsername())
				.claim("email", identity.getEmail())
				.claim("name", identity.getDisplayName())
				.claim("picture", identity.getAvatar().get());
	}
}
