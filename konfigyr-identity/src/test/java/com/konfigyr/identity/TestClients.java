package com.konfigyr.identity;

import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.security.NamespaceClientId;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

public interface TestClients {

	/**
	 * Creates a {@link ClientRegistration.Builder} with the given client registration identifier with
	 * default values that can be used to immediately create a {@link ClientRegistration}.
	 *
	 * @param id client registration identifier, can't be {@literal null}
	 * @return client registration builder, never {@literal null}
	 */
	static ClientRegistration.Builder clientRegistration(@NonNull String id) {
		return ClientRegistration.withRegistrationId(id)
				.clientId("konfigyr")
				.clientSecret("shhh!")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.tokenUri("https://oauth.com/oauth/token");
	}

	/**
	 * Creates a {@link RegisteredClient.Builder} with the given client identifier.
	 *
	 * @param clientId client identifier, can't be {@literal null}
	 * @return registered client builder, never {@literal null}
	 */
	static RegisteredClient.Builder clientRegistration(@NonNull NamespaceClientId clientId) {
		final var builder = RegisteredClient.withId("client-registration-" + clientId.get())
				.clientId(clientId.get())
				.clientSettings(ClientSettings.builder()
						.setting(NamespaceClientSettingNames.NAMESPACE, clientId.namespace())
						.setting(NamespaceClientSettingNames.CLIENT_TYPE, clientId.type())
						.build()
				);

		return switch (clientId.type()) {
			case AGENT -> builder
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
					.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
			case WORKLOAD -> builder
					.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
					.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
			case SERVICE_ACCOUNT -> builder
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		};
	}

}
