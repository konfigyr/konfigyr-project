package com.konfigyr.identity;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.konfigyr.crypto.KeysetStore;
import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityService;
import com.konfigyr.identity.authentication.rememberme.AccountRememberMeServices;
import com.konfigyr.identity.authorization.AuthorizationFailureHandler;
import com.konfigyr.identity.authorization.AuthorizationServerScopes;
import com.konfigyr.identity.authorization.issuer.TrustedIssuerRegistration;
import com.konfigyr.identity.authorization.jwk.KeysetSource;
import com.nimbusds.jwt.JWTClaimsSet;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadata;
import org.springframework.security.oauth2.server.authorization.http.converter.OAuth2AuthorizationServerMetadataHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.oidc.OidcProviderConfiguration;
import org.springframework.security.oauth2.server.authorization.oidc.http.converter.OidcProviderConfigurationHttpMessageConverter;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@EnableWireMock
class AuthorizationServerIntegrationTest extends AbstractControllerIntegrationTest {

	@InjectWireMock
	static WireMockServer wireMock;

	@Autowired
	AccountIdentityService identityService;

	@Autowired
	AccountRememberMeServices rememberMeServices;

	@Autowired
	KeysetStore keysets;

	@Autowired
	KeysetSource keysetSource;

	WorkloadIdentityStubs workloadIdentityStubs;

	@BeforeEach
	void rotate() {
		// rotate the JWK keyset that signs the tokens and reload the configured JWK source
		// Why, you ask? To make sure the OIDC server works when a new key is generated
		final var keyset = keysets.read(KonfigyrIdentityKeysets.WEB_KEYS.getName());
		keysets.rotate(keyset);
		keysetSource.afterPropertiesSet();
	}

