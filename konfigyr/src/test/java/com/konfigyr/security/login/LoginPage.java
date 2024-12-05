package com.konfigyr.security.login;

import com.konfigyr.test.page.AbstractPage;
import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTML unit test login page object.
 *
 * @author Vladimir Spasic
 **/
@Getter
class LoginPage extends AbstractPage<LoginPage> {

	static LoginPage load(WebDriver driver) {
		return load(driver, 80);
	}

	static LoginPage load(WebDriver driver, int port) {
		return new LoginPage(driver, port).get();
	}

	LoginPage(WebDriver driver, int port) {
		super(driver, port);
	}

	@FindBy(css = "[data-test-selector=\"oauth-login-button\"]")
	private List<WebElement> loginButtons;

	@Override
	protected void load() {
		driver.get(getUriBuilder().path("login").toUriString());
	}

	@Override
	protected void isLoaded() throws Error {
		assertThat(getUrl()).hasPath("/login");
	}

	void login(WebElement element) {
		element.click();
	}
}
