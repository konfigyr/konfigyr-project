package com.konfigyr.identity.authorization;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityUser;
import com.konfigyr.security.KonfigyrClaimNames;
import org.jspecify.annotations.NullMarked;
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
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link OAuth2TokenCustomizer} that adds claims to access and ID tokens issued by the
 * authorization server.
 * <p>
 * Every token is signed with {@link #SIGNING_ALGORITHM}.
 * <p>
 * Access tokens get the {@code aud} claim, plus:
 * <ul>
 *     <li>a {@code kfg_namespace} claim, when the registered client's {@link ClientSettings}
 *     carries a {@link NamespaceClientSettingNames#NAMESPACE} setting - independent of grant or
 *     authentication type</li>
 *     <li>{@code name}/{@code email} claims, when the principal resolves to an
 *     {@link AccountIdentity}, gated by the authorized {@code openid}/{@code email} scopes</li>
 *     <li>a {@code name} claim from the registered client's name, when the authentication is an
 *     {@link OAuth2ClientAuthenticationToken}</li>
 * </ul>
 * ID tokens get {@code oid}, {@code email}, {@code name}, and {@code picture} claims, when the
 * principal resolves to an {@link AccountIdentity}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
final class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	static final JwsAlgorithm SIGNING_ALGORITHM = SignatureAlgorithm.PS256;
	static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE = new OAuth2TokenType(OidcParameterNames.ID_TOKEN);

	private final List<String> audiences;

	/**
	 * Creates a new {@link TokenCustomizer}.
	 *
	 * @param audiences the {@code aud} claim values to add to every access token, can't be {@literal null}
	 */
	TokenCustomizer(List<String> audiences) {
		this.audiences = Collections.unmodifiableList(audiences);
	}

	/**
	 * Applies the claim customizations described in the class Javadoc, based on the context's
	 * token type.
	 *
	 * @param context the JWT encoding context, can't be {@literal null}
	 */
	@Override
	public void customize(JwtEncodingContext context) {
		// always use PS256 for signing the JWS
		context.getJwsHeader().algorithm(SIGNING_ALGORITHM);

		final Authentication authentication = context.getPrincipal();

		if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
			final RegisteredClient registeredClient = context.getRegisteredClient();
			registerNamespaceClaim(registeredClient.getClientSettings(), context.getClaims());

			context.getClaims().audience(audiences);

			final Set<String> authorizedScopes = context.getAuthorizedScopes();

			if (authentication.getPrincipal() instanceof AccountIdentityUser user) {
				customizeAccessToken(user.getAccountIdentity(), authorizedScopes, context.getClaims());
			}

			if (authentication.getPrincipal() instanceof AccountIdentity identity) {
				customizeAccessToken(identity, authorizedScopes, context.getClaims());
			}

			if (authentication instanceof OAuth2ClientAuthenticationToken) {
				customizeAccessToken(registeredClient, context.getClaims());
			}
		}

		if (ID_TOKEN_TOKEN_TYPE.equals(context.getTokenType())) {

			if (authentication.getPrincipal() instanceof AccountIdentityUser user) {
				customizeIdToken(user.getAccountIdentity(), context.getClaims());
			}

			if (authentication.getPrincipal() instanceof AccountIdentity identity) {
				customizeIdToken(identity, context.getClaims());
			}
		}
	}

	private void customizeAccessToken(AccountIdentity identity, Set<String> authorizedScopes, JwtClaimsSet.Builder claims) {
		if (authorizedScopes.contains(OidcScopes.OPENID)) {
			claims.claim(StandardClaimNames.EMAIL, identity.getEmail())
					.claim(StandardClaimNames.NAME, identity.getDisplayName());
		}

		if (authorizedScopes.contains(OidcScopes.EMAIL)) {
			claims.claim(StandardClaimNames.EMAIL, identity.getEmail());
		}
	}

	private void customizeAccessToken(RegisteredClient registeredClient, JwtClaimsSet.Builder claims) {
		claims.claim(StandardClaimNames.NAME, registeredClient.getClientName());
	}

	private void customizeIdToken(AccountIdentity identity, JwtClaimsSet.Builder claims) {
		claims.claim("oid", identity.getUsername())
				.claim(StandardClaimNames.EMAIL, identity.getEmail())
				.claim(StandardClaimNames.NAME, identity.getDisplayName())
				.claim(StandardClaimNames.PICTURE, identity.getAvatar().get());
	}
	
	private void registerNamespaceClaim(ClientSettings settings, JwtClaimsSet.Builder claims) {
		final EntityId namespace = settings.getSetting(NamespaceClientSettingNames.NAMESPACE);

		if (namespace != null) {
			claims.claim(KonfigyrClaimNames.NAMESPACE, namespace.serialize());
		}
	}
}