	@Test
	@DisplayName("should retrieve OIDC metadata")
	void authorizationServerOidcMetadata() {
		mvc.withHttpMessageConverters(List.of(new OidcProviderConfigurationHttpMessageConverter()))
				.get()
				.uri("/.well-known/openid-configuration")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "unknown")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(OidcProviderConfiguration.class)
				.satisfies(assertUri("", OidcProviderConfiguration::getIssuer))
				.satisfies(assertUri("/oauth/authorize", OidcProviderConfiguration::getAuthorizationEndpoint))
				.satisfies(assertUri("/oauth/token", OidcProviderConfiguration::getTokenEndpoint))
				.satisfies(assertUri("/oauth/jwks", OidcProviderConfiguration::getJwkSetUrl))
				.satisfies(assertUri("/oauth/userinfo", OidcProviderConfiguration::getUserInfoEndpoint))
				.satisfies(assertUri("/oauth/revoke", OidcProviderConfiguration::getTokenRevocationEndpoint))
				.satisfies(assertUri("/oauth/introspect", OidcProviderConfiguration::getTokenIntrospectionEndpoint))
				.satisfies(assertUri("/connect/logout", OidcProviderConfiguration::getEndSessionEndpoint))
				.satisfies(assertElements(
						OidcProviderConfiguration::getGrantTypes,
						AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
						AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
						AuthorizationGrantType.REFRESH_TOKEN.getValue(),
						AuthorizationGrantType.TOKEN_EXCHANGE.getValue()
				))
				.satisfies(assertElements(OidcProviderConfiguration::getResponseTypes, "code"))
				.satisfies(assertElements(OidcProviderConfiguration::getCodeChallengeMethods, "S256"))
				.satisfies(assertElements(
						OidcProviderConfiguration::getTokenEndpointAuthenticationMethods,
						ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(),
						ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue(),
						ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue(),
						ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue(),
						ClientAuthenticationMethod.TLS_CLIENT_AUTH.getValue(),
						ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH.getValue()
				))
				.satisfies(assertOAuthScopes(OidcProviderConfiguration::getScopes))
				.satisfies(assertElements(OidcProviderConfiguration::getIdTokenSigningAlgorithms, "RS256", "PS256"))
				.satisfies(assertElements(
						OidcProviderConfiguration::getDPoPSigningAlgorithms,
						"RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512"
				));
	}

	@Test
	@DisplayName("should retrieve OAuth2 metadata")
	void authorizationServerOAuthMetadata() {
		mvc.withHttpMessageConverters(List.of(new OAuth2AuthorizationServerMetadataHttpMessageConverter()))
				.get()
				.uri("/.well-known/oauth-authorization-server")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "unknown")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.convertTo(OAuth2AuthorizationServerMetadata.class)
				.satisfies(assertUri("", OAuth2AuthorizationServerMetadata::getIssuer))
				.satisfies(assertUri("/oauth/authorize", OAuth2AuthorizationServerMetadata::getAuthorizationEndpoint))
				.satisfies(assertUri("/oauth/token", OAuth2AuthorizationServerMetadata::getTokenEndpoint))
				.satisfies(assertUri("/oauth/jwks", OAuth2AuthorizationServerMetadata::getJwkSetUrl))
				.satisfies(assertUri("/oauth/revoke", OAuth2AuthorizationServerMetadata::getTokenRevocationEndpoint))
				.satisfies(assertUri("/oauth/introspect", OAuth2AuthorizationServerMetadata::getTokenIntrospectionEndpoint))
				.satisfies(assertElements(
						OAuth2AuthorizationServerMetadata::getGrantTypes,
						AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
						AuthorizationGrantType.CLIENT_CREDENTIALS.getValue(),
						AuthorizationGrantType.REFRESH_TOKEN.getValue(),
						AuthorizationGrantType.TOKEN_EXCHANGE.getValue()
				))
				.satisfies(assertElements(OAuth2AuthorizationServerMetadata::getResponseTypes, "code"))
				.satisfies(assertElements(OAuth2AuthorizationServerMetadata::getCodeChallengeMethods, "S256"))
				.satisfies(assertElements(
						OAuth2AuthorizationServerMetadata::getTokenEndpointAuthenticationMethods,
						ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue(),
						ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue(),
						ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue(),
						ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue(),
						ClientAuthenticationMethod.TLS_CLIENT_AUTH.getValue(),
						ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH.getValue()
				))
				.satisfies(assertElements(
						OAuth2AuthorizationServerMetadata::getDPoPSigningAlgorithms,
						"RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512"
				))
				.satisfies(assertOAuthScopes(OAuth2AuthorizationServerMetadata::getScopes));
	}

	@Test
	@DisplayName("should fail to authorize request for unknown client")
	void authorizationForUnknownClient() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "unknown")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(assertOAuthError(OAuth2ErrorCodes.INVALID_REQUEST));
	}

	@Test
	@DisplayName("should fail to authorize request for unsupported response_type")
	void authorizationForUnsupportedResponseType() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "invalid")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(assertOAuthError(OAuth2ErrorCodes.UNSUPPORTED_RESPONSE_TYPE));
	}

	@Test
	@DisplayName("should fail to authorize request for unsupported redirect_uri")
	void authorizationForUnsupportedRedirectUri() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(assertOAuthError(OAuth2ErrorCodes.INVALID_REQUEST));
	}

	@Test
	@DisplayName("should fail to authorize request for unsupported scope")
	void authorizationForUnsupportedScope() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.CLIENT_SECRET, "secret")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "unsupported_scope")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/oauth/client/code")
						.hasParameter(OAuth2ParameterNames.ERROR, OAuth2ErrorCodes.INVALID_SCOPE)
						.hasParameter(OAuth2ParameterNames.STATE, "state")
				));
	}

	@Test
	@DisplayName("should fail to authorize Agent namespace client request without code_challenge")
	void authorizationForNamespaceClientWithoutPkce() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "namespaces")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost:56789/callback")
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/callback")
						.hasParameter(OAuth2ParameterNames.ERROR, OAuth2ErrorCodes.INVALID_REQUEST)
						.hasParameter(OAuth2ParameterNames.ERROR_DESCRIPTION, "OAuth 2.0 Parameter: code_challenge")
						.hasParameter(OAuth2ParameterNames.STATE, "state")
				));
	}

	@Test
	@DisplayName("should deny authorization code to user who is not a member of the namespace")
	void authorizationDeniedForNonMember()  {
		final var verifier = PkceGenerator.generateCodeVerifier();

		// jane (id=2) is NOT a member of namespace_id=1 (Agent app)
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "kfg-AQIAAAAAAAAAAQAAAABqJ8Ep0uAEZ3m_IJpKeL8_2Zk")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "namespaces")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(PkceParameterNames.CODE_CHALLENGE, PkceGenerator.generateCodeChallenge(verifier))
				.queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://127.0.0.1:3000/callback")
				.with(rememberMe(rememberMeServices, identityService, 2))
				.exchange()
				.assertThat()
				.apply(log())
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/callback")
						.hasParameter(OAuth2ParameterNames.ERROR, OAuth2ErrorCodes.ACCESS_DENIED)
						.hasParameter(OAuth2ParameterNames.STATE, "state")
				));
	}

	@Test
	@DisplayName("should issue authorization code and embed namespace claim for namespace member")
	void authorizationCodeIssuedForMemberUsingAgentClient()  {
		final var verifier = PkceGenerator.generateCodeVerifier();
		final var session = new MockHttpSession();

		MvcTestResult result = mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "namespaces")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(PkceParameterNames.CODE_CHALLENGE, PkceGenerator.generateCodeChallenge(verifier))
				.queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/callback")
				.with(rememberMe(rememberMeServices, identityService, 1))
				.session(session)
				.exchange();

		// redirected to the OAuth consents page
		result.assertThat()
				.apply(log())
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/oauth/consents")
						.hasParameter(OAuth2ParameterNames.CLIENT_ID, "kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY")
						.hasParameter(OAuth2ParameterNames.SCOPE, "namespaces")
				));

		// send the consents and continue the OAuth authorization process
		result = mvc.post()
				.uri("/oauth/authorize")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.formFields(extractUriParameters(result.getResponse().getRedirectedUrl()))
				.with(rememberMe(rememberMeServices, identityService, 1))
				.session(session)
				.exchange();

		result.assertThat()
				.apply(log())
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasScheme("http")
						.hasHost("localhost")
						.hasPath("/callback")
						.hasParameter(OAuth2ParameterNames.STATE, "state")
						.hasParameter(OAuth2ParameterNames.CODE)
				));

		final String code = extractUriParameters(result.getResponse().getRedirectedUrl())
				.getFirst(OAuth2ParameterNames.CODE);

		mvc.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/callback")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(PkceParameterNames.CODE_VERIFIER, verifier)
				.param(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.param(OAuth2ParameterNames.CODE, code)
				.formField(OAuth2ParameterNames.CLIENT_ID, "kfg-AQIAAAAAAAAAAgAAAABqJToWEdz4oFXK7fpp_88p_yY")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.access_token", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.scope", it -> it.assertThat().isEqualTo("namespaces"));
	}

	@Test
	@DisplayName("should fail to obtain token for unknown client")
	void tokenForUnknownClient() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(OAuth2ParameterNames.CODE, "some authorization code")
				.with(httpBasic("unknown", "secret"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_CLIENT));
	}

	@Test
	@DisplayName("should fail to obtain token for client that had expired")
	void tokenForExpiringClient() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
				.param(OAuth2ParameterNames.SCOPE, "namespaces")
				.with(httpBasic(
						"kfg-AQEAAAAAAAAAAgAAAABqJToWfXkWbVML9iZbEPVai4o",
						"10S6cd0JgdO6WCLmOLB46d-Enx7K20hKSF1qicfev5g"
				))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_CLIENT));
	}

	@Test
	@DisplayName("should fail to obtain token for invalid client credentials")
	void tokenForInvalidClientCredentials() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(OAuth2ParameterNames.CODE, "some authorization code")
				.with(httpBasic("konfigyr", "wrong secret"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_CLIENT));
	}

	@Test
	@DisplayName("should fail to obtain token for unsupported grant type")
	void tokenForInvalidGrantType() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "password")
				.param(OAuth2ParameterNames.CODE, "some authorization code")
				.with(httpBasic("konfigyr", "secret"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE));
	}

	@Test
	@DisplayName("should fail to obtain token for invalid authorization code")
	void tokenForInvalidAuthorizationCode() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(OAuth2ParameterNames.CODE, "some authorization code")
				.with(httpBasic("konfigyr", "secret"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_GRANT));
	}

	@Test
	@DisplayName("should fail to authorize request without code_challenge request parameter")
	void requireCodeChallenge()  {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus3xxRedirection()
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/oauth/client/code")
						.hasParameter(OAuth2ParameterNames.ERROR, OAuth2ErrorCodes.INVALID_REQUEST)
						.hasParameter(OAuth2ParameterNames.ERROR_DESCRIPTION, "OAuth 2.0 Parameter: code_challenge")
						.hasParameter(OAuth2ParameterNames.ERROR_URI, "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1")
				));
	}

	@Test
	@DisplayName("should fail to authorize request without code_challenge request parameter")
	void requireCodeChallengeMethod()  {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.queryParam(PkceParameterNames.CODE_CHALLENGE, "a challenge")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus3xxRedirection()
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/oauth/client/code")
						.hasParameter(OAuth2ParameterNames.ERROR, OAuth2ErrorCodes.INVALID_REQUEST)
						.hasParameter(OAuth2ParameterNames.ERROR_DESCRIPTION, "OAuth 2.0 Parameter: code_challenge_method")
						.hasParameter(OAuth2ParameterNames.ERROR_URI, "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1")
				));
	}

	@Test
	@DisplayName("should authorize request and redirect to login page when principal is not authenticated")
	void redirectToLoginWhenNotAuthenticated()  {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.queryParam(PkceParameterNames.CODE_CHALLENGE, "pkce-challenge")
				.queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("/login")
				.request()
				.sessionAttributes()
				.hasEntrySatisfying("SPRING_SECURITY_SAVED_REQUEST", it -> assertThat(it)
						.isInstanceOf(SavedRequest.class)
						.asInstanceOf(InstanceOfAssertFactories.type(SavedRequest.class))
						.returns("GET", SavedRequest::getMethod)
						.extracting(SavedRequest::getRedirectUrl)
						.isNotNull()
						.extracting(URI::create, InstanceOfAssertFactories.URI_TYPE)
						.hasScheme("http")
						.hasHost("localhost")
						.hasNoPort()
						.hasPath("/oauth/authorize")
						.hasParameter(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
						.hasParameter(OAuth2ParameterNames.RESPONSE_TYPE, "code")
						.hasParameter(OAuth2ParameterNames.SCOPE, "openid")
						.hasParameter(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				);
	}

	@Test
	@DisplayName("should issue OAuth Access Token for authorization_code grant type")
	void issueAccessTokenForAuthorizationCode()  {
		final var verifier = PkceGenerator.generateCodeVerifier();

		final var result = mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(PkceParameterNames.CODE_CHALLENGE, PkceGenerator.generateCodeChallenge(verifier))
				.queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.with(rememberMe(rememberMeServices, identityService, 1))
				.exchange();

		result.assertThat()
				.apply(log())
				.satisfies(it -> it.assertThat()
						.cookies()
						.doesNotContainCookie("konfigyr.account")
				)
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/oauth/client/code")
						.hasParameter(OAuth2ParameterNames.STATE, "state")
						.hasParameter(OAuth2ParameterNames.CODE)
				));

		assertThat(result.getResponse().getRedirectedUrl())
				.isNotNull();

		final String code = extractUriParameters(result.getResponse().getRedirectedUrl())
				.getFirst(OAuth2ParameterNames.CODE);

		mvc.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(PkceParameterNames.CODE_VERIFIER, verifier)
				.param(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.param(OAuth2ParameterNames.CODE, code)
				.with(httpBasic("konfigyr", "secret"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.token_type", it -> it.assertThat()
						.isEqualTo(OAuth2AccessToken.TokenType.BEARER.getValue())
				)
				.hasPathSatisfying("$.scope", it -> it.assertThat()
						.isEqualTo("openid")
				)
				.hasPathSatisfying("$.expires_in", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.access_token", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.refresh_token", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.id_token", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.access_token", it -> mvc.post()
						.uri("/oauth/introspect")
						.param("token", it.assertThat().actual().toString())
						.with(httpBasic("konfigyr", "secret"))
						.exchange()
						.assertThat()
						.hasStatus(HttpStatus.OK)
				)
				.hasPathSatisfying("$.refresh_token", it -> mvc.post()
						.uri("/oauth/token")
						.param(OAuth2ParameterNames.GRANT_TYPE, "refresh_token")
						.param("refresh_token", it.assertThat().actual().toString())
						.with(httpBasic("konfigyr", "secret"))
						.exchange()
						.assertThat()
						.hasStatus(HttpStatus.OK)
				);
	}

	@Test
	@DisplayName("should fail to issue OAuth Access Token when invalid PKCE verifier is sent")
	void tokenForInvalidCodeVerifier()  {
		final var verifier = PkceGenerator.generateCodeVerifier();

		final var result = mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(PkceParameterNames.CODE_CHALLENGE, PkceGenerator.generateCodeChallenge(verifier))
				.queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.with(rememberMe(rememberMeServices, identityService, 1))
				.exchange();

		result.assertThat()
				.apply(log())
				.satisfies(it -> it.assertThat()
						.cookies()
						.doesNotContainCookie("konfigyr.account")
				)
				.satisfies(assertRedirect(uri -> assertThat(uri)
						.hasPath("/oauth/client/code")
						.hasParameter(OAuth2ParameterNames.STATE, "state")
						.hasParameter(OAuth2ParameterNames.CODE)
				));

		assertThat(result.getResponse().getRedirectedUrl())
				.isNotNull();

		final String code = extractUriParameters(result.getResponse().getRedirectedUrl())
				.getFirst(OAuth2ParameterNames.CODE);

		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(PkceParameterNames.CODE_VERIFIER, "invalid verifier")
				.param(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
				.param(OAuth2ParameterNames.CODE, code)
				.with(httpBasic("konfigyr", "secret"))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_GRANT));
	}

	@Test
	@DisplayName("should issue OAuth Access Token for client_credentials grant type")
	void issueAccessTokenForClientCredentials()  {
		mvc.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
				.param(OAuth2ParameterNames.SCOPE, "namespaces")
				.with(httpBasic(
						"kfg-AQEAAAAAAAAAAQAAAABqJTlV2OXTvVveqnbXJ21wbPw",
						"n0obEPw2_5DoDNkxyXhW5Ul1TgC-t2r3H8_wj7PDqFc"
				))
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.token_type", it -> it.assertThat()
						.isEqualTo(OAuth2AccessToken.TokenType.BEARER.getValue())
				)
				.hasPathSatisfying("$.scope", it -> it.assertThat()
						.isEqualTo("namespaces")
				)
				.hasPathSatisfying("$.expires_in", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.access_token", it -> it.assertThat().isNotNull())
				.doesNotHavePath("$.id_token")
				.doesNotHavePath("$.refresh_token")
				.hasPathSatisfying("$.access_token", it -> mvc.post()
						.uri("/oauth/introspect")
						.param("token", it.assertThat().actual().toString())
						.with(httpBasic(
								"kfg-AQEAAAAAAAAAAQAAAABqJTlV2OXTvVveqnbXJ21wbPw",
								"n0obEPw2_5DoDNkxyXhW5Ul1TgC-t2r3H8_wj7PDqFc"
						))
						.exchange()
						.assertThat()
						.hasStatus(HttpStatus.OK)
				);
	}

	@Test
	@DisplayName("should fail to exchange Workload namespace client request without subject_token")
	void exchangeTokenWithoutSubjectToken() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.formField(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue())
				.formField(OAuth2ParameterNames.SCOPE, "namespaces")
				.formField(OAuth2ParameterNames.CLIENT_ID, "kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_REQUEST, error -> assertThat(error)
						.returns("OAuth 2.0 Parameter: subject_token", OAuth2Error::getDescription)
				));
	}

	@Test
	@DisplayName("should fail Workload token exchange when client is unknown")
	void workloadTokenExchangeWithUnknownClient() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.formField(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue())
				.formField(OAuth2ParameterNames.CLIENT_ID, "unknown")
				.formField("subject_token", "any.token.value")
				.formField("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_CLIENT));
	}

	@Test
	@DisplayName("should fail Workload token exchange when subject_token is not a valid JWT")
	void workloadTokenExchangeWithInvalidSubjectToken() {
		mvc.withHttpMessageConverters(List.of(new OAuth2ErrorHttpMessageConverter()))
				.post().uri("/oauth/token")
				.formField(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue())
				.formField(OAuth2ParameterNames.CLIENT_ID, "kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0")
				.formField("subject_token", "not-a-valid-jwt")
				.formField("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST)
				.satisfies(assertOAuthErrorResponse(OAuth2ErrorCodes.INVALID_REQUEST));
	}

	@Test
	@DisplayName("should issue Konfigyr access token for a valid workload identity token exchange")
	void workloadTokenExchangeSuccess() {
		// Register a mock OIDC discovery and JWKS endpoint for the workload identity stubs.
		workloadIdentityStubs = new WorkloadIdentityStubs(wireMock.baseUrl(), "workload-identity");
		wireMock.addStubMapping(workloadIdentityStubs.createMetadataStub());
		wireMock.addStubMapping(workloadIdentityStubs.createKeysetStub());

		// this issuer is configured for this client, we need to mock it to load
		// the JWK set from the `WorkloadIdentityStubs` to verify JWS signature
		final var configuredIssuerUri = "https://token.actions.githubusercontent.com";

		// The spy is configured to return a TrustedIssuer whose `jwksUri` points to WireMock,
		// so token validation never makes real network calls to external OIDC providers.
		doReturn(TrustedIssuerRegistration.withId(configuredIssuerUri)
				.name("Stubbed issuer")
				.issuerUri(workloadIdentityStubs.issuerUri())
				.jwksUri(workloadIdentityStubs.keysetUri())
				.build()).when(trustedIssuerRepository).lookup(EntityId.from(2), configuredIssuerUri);

		// Subject must satisfy the regex subjectPattern "repo:konfigyr/*:ref:refs/heads/main"
		// configured on the workload test client: "/*" matches zero or more trailing slashes,
		// so "repo:konfigyr/:ref:refs/heads/main" (no repo name segment) satisfies the pattern.
		final var subjectToken = workloadIdentityStubs.issue(
				new JWTClaimsSet.Builder()
						.subject("repo:konfigyr/:ref:refs/heads/main")
		);

		mvc.post().uri("/oauth/token")
				.formField(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue())
				.formField(OAuth2ParameterNames.CLIENT_ID, "kfg-AQMAAAAAAAAAAgAAAABqJToWpdAzsv7lni7oCvpjfb0")
				.formField("subject_token", subjectToken)
				.formField("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.token_type", it -> it.assertThat()
						.isEqualTo(OAuth2AccessToken.TokenType.BEARER.getValue()))
				.hasPathSatisfying("$.scope", it -> it.assertThat()
						.asInstanceOf(InstanceOfAssertFactories.STRING)
						.satisfies(scope -> assertThat(scope.split(" "))
								.containsExactlyInAnyOrder("namespaces:read", "namespaces:publish-releases")))
				.hasPathSatisfying("$.access_token", it -> it.assertThat().isNotNull())
				.doesNotHavePath("$.refresh_token")
				.doesNotHavePath("$.id_token");
	}

	static Consumer<MvcTestResult> assertOAuthError(String code) {
		return result -> {
			result.assertThat()
					.as("OAuth error should be forwarded to %s", AuthorizationFailureHandler.OAUTH_ERROR_PAGE)
					.hasForwardedUrl(AuthorizationFailureHandler.OAUTH_ERROR_PAGE);

			result.assertThat()
					.request()
					.attributes()
					.as("OAuth should be present in the request attribute with code %s", code)
					.hasEntrySatisfying(WebAttributes.AUTHENTICATION_EXCEPTION, ex -> assertThat(ex)
							.isInstanceOf(OAuth2AuthenticationException.class)
							.asInstanceOf(type(OAuth2AuthenticationException.class))
							.extracting(OAuth2AuthenticationException::getError)
							.returns(code, OAuth2Error::getErrorCode)
					);
		};
	}

	static Consumer<MvcTestResult> assertOAuthErrorResponse(String code) {
		return assertOAuthErrorResponse(code, ignore -> { /* noop */ });
	}

	static Consumer<MvcTestResult> assertOAuthErrorResponse(String code, ThrowingConsumer<OAuth2Error> assertion) {
		return result -> result.assertThat()
				.bodyJson()
				.as("OAuth error be present in the response: %s", code)
				.convertTo(OAuth2Error.class)
				.returns(code, OAuth2Error::getErrorCode)
				.satisfies(assertion);
	}

	static Consumer<MvcTestResult> assertRedirect(Consumer<URI> assertion) {
		return result -> result.assertThat()
				.hasStatus3xxRedirection()
				.satisfies(it -> assertThat(it.getResponse())
						.extracting(MockHttpServletResponse::getRedirectedUrl)
						.extracting(URI::create)
						.satisfies(assertion)
				);
	}

	static <T> Consumer<T> assertUri(String path, Function<T, URL> mapper) {
		return metadata -> assertThat(mapper.apply(metadata))
				.hasProtocol("http")
				.hasHost("localhost")
				.hasPath(path);
	}

	static <T> Consumer<T> assertElements(Function<T, Collection<String>> mapper, String... elements) {
		return metadata -> assertThat(mapper.apply(metadata))
				.containsExactlyInAnyOrder(elements);
	}

	static <T> Consumer<T> assertOAuthScopes(Function<T, Collection<String>> mapper) {
		final var scopes = new ArrayList<String>();
		AuthorizationServerScopes.register(scopes);

		return metadata -> assertThat(mapper.apply(metadata))
				.containsExactlyInAnyOrderElementsOf(scopes);
	}

	static RequestPostProcessor rememberMe(AccountRememberMeServices rememberMeServices, AccountIdentityService service, long id) {
		final AccountIdentity identity = service.get(EntityId.from(id));

		return request -> {
			final var response = new MockHttpServletResponse();

			final List<GrantedAuthority> authorities = new ArrayList<>(identity.getAuthorities());
			authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.AUTHORIZATION_CODE_AUTHORITY));

			rememberMeServices.loginSuccess(request, response, new UsernamePasswordAuthenticationToken(
					identity, identity.getPassword(), authorities
			));

			request.setCookies(response.getCookies());

			return request;
		};
	}

	static MultiValueMap<String, String> extractUriParameters(String uri) {
		final var parameters = new LinkedMultiValueMap<String, String>();

		final UriComponents consentsUri = UriComponentsBuilder
				.fromUriString(uri)
				.build();

		consentsUri.getQueryParams().forEach((name, values) -> {
			for (final var value : values) {
				parameters.add(name, URLDecoder.decode(value, StandardCharsets.UTF_8));
			}
		});

		return parameters;
	}

}


