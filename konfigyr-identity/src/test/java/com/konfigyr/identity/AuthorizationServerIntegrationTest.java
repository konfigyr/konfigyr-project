package com.konfigyr.identity;

import com.konfigyr.entity.EntityId;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityService;
import com.konfigyr.identity.authentication.rememberme.AccountRememberMeServices;
import com.konfigyr.identity.authorization.AuthorizationFailureHandler;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class AuthorizationServerIntegrationTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	AccountIdentityService identityService;

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
	@DisplayName("should fail to obtain token for unknown client")
	void tokenForUnknownClient() {
		mvc.post().uri("/oauth/token")
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
	@DisplayName("should fail to obtain token for invalid client credentials")
	void tokenForInvalidClientCredentials() {
		mvc.post().uri("/oauth/token")
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
		mvc.post().uri("/oauth/token")
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
		mvc.post().uri("/oauth/token")
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
	@DisplayName("should authorize request and redirect to login page when principal is not authenticated")
	void redirectToLoginWhenNotAuthenticated()  {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("http://localhost/login")
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
	@DisplayName("should issue OAuth Access Token")
	void issueAccessTokens()  {
		final var result = mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.STATE, "state")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.with(rememberMe(identityService, 1))
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

		final String code = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl())
				.build()
				.getQueryParams()
				.getFirst(OAuth2ParameterNames.CODE);

		mvc.post().uri("/oauth/token")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
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
				.hasPathSatisfying("$.id_token", it -> it.assertThat().isNotNull());
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
		return result -> result.assertThat()
				.bodyJson()
				.as("OAuth error be present in the response: %s", code)
				.hasPathSatisfying("$.error", it -> it.assertThat()
						.isEqualTo(code)
				);
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

	static RequestPostProcessor rememberMe(AccountIdentityService service, long id) {
		final AccountRememberMeServices services = new AccountRememberMeServices(service::get);
		final AccountIdentity identity = service.get(EntityId.from(id));

		return request -> {
			final var response = new MockHttpServletResponse();

			services.loginSuccess(request, response, new UsernamePasswordAuthenticationToken(
					identity, identity.getPassword(), identity.getAuthorities()
			));

			request.setCookies(response.getCookies());

			return request;
		};
	}

}


