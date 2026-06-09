package com.konfigyr.identity.authorization;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityUser;
import com.konfigyr.security.NamespaceClientId;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

import static com.konfigyr.data.tables.NamespaceMembers.NAMESPACE_MEMBERS;

/**
 * Authorization code request validator that enforces namespace membership before an
 * authorization code is issued.
 * <p>
 * For clients whose {@code client_id} can be parsed as a {@link NamespaceClientId}, the
 * authenticated principal must be a member of the corresponding namespace. Requests from
 * non-members are rejected with {@code error=access_denied} and redirected back to the
 * client's registered {@code redirect_uri}.
 * <p>
 * Non-namespace clients (i.e., those whose {@code client_id} does not match the
 * {@link NamespaceClientId#PREFIX}) are passed through without any check.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public class NamespaceMembershipValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

	private final DSLContext context;

	NamespaceMembershipValidator(@NonNull DSLContext context) {
		this.context = context;
	}

	@Override
	public void accept(@NonNull OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
		final OAuth2AuthorizationCodeRequestAuthenticationToken authentication = authenticationContext.getAuthentication();

		if (!(authentication.getPrincipal() instanceof Authentication principal)) {
			return;
		}

		final RegisteredClient client = authenticationContext.getRegisteredClient();

		final EntityId namespace = NamespaceClientId.tryParse(client.getClientId())
				.map(NamespaceClientId::namespace)
				.orElse(null);

		if (namespace == null) {
			return;
		}

		final AccountIdentity identity;

		if (principal.getPrincipal() instanceof AccountIdentityUser user) {
			identity = user.getAccountIdentity();
		} else if (principal.getPrincipal() instanceof AccountIdentity id) {
			identity = id;
		} else {
			return;
		}

		final boolean isMember = context.fetchExists(NAMESPACE_MEMBERS, DSL.and(
				NAMESPACE_MEMBERS.NAMESPACE_ID.eq(namespace.get()),
				NAMESPACE_MEMBERS.ACCOUNT_ID.eq(identity.getId().get())
		));

		if (!isMember) {
			throwAccessDenied(authentication, principal, client);
		}
	}

	private static void throwAccessDenied(
			@NonNull OAuth2AuthorizationCodeRequestAuthenticationToken authentication,
			@NonNull Authentication principal,
			@NonNull RegisteredClient registeredClient
	) {
		OAuth2AuthorizationCodeRequestAuthenticationToken result = authentication;

		if (!StringUtils.hasText(authentication.getRedirectUri())) {
			result = new OAuth2AuthorizationCodeRequestAuthenticationToken(
					authentication.getAuthorizationUri(),
					authentication.getClientId(),
					principal,
					registeredClient.getRedirectUris().iterator().next(),
					authentication.getState(),
					authentication.getScopes(),
					authentication.getAdditionalParameters()
			);
			result.setAuthenticated(authentication.isAuthenticated());
		}

		throw new OAuth2AuthorizationCodeRequestAuthenticationException(
				new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED),
				result
		);
	}
}
