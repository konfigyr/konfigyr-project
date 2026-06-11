package com.konfigyr.identity.authorization.workload;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.WorkloadIdentityStubs;
import com.konfigyr.identity.authorization.NamespaceClientSettingNames;
import com.konfigyr.identity.authorization.issuer.TrustedIssuer;
import com.konfigyr.identity.authorization.issuer.TrustedIssuerRepository;
import com.konfigyr.test.OAuth2AccessTokens;
import com.nimbusds.jwt.JWTClaimsSet;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadTokenExchangeAuthenticationProviderTest {

	@RegisterExtension
	static WireMockExtension wiremock = WireMockExtension.newInstance()
			.options(WireMockConfiguration.wireMockConfig().dynamicPort())
			.configureStaticDsl(true)
			.build();

	static final String SUBJECT = "repo:acme/api:ref:refs/heads/main";
	static final EntityId NAMESPACE = EntityId.from(7L);

	static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
	static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

	@Mock
	TrustedIssuerRepository trustedIssuerRepository;

	@Mock
	OAuth2AuthorizationService authorizationService;

	@Mock
	OAuth2TokenGenerator<OAuth2Token> tokenGenerator;

	@Mock(strictness = Mock.Strictness.LENIENT)
	AuthorizationServerContext authorizationServerContext;

	WorkloadIdentityStubs identityStubs;
	WorkloadTokenExchangeAuthenticationProvider provider;

	@BeforeEach
	void setup() {
		provider = new WorkloadTokenExchangeAuthenticationProvider(
				trustedIssuerRepository, authorizationService, tokenGenerator);

		final var settings = AuthorizationServerSettings.builder()
				.issuer(wiremock.baseUrl())
				.build();

		doReturn(wiremock.baseUrl()).when(authorizationServerContext).getIssuer();
		doReturn(settings).when(authorizationServerContext).getAuthorizationServerSettings();

		AuthorizationServerContextHolder.setContext(authorizationServerContext);

		identityStubs = new WorkloadIdentityStubs(wiremock.baseUrl(), "test-workload-identity-server");
		wiremock.addStubMapping(identityStubs.createMetadataStub());
		wiremock.addStubMapping(identityStubs.createKeysetStub());
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
		final var client = workloadClient(identityStubs.issuerUri(), null);
		final var exchange = tokenExchange(client, "any-token", ACCESS_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.isEqualTo("Unsupported subject_token_type: %s", ACCESS_TOKEN_TYPE);

		verify(trustedIssuerRepository, never()).lookup(any(), any());
	}

	@Test
	@DisplayName("should throw invalid_client when NAMESPACE setting is missing from client settings")
	void throwsWhenNamespaceMissing() {
		final var client = createClient(
				ClientSettings.builder()
						.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, identityStubs.issuerUri())
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

		verify(trustedIssuerRepository, never()).lookup(any(), any());
	}

	@Test
	@DisplayName("should throw invalid_client when issuer is not in the trusted repository")
	void throwsWhenIssuerNotTrusted() {
		doReturn(null).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());

		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), null), "any-token", JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_CLIENT)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.isEqualTo("Can't find any trusted issuer for this OAuth2 Client that matches this issuer URI: %s", identityStubs.issuerUri());
	}

	@Test
	@DisplayName("should throw invalid_request for a malformed subject_token that cannot be parsed as JWT")
	void throwsForMalformedSubjectToken() {
		doReturn(trustedIssuer(null)).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());

		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), null), "not-a-jwt", JWT_TOKEN_TYPE);

		// Decode fails at JWT parsing before any JWKS fetch
		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.extracting(OAuth2Error::getDescription, InstanceOfAssertFactories.STRING)
				.contains("Invalid subject_token", "Malformed token");

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should throw invalid_request when subject_token audience does not match trusted issuer")
	void throwsForAudienceMismatch() {
		// Trusted issuer requires "konfigyr-api" audience but token has a different one
		doReturn(trustedIssuer(Set.of("konfigyr-api"))).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());

		final var token = signToken(SUBJECT, Instant.now().plusSeconds(300), "wrong-audience");
		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), null), token, JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.returns("Subject token audience does not match any of the required audiences", OAuth2Error::getDescription);

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should throw invalid_request when subject_token sub claim does not match the configured pattern")
	void throwsForSubjectPatternMismatch() {
		doReturn(trustedIssuer(null)).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());

		// Client requires sub to match "repo:acme/*" but the token has a different subject
		final var token = signToken("repo:other/repo:ref:refs/heads/main", Instant.now().plusSeconds(300));
		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), "repo:acme/.*"), token, JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.INVALID_REQUEST)
				.returns("Subject token sub claim does not match the required pattern", OAuth2Error::getDescription);

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should throw server_error when the token generator returns null")
	void throwsWhenTokenGeneratorReturnsNull() {
		doReturn(trustedIssuer(null)).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());
		doReturn(null).when(tokenGenerator).generate(any());

		final var token = signToken(SUBJECT, Instant.now().plusSeconds(300));
		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), null), token, JWT_TOKEN_TYPE);

		assertOAuthError(exchange, OAuth2ErrorCodes.SERVER_ERROR)
				.returns("Unexpected error occurred while generating the access token", OAuth2Error::getDescription);

		verify(authorizationService, never()).save(any());
	}

	@Test
	@DisplayName("should issue access token with requested scopes on successful exchange")
	void issuesAccessTokenWithRequestedScopes() {
		doReturn(trustedIssuer(null)).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());
		doReturn(mockAccessToken("namespaces")).when(tokenGenerator).generate(any());

		final var token = signToken(SUBJECT, Instant.now().plusSeconds(300));
		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), null), token, JWT_TOKEN_TYPE, "namespaces");

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
		doReturn(trustedIssuer(null)).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());
		doReturn(mockAccessToken("namespaces", "namespaces:read")).when(tokenGenerator).generate(any());

		final var token = signToken(SUBJECT, Instant.now().plusSeconds(300));

		// Client has two scopes; exchange requests none — should use both
		final var client = RegisteredClient.withId("multi-scope")
				.clientId("kfg-multi-scope")
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
				.clientSettings(ClientSettings.builder()
						.setting(NamespaceClientSettingNames.NAMESPACE, NAMESPACE)
						.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, identityStubs.issuerUri())
						.build())
				.scope("namespaces")
				.scope("namespaces:read")
				.build();

		final var exchange = tokenExchange(client, token, JWT_TOKEN_TYPE);
		final var result = (OAuth2AccessTokenAuthenticationToken) provider.authenticate(exchange);

		assertThat(result.getAccessToken().getScopes())
				.containsExactlyInAnyOrder("namespaces", "namespaces:read");
	}

	@Test
	@DisplayName("should throw invalid_scope when requested scope is not registered on the client")
	void throwsForUnregisteredScope() {
		doReturn(trustedIssuer(null)).when(trustedIssuerRepository).lookup(NAMESPACE, identityStubs.issuerUri());

		final var token = signToken(SUBJECT, Instant.now().plusSeconds(300));
		final var exchange = tokenExchange(workloadClient(identityStubs.issuerUri(), null), token, JWT_TOKEN_TYPE, "vaults:write");

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

	private TrustedIssuer trustedIssuer(Set<String> allowedAudiences) {
		return new TrustedIssuer(identityStubs.issuerUri(), "Test Issuer", identityStubs.keysetUri(), allowedAudiences != null ? allowedAudiences : Set.of());
	}

	private String signToken(String subject, Instant expiry, String... audiences) {
		final var claims = new JWTClaimsSet.Builder()
				.subject(subject)
				.expirationTime(Date.from(expiry));

		if (audiences.length > 0) {
			claims.audience(List.of(audiences));
		}

		return identityStubs.issue(claims);
	}

	private static RegisteredClient workloadClient(String issuerUri, String subjectPattern) {
		final var settings = ClientSettings.builder()
				.setting(NamespaceClientSettingNames.NAMESPACE, NAMESPACE)
				.setting(NamespaceClientSettingNames.WORKLOAD_ISSUER_URI, issuerUri);

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
