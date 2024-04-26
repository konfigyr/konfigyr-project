package com.konfigyr.security;

import com.konfigyr.page.LoginPage;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Vladimir Spasic
 **/
@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class SecurityIntegrationTest {

	@Autowired
	MockMvc mvc;

	WebDriver driver;

	@BeforeEach
	void setup() {
		driver = MockMvcHtmlUnitDriverBuilder
				.mockMvcSetup(mvc)
				.javascriptEnabled(false)
				.build();
	}

	@Test
	@DisplayName("should render index page when not authenticated")
	void shouldRenderIndexPage() throws Exception {
		mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(
						containsStringIgnoringCase("<a href=\"/login\">")
				));
	}

	@Test
	@DisplayName("should redirect to login page when not authenticated")
	void shouldRedirectToLogin() throws Exception {
		mvc.perform(get("/search"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("http://localhost/login"));
	}

	@Test
	@DisplayName("should render OAuth2 providers on login page")
	void shouldRenderProviders() {
		final var page = LoginPage.load(driver);

		assertThat(page.getTitle())
				.isNotNull()
				.isEqualTo("Konfigyr - Login");

		final var buttons = page.getLoginButtons();

		assertThat(buttons)
				.hasSize(1)
				.extracting(it -> it.getAttribute("href"))
				.containsExactly("http://localhost/oauth2/authorization/github");

		page.login(buttons.getFirst());

		assertThat(driver.getCurrentUrl())
				.isNotBlank()
				.satisfies(it -> assertThat(URI.create(it))
						.hasHost("login-demo.curity.io")
						.hasParameter(OAuth2ParameterNames.RESPONSE_TYPE, OAuth2AuthorizationResponseType.CODE.getValue())
						.hasParameter(OAuth2ParameterNames.SCOPE, "openid email")
						.hasParameter(OAuth2ParameterNames.CLIENT_ID, "github-test")
						.hasParameter(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/login/oauth2/code/github")
						.hasParameter(OAuth2ParameterNames.STATE)
						.hasParameter(OidcParameterNames.NONCE)
				);
	}

}
