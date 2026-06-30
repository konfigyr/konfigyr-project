package com.konfigyr.identity.authorization.workload;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.identity.authorization.issuer.TrustedIssuer;
import com.konfigyr.identity.authorization.issuer.TrustedIssuerRegistration;
import com.konfigyr.identity.authorization.issuer.TrustedIssuerRegistry;
import com.konfigyr.test.OAuth2AccessTokens;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenExchangeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadTokenExchangeAuthenticationProviderTest {

	static final String SUBJECT = "repo:acme/api:ref:refs/heads/main";
	static final String ISSUER_URI = "https://token.actions.githubusercontent.com";
	static final EntityId NAMESPACE = EntityId.from(7L);

	static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
	static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

	@Mock
	TrustedIssuerRegistry registry;

	@Mock
	JwtDecoder jwtDecoder;

	@Mock(strictness = Mock.Strictness.LENIENT)
	Jwt jwt;

	@Mock
	OAuth2AuthorizationService authorizationService;

	@Mock
	OAuth2TokenGenerator<OAuth2Token> tokenGenerator;

	@Mock(strictness = Mock.Strictness.LENIENT)
	AuthorizationServerContext authorizationServerContext;

	WorkloadTokenExchangeAuthenticationProvider provider;

	@BeforeEach
	void setup() {
		provider = new WorkloadTokenExchangeAuthenticationProvider(
				registry, authorizationService, tokenGenerator);

		doReturn(SUBJECT).when(jwt).getSubject();

		final var settings = AuthorizationServerSettings.builder()
				.issuer("https://auth.konfigyr.com")
				.build();

		doReturn("https://auth.konfigyr.com").when(authorizationServerContext).getIssuer();
		doReturn(settings).when(authorizationServerContext).getAuthorizationServerSettings();

		AuthorizationServerContextHolder.setContext(authorizationServerContext);
	}

	@AfterEach
	void clearAuthorizationServerContext() {
		AuthorizationServerContextHolder.resetContext();
	}

	@Test
	@DisplayName("should support OAuth2TokenExchangeAuthenticationToken")
	void supportsTokenExchangeAuthenticationToken() {
		assertThat(provider.supports(OAuth2TokenExchangeAuthenticationToken.class)).isTrue();
		assertThat(provider.supports(OAuth2AccessTokenAuthenticationToken.class)).isFalse();
	}

	@Test
	@DisplayName("should throw invalid_request for unsupported subject_token_type")
	void throwsForUnsupportedSubjectTokenType() {
		final var client = workloadClient(null);
		final var exchange = tokenExchange(client, "any-token", ACCESS_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.isEqualTo("Unsupported subject_token_type: %s", ACCESS_TOKEN_TYPE);

		verify(registry, never()).get(any(), any());
	}

	@Test
	@DisplayName("should throw invalid_client when NAMESPACE setting is missing from client settings")
	void throwsWhenNamespaceMissing() {
		final var client = createClient(
				ClientSettings.builder()
						.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, ISSUER_URI)
						.build()
		);

		final var exchange = tokenExchange(client, "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_CLIENT)
				.returns(null, OAuth2Error::getDescription);
	}

	@Test
	@DisplayName("should throw invalid_client when issuer URI is not configured on the client")
	void throwsWhenIssuerUriMissing() {
		final var client = createClient(
				ClientSettings.builder()
						.setting(NamespaceClientSettingNames.NAMESPACE, NAMESPACE)
						.build()
		);

		final var exchange = tokenExchange(client, "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_CLIENT)
				.returns("Trusted issuer is not specified for this OAuth2 Client", OAuth2Error::getDescription);

		verify(registry, never()).get(any(), any());
	}

	@Test
	@DisplayName("should throw invalid_client when issuer is not in the trusted registry")
	void throwsWhenIssuerNotTrusted() {
		doThrow(new OAuth2AuthenticationException(new OAuth2Error(
				OAuth2ErrorCodes.INVALID_CLIENT,
				"Can't find any trusted issuer for this OAuth2 Client that matches this issuer URI: " + ISSUER_URI,
				null
		))).when(registry).get(NAMESPACE, ISSUER_URI);

		final var exchange = tokenExchange(workloadClient(null), "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_CLIENT)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.isEqualTo("Can't find any trusted issuer for this OAuth2 Client that matches this issuer URI: %s", ISSUER_URI);
	}

	@Test
	@DisplayName("should throw invalid_request for a malformed subject_token that cannot be parsed as JWT")
	void throwsForMalformedSubjectToken() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);
		doThrow(new JwtException("Malformed token")).when(jwtDecoder).decode("not-a-jwt");

		final var exchange = tokenExchange(workloadClient(null), "not-a-jwt", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.contains("Invalid subject_token", "Malformed token");

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should throw invalid_request when subject_token audience does not match trusted issuer")
	void throwsForAudienceMismatch() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);

		final var audError = new OAuth2Error("invalid_token", "The aud claim is not valid", null);
		doThrow(new JwtValidationException("Validation failed", List.of(audError)))
				.when(jwtDecoder).decode(any());

		final var exchange = tokenExchange(workloadClient(null), "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.contains("Invalid subject_token: The aud claim is not valid");

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should throw invalid_request when subject_token sub claim does not match the configured pattern")
	void throwsForSubjectPatternMismatch() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);
		doReturn(jwt).when(jwtDecoder).decode(any());
		doReturn("repo:other/repo:ref:refs/heads/main").when(jwt).getSubject();

		final var exchange = tokenExchange(workloadClient("repo:acme/.*"), "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.returns("Subject token sub claim does not match the required pattern", OAuth2Error::getDescription);

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should throw server_error when the token generator returns null")
	void throwsWhenTokenGeneratorReturnsNull() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);
		doReturn(jwt).when(jwtDecoder).decode(any());
		doReturn(null).when(tokenGenerator).generate(any());

		final var exchange = tokenExchange(workloadClient(null), "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.SERVER_ERROR)
				.returns("Unexpected error occurred while generating the access token", OAuth2Error::getDescription);

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should issue access token with requested scopes on successful exchange")
	void issuesAccessTokenWithRequestedScopes() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);
		doReturn(jwt).when(jwtDecoder).decode(any());
		doReturn(mockAccessToken("namespaces")).when(tokenGenerator).generate(any());

		final var exchange = tokenExchange(workloadClient(null), "any-token", JWT_TOKEN_TYPE, "namespaces");
		final var result = provider.authenticate(exchange);

		assertThat(result)
				.isNotNull()
				.isInstanceOf(OAuth2AccessTokenAuthenticationToken.class);

		final var authentication = (OAuth2AccessTokenAuthenticationToken) result;

		assertThat(authentication.getAccessToken().getScopes())
				.containsExactly("namespaces");

		assertThat(authentication.getAdditionalParameters())
				.containsKey(OAuth2ParameterNames.ISSUED_TOKEN_TYPE);

		final var captor = ArgumentCaptor.forClass(OAuth2Authorization.class);
		verify(authorizationService).save(captor.capture());

		assertThat(captor.getValue())
				.returns(SUBJECT, OAuth2Authorization::getPrincipalName)
				.returns(AuthorizationGrantType.TOKEN_EXCHANGE, OAuth2Authorization::getAuthorizationGrantType);
	}

	@Test
	@DisplayName("should fall back to all client scopes when none are requested in the exchange")
	void usesClientScopesWhenNoneRequested() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);
		doReturn(jwt).when(jwtDecoder).decode(any());
		doReturn(mockAccessToken("namespaces", "namespaces:read")).when(tokenGenerator).generate(any());

		final var client = RegisteredClient.withId("multi-scope")
				.clientId("kfg-multi-scope")
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.clientSettings(ClientSettings.builder()
						.setting(NamespaceClientSettingNames.NAMESPACE, NAMESPACE)
						.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, ISSUER_URI)
						.build())
				.scope("namespaces")
				.scope("namespaces:read")
				.build();

		final var exchange = tokenExchange(client, "any-token", JWT_TOKEN_TYPE);
		final var result = (OAuth2AccessTokenAuthenticationToken) provider.authenticate(exchange);

		assertThat(result.getAccessToken().getScopes())
				.containsExactlyInAnyOrder("namespaces", "namespaces:read");
	}

	@Test
	@DisplayName("should throw invalid_scope when requested scope is not registered on the client")
	void throwsForUnregisteredScope() {
		doReturn(trustedIssuer()).when(registry).get(NAMESPACE, ISSUER_URI);
		doReturn(jwt).when(jwtDecoder).decode(any());

		final var exchange = tokenExchange(workloadClient(null), "any-token", JWT_TOKEN_TYPE, "vaults:write");

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_SCOPE)
				.returns(null, OAuth2Error::getDescription);

		verify(authorizationService, never()).save(any());
	}

	private ObjectAssert<OAuth2Error> assertOAuthError(OAuth2TokenExchangeAuthenticationToken exchange, String expectedErrorCode) {
		return assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> provider.authenticate(exchange))
				.extracting(OAuth2AuthenticationException::getError, InstanceOfAssertFactories.type(OAuth2Error.class))
				.returns(expectedErrorCode, OAuth2Error::getErrorCode);
	}

	private TrustedIssuer trustedIssuer() {
		final var registration = TrustedIssuerRegistration.withId("test-issuer")
				.issuerUri(ISSUER_URI)
				.name("Test Issuer")
				.build();
		return new TrustedIssuer(registration, jwtDecoder);
	}

	private static RegisteredClient workloadClient(String subjectPattern) {
		final var settings = ClientSettings.builder()
				.setting(NamespaceClientSettingNames.NAMESPACE, NAMESPACE)
				.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, ISSUER_URI);

		if (subjectPattern != null) {
			settings.setting(NamespaceClientSettingNames.WORKLOAD_SUBJECT_PATTERN, subjectPattern);
		}

		return createClient(settings.build());
	}

	private static RegisteredClient createClient(ClientSettings settings) {
		return RegisteredClient.withId("test-workload-client")
				.clientId("kfg-test-workload-client")
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.clientSettings(settings)
				.scope("namespaces")
				.build();
	}

	private static OAuth2TokenExchangeAuthenticationToken tokenExchange(
			RegisteredClient client,
			String subjectToken,
			String subjectTokenType,
			String... scopes
	) {
		return new OAuth2TokenExchangeAuthenticationToken(
				"urn:ietf:params:oauth:token-type:access_token",
				subjectToken,
				subjectTokenType,
				new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null),
				null,
				null,
				null,
				null,
				Set.of(scopes),
				Map.of()
		);
	}

	private static OAuth2Token mockAccessToken(String... scopes) {
		return OAuth2AccessTokens.createAccessToken("test-access-token-value", Duration.ofSeconds(1800), scopes);
	}

}
