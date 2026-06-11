package com.konfigyr.identity.authorization.workload;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.TestClients;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.security.NamespaceClientId;
import com.konfigyr.security.NamespaceClientType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class WorkloadClientAuthenticationProviderTest {

	static final EntityId NAMESPACE = EntityId.from(42L);
	static final NamespaceClientId WORKLOAD_CLIENT_ID = NamespaceClientId.of(NAMESPACE, NamespaceClientType.WORKLOAD);
	static final NamespaceClientId AGENT_CLIENT_ID = NamespaceClientId.of(NAMESPACE, NamespaceClientType.AGENT);

	@Mock
	RegisteredClientRepository repository;

	WorkloadClientAuthenticationProvider provider;

	@BeforeEach
	void setup() {
		provider = new WorkloadClientAuthenticationProvider(repository);
	}

	@Test
	@DisplayName("should support OAuth2ClientAuthenticationToken")
	void supportsOAuth2ClientAuthenticationToken() {
		assertThat(provider.supports(OAuth2ClientAuthenticationToken.class)).isTrue();
	}

	@Test
	@DisplayName("should return null for non-NONE authentication method")
	void returnsNullForSecretBasicMethod() {
		final var authentication = new OAuth2ClientAuthenticationToken(
				"kfg-client", ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret", null);

		assertThat(provider.authenticate(authentication)).isNull();
	}

	@Test
	@DisplayName("should throw invalid_client when client is not found in the repository")
	void throwsWhenClientNotFound() {
		doReturn(null).when(repository).findByClientId(WORKLOAD_CLIENT_ID.get());

		final var authentication = unauthenticatedToken(WORKLOAD_CLIENT_ID.get());

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> provider.authenticate(authentication))
				.satisfies(ex -> assertThat(ex.getError().getErrorCode())
						.isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT));
	}

	@Test
	@DisplayName("should return null when registered client is not a WORKLOAD type")
	void returnsNullForAgentClient() {
		final var client = TestClients.clientRegistration(AGENT_CLIENT_ID).build();
		doReturn(client).when(repository).findByClientId(AGENT_CLIENT_ID.get());

		final var authentication = unauthenticatedToken(AGENT_CLIENT_ID.get());

		assertThat(provider.authenticate(authentication)).isNull();
	}

	@Test
	@DisplayName("should return null when registered client has no CLIENT_TYPE setting")
	void returnsNullWhenClientTypeMissing() {
		final var client = RegisteredClient.withId("test-id")
				.clientId(WORKLOAD_CLIENT_ID.get())
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.clientSettings(ClientSettings.builder().build())
				.scope("namespaces")
				.build();

		doReturn(client).when(repository).findByClientId(WORKLOAD_CLIENT_ID.get());

		final var authentication = unauthenticatedToken(WORKLOAD_CLIENT_ID.get());

		assertThat(provider.authenticate(authentication)).isNull();
	}

	@Test
	@DisplayName("should throw invalid_client when WORKLOAD client does not support NONE method")
	void throwsWhenNoneMethodNotRegistered() {
		final var client = RegisteredClient.withId("test-id")
				.clientId(WORKLOAD_CLIENT_ID.get())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.clientSettings(ClientSettings.builder()
						.setting(NamespaceClientSettingNames.NAMESPACE, NAMESPACE)
						.setting(NamespaceClientSettingNames.CLIENT_TYPE, NamespaceClientType.WORKLOAD)
						.build())
				.scope("namespaces")
				.build();

		doReturn(client).when(repository).findByClientId(WORKLOAD_CLIENT_ID.get());

		final var authentication = unauthenticatedToken(WORKLOAD_CLIENT_ID.get());

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> provider.authenticate(authentication))
				.satisfies(ex -> assertThat(ex.getError().getErrorCode())
						.isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT));
	}

	@Test
	@DisplayName("should return authenticated token for a valid WORKLOAD client")
	void authenticatesValidWorkloadClient() {
		final var client = TestClients.clientRegistration(WORKLOAD_CLIENT_ID)
				.scope("namespaces")
				.build();

		doReturn(client).when(repository).findByClientId(WORKLOAD_CLIENT_ID.get());

		final var result = provider.authenticate(unauthenticatedToken(WORKLOAD_CLIENT_ID.get()));

		assertThat(result)
				.isNotNull()
				.isInstanceOf(OAuth2ClientAuthenticationToken.class)
				.asInstanceOf(InstanceOfAssertFactories.type(OAuth2ClientAuthenticationToken.class))
				.returns(true, AbstractAuthenticationToken::isAuthenticated)
				.returns(client, OAuth2ClientAuthenticationToken::getRegisteredClient)
				.returns(ClientAuthenticationMethod.NONE, OAuth2ClientAuthenticationToken::getClientAuthenticationMethod);
	}

	private static OAuth2ClientAuthenticationToken unauthenticatedToken(String clientId) {
		return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, null);
	}

}
