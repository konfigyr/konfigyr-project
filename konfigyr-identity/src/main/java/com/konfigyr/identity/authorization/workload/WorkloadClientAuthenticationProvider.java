package com.konfigyr.identity.authorization.workload;

import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.security.NamespaceClientType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * An {@link AuthenticationProvider} that authenticates {@link NamespaceClientType#WORKLOAD}
 * namespace clients using {@link ClientAuthenticationMethod#NONE}. Unlike the default
 * {@code PublicClientAuthenticationProvider}, this provider does not require a PKCE
 * {@code code_verifier} because workload clients authenticate via the token exchange
 * grant, not the authorization code flow.
 * <p>
 * Non-workload clients with method {@link ClientAuthenticationMethod#NONE} are passed
 * through by returning {@code null}, leaving the PKCE check for Agent clients intact.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@NullMarked
class WorkloadClientAuthenticationProvider implements AuthenticationProvider {

	private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1";

	private final RegisteredClientRepository registeredClientRepository;

	WorkloadClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
		Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
		this.registeredClientRepository = registeredClientRepository;
	}

	@Nullable
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final OAuth2ClientAuthenticationToken clientAuthentication = (OAuth2ClientAuthenticationToken) authentication;

		if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
			return null;
		}

		final String clientId = Objects.toString(clientAuthentication.getPrincipal(), clientAuthentication.getName());
		final RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

		if (registeredClient == null) {
			throwInvalidClient(OAuth2ParameterNames.CLIENT_ID);
		}

		final NamespaceClientType namespaceClientType = registeredClient.getClientSettings()
				.getSetting(NamespaceClientSettingNames.CLIENT_TYPE);

		// check if the client is a workload client, if not, return null and try
		// to authenticate the OAuth request with a different authentication provider
		if (!NamespaceClientType.WORKLOAD.equals(namespaceClientType)) {
			return null;
		}

		if (!registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
			throwInvalidClient("authentication_method");
		}

		return new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.NONE, null);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private static void throwInvalidClient(String parameterName) {
		throw new OAuth2AuthenticationException(new OAuth2Error(
				OAuth2ErrorCodes.INVALID_CLIENT,
				"Client authentication failed: " + parameterName,
				ERROR_URI
		));
	}

}
