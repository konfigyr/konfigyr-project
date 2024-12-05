package com.konfigyr.security.login;

import com.konfigyr.test.page.AbstractPage;
import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTML unit test login page object.
 *
 * @author Vladimir Spasic
 **/
@Getter
class LoginPage extends AbstractPage<LoginPage> {

	static LoginPage create(WebDriver driver, int port) {
		return new LoginPage(driver, port);
	}

	static LoginPage load(WebDriver driver) {
		return load(driver, 80);
	}

	static LoginPage load(WebDriver driver, int port) {
		return create(driver, port).get();
	}

	LoginPage(WebDriver driver, int port) {
		super(driver, port);
	}

	@FindBy(css = "[data-test-selector=\"oauth-login-button\"]")
	private List<WebElement> loginButtons;

	@Override
	protected void load() {
		load(false, false);
	}

	protected void load(boolean error, boolean logout) {
		final UriComponentsBuilder builder = getUriBuilder().path("login");

		if (error) {
			builder.queryParam("error", "");
		}

		if (logout) {
			builder.queryParam("logout", "");
		}

		driver.get(builder.toUriString());
	}

	@Override
	protected void isLoaded() throws Error {
		assertThat(getUrl()).hasPath("/login");
	}

	void login(WebElement element) {
		element.click();
	}
}
