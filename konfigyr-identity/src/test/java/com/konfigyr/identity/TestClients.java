package com.konfigyr.identity;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public interface TestClients {

	/**
	 * Creates a {@link ClientRegistration.Builder} with the given client registration identifier with
	 * default values that can be used to immediately create a {@link ClientRegistration}.
	 *
	 * @param id client registration identifier, can't be {@literal null}
	 * @return client registration builder, never {@literal null}
	 */
	@NonNull
	static ClientRegistration.Builder clientRegistration(@NonNull String id) {
		return ClientRegistration.withRegistrationId(id)
				.clientId("konfigyr")
				.clientSecret("shhh!")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.tokenUri("https://oauth.com/oauth/token");
	}

}
