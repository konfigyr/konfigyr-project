package com.konfigyr.identity.authorization.workload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WorkloadClientAuthenticationConverterTest {

	WorkloadClientAuthenticationConverter converter;
	MockHttpServletRequest request;

	@BeforeEach
	void setup() {
		converter = new WorkloadClientAuthenticationConverter();
		request = new MockHttpServletRequest();
	}

	@Test
	@DisplayName("should return null when grant_type is missing")
	void returnsNullWhenGrantTypeMissing() {
		assertThat(converter.convert(request)).isNull();
	}

	@Test
	@DisplayName("should return null for authorization_code grant type")
	void returnsNullForAuthorizationCode() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());

		assertThat(converter.convert(request)).isNull();
	}

	@Test
	@DisplayName("should return null for client_credentials grant type")
	void returnsNullForClientCredentials() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());

		assertThat(converter.convert(request)).isNull();
	}

	@Test
	@DisplayName("should throw invalid_request when client_id is absent on token exchange request")
	void throwsWhenClientIdAbsent() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue());

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> converter.convert(request))
				.satisfies(ex -> assertThat(ex.getError().getErrorCode())
						.isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST));
	}

	@Test
	@DisplayName("should throw invalid_request when client_id is blank on token exchange request")
	void throwsWhenClientIdBlank() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue());
		request.addParameter(OAuth2ParameterNames.CLIENT_ID, "  ");

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> converter.convert(request))
				.satisfies(ex -> assertThat(ex.getError().getErrorCode())
						.isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST));
	}

	@Test
	@DisplayName("should throw invalid_request when client_id appears more than once")
	void throwsWhenClientIdDuplicated() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue());
		request.addParameter(OAuth2ParameterNames.CLIENT_ID, "kfg-client-1");
		request.addParameter(OAuth2ParameterNames.CLIENT_ID, "kfg-client-2");

		assertThatExceptionOfType(OAuth2AuthenticationException.class)
				.isThrownBy(() -> converter.convert(request))
				.satisfies(ex -> assertThat(ex.getError().getErrorCode())
						.isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST));
	}

	@Test
	@DisplayName("should return OAuth2ClientAuthenticationToken with NONE method for valid token exchange request")
	void convertsValidTokenExchangeRequest() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue());
		request.addParameter(OAuth2ParameterNames.CLIENT_ID, "kfg-test-client");
		request.addParameter("subject_token", "some.jwt.token");
		request.addParameter("subject_token_type", "urn:ietf:params:oauth:token-type:jwt");

		final var result = converter.convert(request);

		assertThat(result)
				.isNotNull()
				.isInstanceOf(OAuth2ClientAuthenticationToken.class);

		final var token = (OAuth2ClientAuthenticationToken) result;

		assertThat(token.getPrincipal()).isEqualTo("kfg-test-client");
		assertThat(token.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.NONE);
		assertThat(token.isAuthenticated()).isFalse();
	}

	@Test
	@DisplayName("should include all request parameters except client_id in additional parameters")
	void populatesAdditionalParametersWithoutClientId() {
		request.addParameter(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue());
		request.addParameter(OAuth2ParameterNames.CLIENT_ID, "kfg-test-client");
		request.addParameter("subject_token", "my-subject-token");
		request.addParameter("subject_token_type", "urn:ietf:params:oauth:token-type:jwt");
		request.addParameter(OAuth2ParameterNames.SCOPE, "namespaces");

		final var token = (OAuth2ClientAuthenticationToken) converter.convert(request);

		assertThat(token.getAdditionalParameters())
				.containsEntry("subject_token", "my-subject-token")
				.containsEntry("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
				.containsEntry(OAuth2ParameterNames.SCOPE, "namespaces")
				.containsEntry(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.TOKEN_EXCHANGE.getValue())
				.doesNotContainKey(OAuth2ParameterNames.CLIENT_ID);
	}

}
