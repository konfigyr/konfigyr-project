package com.konfigyr.security.login;

import com.konfigyr.test.AbstractMvcIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
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
class LoginSecurityControllerTest extends AbstractMvcIntegrationTest {

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
