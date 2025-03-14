package com.konfigyr.identity.acceptance;

import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityService;
import com.konfigyr.identity.authentication.rememberme.AccountRememberMeServices;
import com.konfigyr.test.TestContainers;
import com.konfigyr.test.TestProfile;
import org.assertj.core.api.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.htmlunit.webdriver.MockMvcHtmlUnitDriverBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@TestProfile
@AutoConfigureMockMvc
@ImportTestcontainers(TestContainers.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationCodeAcceptanceTest {

	static WebDriver driver;
	static UriComponents host;
	static RememberMeServices services;

	@Autowired
	MockMvcTester mvc;

	@BeforeAll
	static void setup(WebApplicationContext context) {
		final int port = context.getEnvironment()
				.getProperty("local.server.port", Integer.class, 8080);

		driver = MockMvcHtmlUnitDriverBuilder.mockMvcSetup(context.getBean(MockMvc.class))
				.javascriptEnabled(false)
				.build();

		host = UriComponentsBuilder.newInstance()
				.scheme("http")
				.host("localhost")
				.port(port)
				.build();

		services = new AccountRememberMeServices(context.getBean(AccountIdentityService.class)::get);
	}

	@BeforeEach
	void cleanup() {
		driver.manage().deleteAllCookies();
	}

	@AfterAll
	static void close() {
		if (driver != null) {
			driver.close();
		}
	}

	@Test
	@DisplayName("should process authorization request with `code` response type")
	void processAuthorizationRequest() {
		final UriComponents uri = authorizationUri().build();
		final Cookie cookie = generateRememberMeCookie(AccountIdentities.jane().build());

		driver.get(uri.toUriString());

		assertThatCurrentUri(driver)
				.as("Should redirect to login page when not authenticated")
				.hasScheme(uri.getScheme())
				.hasHost(uri.getHost())
				.hasPort(uri.getPort())
				.hasPath("/login");

		driver.manage().addCookie(cookie);

		driver.get(uri.toUriString());

		assertThatCurrentUri(driver)
				.as("Should redirect to login page when not authenticated")
				.hasScheme(uri.getScheme())
				.hasHost(uri.getHost())
				.hasPort(uri.getPort())
				.hasPath("/oauth/consents")
				.hasParameter(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.hasParameter(OAuth2ParameterNames.SCOPE, "openid namespaces")
				.hasParameter(OAuth2ParameterNames.STATE);

		assertThatElement(driver, "#consents input[name=\"_csrf\"]")
				.extracting(it -> it.getAttribute("value"), InstanceOfAssertFactories.STRING)
				.isNotBlank();

		assertThatElement(driver, "#consents input[name=\"state\"]")
				.extracting(it -> it.getAttribute("value"), InstanceOfAssertFactories.STRING)
				.isNotBlank();

		assertThatElement(driver, "#consents input[name=\"client_id\"]")
				.satisfies(elementHasValue("konfigyr"));

		assertThatElement(driver, "#consents input[name=\"scope\"]")
				.satisfies(elementHasValue("namespaces"))
				.satisfies(WebElement::click);

		assertThatElement(driver, "#consents button[type=\"submit\"]")
				.satisfies(WebElement::submit);

		assertThatCurrentUri(driver)
				.as("Should redirect to client with authorization code")
				.hasPath("/oauth/client/code")
				.hasParameter(OAuth2ParameterNames.STATE, "test-state")
				.hasParameter(OAuth2ParameterNames.CODE)
				.extracting(UriComponentsBuilder::fromUri)
				.extracting(UriComponentsBuilder::build)
				.satisfies(obtainToken(mvc));
	}

	static UriComponentsBuilder uri() {
		return UriComponentsBuilder.newInstance().uriComponents(host);
	}

	static UriComponentsBuilder authorizationUri() {
		return uri()
				.path("/oauth/authorize")
				.queryParam(OAuth2ParameterNames.RESPONSE_TYPE, "code")
				.queryParam(OAuth2ParameterNames.CLIENT_ID, "konfigyr")
				.queryParam(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.queryParam(OAuth2ParameterNames.SCOPE, "openid namespaces")
				.queryParam(OAuth2ParameterNames.STATE, "test-state");
	}

	static Cookie generateRememberMeCookie(AccountIdentity identity) {
		final var request = new MockHttpServletRequest();
		final var response = new MockHttpServletResponse();
		final var authentication = new TestingAuthenticationToken(identity, "", identity.getAuthorities());

		services.loginSuccess(request, response, authentication);

		assertThat(response.getCookies())
				.hasSize(1);

		final var cookie = response.getCookies()[0];

		return new Cookie(cookie.getName(), cookie.getValue(), cookie.getPath(),
				Date.from(Instant.now().plusSeconds(600)));
	}

	static UriAssert assertThatCurrentUri(WebDriver driver) {
		return assertThat(driver.getCurrentUrl())
				.isNotBlank()
				.asInstanceOf(new InstanceOfAssertFactory<>(
						String.class, value -> new UriAssert(URI.create(value))
				));
	}

	static ObjectAssert<WebElement> assertThatElement(WebDriver driver, String selector) {
		return assertThat(driver.findElement(By.cssSelector(selector)))
				.as("Element with CSS selector '%s' exists in the DOM", selector)
				.isNotNull();
	}

	static ThrowingConsumer<WebElement> elementHasValue(String value) {
		return element -> assertThat(element.getAttribute("value"))
				.as("HTML element of %s, should contain a value: %s", element, value)
				.isEqualTo(value);
	}

	static ThrowingConsumer<UriComponents> obtainToken(MockMvcTester mvc) {
		final var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth("konfigyr", "secret");

		return uri -> mvc.post()
				.uri("/oauth/token")
				.param(OAuth2ParameterNames.GRANT_TYPE, "authorization_code")
				.param(OAuth2ParameterNames.REDIRECT_URI, "http://localhost/oauth/client/code")
				.param(OAuth2ParameterNames.CODE, uri.getQueryParams().getFirst("code"))
				.headers(headers)
				.exchange()
				.assertThat()
				.hasStatusOk()
				.bodyJson()
				.hasPathSatisfying("$.token_type", it -> it.assertThat()
						.isEqualTo(OAuth2AccessToken.TokenType.BEARER.getValue())
				)
				.hasPathSatisfying("$.scope", it -> it.assertThat()
						.isEqualTo("openid namespaces")
				)
				.hasPathSatisfying("$.expires_in", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.access_token", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.refresh_token", it -> it.assertThat().isNotNull())
				.hasPathSatisfying("$.id_token", it -> it.assertThat().isNotNull());
	}

}
