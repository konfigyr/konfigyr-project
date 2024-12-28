package com.konfigyr.identity.authentication.controller;

import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@TestProfile
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
class LoginControllerTest {

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
		assertThat(mvc.get().uri("/"))
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("http://localhost/login");
	}

	@Test
	@DisplayName("should generate login page model")
	void assertLoginAuthenticationOptions() {
		assertThat(mvc.get().uri("/login"))
				.apply(log())
				.hasStatusOk()
				.hasViewName("login")
				.model()
				.containsEntry("error", null)
				.containsEntry("logout", false)
				.containsEntry("options", Set.of(
						new LoginController.AuthenticationOption(
								"oauth-test", "Test OAuth client", URI.create("/oauth2/authorization/oauth-test")
						)
				));
	}

	@Test
	@DisplayName("should fail to logout when CSRF token is not present")
	void shouldFailToLogoutWhenCSRFTokenIsNotPresent() {
		assertThat(mvc.post().uri("/logout"))
				.apply(log())
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	@DisplayName("should fail to logout when CSRF token is invalid")
	void shouldFailToLogoutWhenCSRFTokenIsInvalid() {
		assertThat(mvc.post().uri("/logout").with(csrf().useInvalidToken()))
				.apply(log())
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	@DisplayName("should redirect to login page when logging out")
	void shouldRedirectToLoginWhenLoggingOut() {
		assertThat(mvc.post().uri("/logout").with(csrf()))
				.apply(log())
				.hasStatus3xxRedirection()
				.hasRedirectedUrl("/login?logout");
	}

	@Test
	@DisplayName("should render the login page with a logout success message")
	void shouldRenderLoginPageWithLogoutSuccessMessage() {
		driver.get("http://localhost/login?logout");

		assertThat(driver.findElement(By.cssSelector("#logout-success")))
				.isNotNull()
				.extracting(WebElement::getText, InstanceOfAssertFactories.STRING)
				.containsIgnoringCase("Logged out")
				.containsIgnoringCase("You have been successfully logged out of your account.");

		assertThat(driver.findElements(By.cssSelector("#login-options a")))
				.hasSize(1);
	}

	@Test
	@DisplayName("should render login page with OAuth exception")
	void shouldRenderLoginErrors() {
		// should force an OAuth 2.0 error with an invalid request
		driver.get("http://localhost/login/oauth2/code/konfigyr-test");

		assertThat(driver.findElement(By.cssSelector("#oauth-error")))
				.isNotNull()
				.extracting(WebElement::getText, InstanceOfAssertFactories.STRING)
				.containsIgnoringCase(OAuth2ErrorCodes.INVALID_REQUEST);

		assertThat(driver.findElements(By.cssSelector("#login-options a")))
				.hasSize(1);
	}

	@Test
	@DisplayName("should render login page with configured OAuth2 Client registrations")
	void shouldRenderLoginPage() {
		driver.get("http://localhost/login");

		Assertions.assertThat(driver.getTitle())
				.isNotNull()
				.isEqualTo("Konfigyr Identity - Login");

		assertThat(driver.findElements(By.cssSelector("#login-options a")))
				.hasSize(1)
				.extracting(WebElement::getText, it -> it.getAttribute("href"))
				.containsExactlyInAnyOrder(
						tuple("Test OAuth client", "http://localhost/oauth2/authorization/oauth-test")
				);
	}
}
