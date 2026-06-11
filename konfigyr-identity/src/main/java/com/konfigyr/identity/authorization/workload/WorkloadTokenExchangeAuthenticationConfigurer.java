package com.konfigyr.identity.authorization.workload;

import com.konfigyr.identity.authorization.issuer.TrustedIssuerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.*;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;

/**
 * An {@link AbstractHttpConfigurer} that wires all components required for the Workload
 * Identity token exchange flow into the OAuth 2.0 Authorization Server.
 * <p>
 * It performs the following steps:
 * <ul>
 *     <li>replaces the default client authentication converter list with one that includes
 *         {@link WorkloadClientAuthenticationConverter} for {@code grant_type=token_exchange}
 *         requests alongside the standard converters for all other grant types</li>
 *     <li>registers {@link WorkloadClientAuthenticationProvider} to authenticate
 *         {@link com.konfigyr.security.NamespaceClientType#WORKLOAD} clients without
 *         requiring a PKCE {@code code_verifier}</li>
 *     <li>registers {@link WorkloadTokenExchangeAuthenticationProvider} to validate the
 *         external OIDC {@code subject_token} and issue a scoped Konfigyr access token</li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class WorkloadTokenExchangeAuthenticationConfigurer
		extends AbstractHttpConfigurer<WorkloadTokenExchangeAuthenticationConfigurer, HttpSecurity> {

	private final OAuth2AuthorizationServerConfigurer authorizationServerConfigurer;

	@Override
	@SuppressWarnings("unchecked")
	public void init(HttpSecurity builder) {
		super.init(builder);

		authorizationServerConfigurer
				.clientAuthentication(clientAuthentication -> clientAuthentication
						.authenticationConverter(createClientAuthenticationConverters())
				);

		builder.authenticationProvider(
				postProcess(new WorkloadClientAuthenticationProvider(
						getSharedObject(builder, RegisteredClientRepository.class)
				))
		);

		builder.authenticationProvider(
				postProcess(new WorkloadTokenExchangeAuthenticationProvider(
						getSharedObject(builder, TrustedIssuerRegistry.class),
						getSharedObject(builder, OAuth2AuthorizationService.class),
						getSharedObject(builder, OAuth2TokenGenerator.class)
				))
		);
	}

	private static <T> T getSharedObject(HttpSecurity builder, Class<T> type) {
		T object = builder.getSharedObject(type);

		if (object == null) {
			object = builder.getSharedObject(ApplicationContext.class).getBean(type);
		}

		return object;
	}

	private static AuthenticationConverter createClientAuthenticationConverters() {
		return new DelegatingAuthenticationConverter(
				new JwtClientAssertionAuthenticationConverter(),
				new ClientSecretBasicAuthenticationConverter(),
				new ClientSecretPostAuthenticationConverter(),
				new PublicClientAuthenticationConverter(),
				new WorkloadClientAuthenticationConverter()
		);
	}
}
