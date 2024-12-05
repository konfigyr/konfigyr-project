package com.konfigyr.security.login;

import com.konfigyr.test.AbstractMvcIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

/**
 * @author Vladimir Spasic
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginSecurityControllerTest extends AbstractMvcIntegrationTest {

	@LocalServerPort
	int localServerPort;

	WebDriver driver;

	@BeforeEach
	void setup(ApplicationContext context) {
		driver = MockMvcHtmlUnitDriverBuilder
				.mockMvcSetup(context.getBean(MockMvc.class))
				.javascriptEnabled(false)
				.build();
	}

	@Test
	@DisplayName("should render index page when not authenticated")
	void shouldRenderIndexPage() {
		assertThat(mvc.get().uri("/"))
				.apply(log())
				.hasStatusOk()
				.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
				.bodyText()
				.containsIgnoringCase("<a href=\"/login\">");
	}

	@Test
	@DisplayName("should redirect to login page when not authenticated")
	void shouldRedirectToLogin() {
		assertThat(mvc.get().uri("/search"))
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("http://localhost/login");
	}

	@Test
	@DisplayName("should render login page with logout success message")
	void shouldRenderLogoutSuccess() {
		final var page = LoginPage.create(driver, localServerPort);
		page.load(false, true);

		assertThat(driver.findElement(By.cssSelector("#logout-success")))
				.isNotNull()
				.extracting(WebElement::getText)
				.isEqualTo("You have been successfully logged out of your account");

		assertThat(page.getLoginButtons())
				.hasSize(1);
	}

	@Test
	@DisplayName("should render login page with OAuth exception")
	void shouldRenderLoginErrors() {
		final var page = LoginPage.create(driver, localServerPort);
		// this should force the invalid_request OAuth error
		driver.get(page.getUriFor("/login/oauth2/code/konfigyr-test").toUriString());

		assertThat(driver.findElement(By.cssSelector("#oauth-error")))
				.isNotNull()
				.extracting(WebElement::getText)
				.isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);

		assertThat(page.getLoginButtons())
				.hasSize(1);
	}

	@Test
	@DisplayName("should render OAuth2 providers on login page")
	void shouldRenderProviders() {
		final var page = LoginPage.load(driver, localServerPort);
		final var authorizationRequestUri = page.getUriFor("/oauth2/authorization/konfigyr-test").toUriString();
		final var authenticationRequestUri = page.getUriFor("/login/oauth2/code/konfigyr-test").toUriString();

		Assertions.assertThat(page.getTitle())
				.isNotNull()
				.isEqualTo("Konfigyr - Login");

		final var buttons = page.getLoginButtons();

		Assertions.assertThat(buttons)
				.hasSize(1)
				.first()
				.returns(authorizationRequestUri, it -> it.getAttribute("href"));

		page.login(buttons.getFirst());

		assertThat(driver.getCurrentUrl())
				.isNotBlank()
				.satisfies(it -> assertThat(URI.create(it))
						.hasHost("login-demo.curity.io")
						.hasParameter(OAuth2ParameterNames.RESPONSE_TYPE, OAuth2AuthorizationResponseType.CODE.getValue())
						.hasParameter(OAuth2ParameterNames.SCOPE, "openid email")
						.hasParameter(OAuth2ParameterNames.CLIENT_ID, "konfigyr-test")
						.hasParameter(OAuth2ParameterNames.REDIRECT_URI, authenticationRequestUri)
						.hasParameter(OAuth2ParameterNames.STATE)
						.hasParameter(OidcParameterNames.NONCE)
				);
	}

}
