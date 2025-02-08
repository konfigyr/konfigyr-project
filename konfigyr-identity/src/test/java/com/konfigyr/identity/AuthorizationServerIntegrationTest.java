package com.konfigyr.identity;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class AuthorizationServerIntegrationTest {

	@Autowired
	MockMvcTester mvc;

	@Test
	@DisplayName("should fail to authorize request for unknown client")
	void authorizationForUnknownClient() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "unknown")
				.queryParam(OAuth2ParameterNames.CLIENT_SECRET, "secret")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("should fail to authorize request for invalid client credentials")
	void authorizationForInvalidClientCredentials() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.CLIENT_SECRET, "the other secret")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("should fail to authorize request for unsupported scope")
	void authorizationForUnsupportedScope() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.CLIENT_SECRET, "secret")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "unsupported_scope")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("should fail to authorize request for unsupported redirect URI")
	void authorizationForUnsupportedRedirectUri() {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.CLIENT_SECRET, "secret")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/redirect_uri")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("should authorize request and redirect to login page")
	void authorizationToLoginPage()  {
		mvc.get().uri("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.CLIENT_SECRET, "secret")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://127.0.0.1:8080/login/oauth2/code/konfigyr")
				.exchange()
				.assertThat()
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("http://localhost/login");
	}

}


