package com.konfigyr.identity.authorization.controller;

import com.konfigyr.entity.EntityId;
import com.konfigyr.security.OAuthScope;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class AuthorizationConsentControllerTest {

	static WebDriver driver;

	@Autowired
	MockMvcTester mvc;

	@BeforeAll
	static void setup(WebApplicationContext context) {
		driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(context.getBean(MockMvc.class))
				.javascriptEnabled(false)
				.build();
	}

	@AfterAll
	static void cleanup() {
		if (driver != null) {
			driver.close();
		}
	}

	@Test
	@DisplayName("should redirect to login page when not authenticated")
	void shouldRedirectToLogin() {
		final var request = get("/oauth/consents")
				.queryParam("client_id", "konfigyr")
				.queryParam("state", "test-state")
				.queryParam("scope", "profile");

		assertThat(mvc.perform(request))
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("http://localhost/login");
	}

	@Test
	@DisplayName("should fail to find registered OAuth client")
	void shouldFailToFindRegisteredOAuthClient() {
		final var request = get("/oauth/consents")
				.queryParam("client_id", "unknown")
				.queryParam("state", "test-state")
				.queryParam("scope", "profile")
				.with(authenticated());

		assertThat(mvc.perform(request))
				.hasFailed()
				.failure()
				.hasRootCauseInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("OAuth 2.0 client registration not found");
	}

	@Test
	@DisplayName("should render OAuth 2.0 consents form")
	void shouldRenderConsentForm() {
		final var request = get("/oauth/consents")
				.queryParam("client_id", "konfigyr")
				.queryParam("state", "test-state")
				.queryParam("scope", "namespaces:read")
				.with(authenticated());

		assertThat(mvc.perform(request))
				.apply(log())
				.hasStatusOk()
				.hasViewName("consents")
				.model()
				.containsEntry("state", "test-state")
				.containsEntry("scope", "namespaces:read")
				.hasEntrySatisfying("client", it -> assertThat(it)
						.isInstanceOf(RegisteredClient.class)
						.asInstanceOf(InstanceOfAssertFactories.type(RegisteredClient.class))
						.returns("konfigyr", RegisteredClient::getId)
						.returns("konfigyr", RegisteredClient::getClientId)
						.returns("konfigyr", RegisteredClient::getClientName)
				)
				.hasEntrySatisfying("scopes", it -> assertThat(it)
						.isInstanceOf(Iterable.class)
						.asInstanceOf(InstanceOfAssertFactories.iterable(AuthorizedScope.class))
						.hasSize(1)
						.containsExactlyInAnyOrder(
								AuthorizedScope.unauthorized(OAuthScope.READ_NAMESPACES)
						)
				)
				.hasEntrySatisfying("consent", it -> assertThat(it).isNull());
	}

	static RequestPostProcessor authenticated() {
		return SecurityMockMvcRequestPostProcessors.user(EntityId.from(1).serialize())
				.authorities(AuthorityUtils.createAuthorityList("konfigyr-identity"))
				.password("empty");
	}

}
