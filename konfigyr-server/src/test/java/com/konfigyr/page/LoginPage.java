package com.konfigyr.page;

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
public class LoginPage extends AbstractPage<LoginPage> {

	static final String PAGE_URL = "http://localhost/login";

	public static LoginPage load(WebDriver driver) {
		return new LoginPage(driver).get();
	}

	LoginPage(WebDriver driver) {
		super(driver);
	}

	@FindBy(css = "[data-test-selector=\"oauth-login-button\"]")
	private List<WebElement> loginButtons;

	@Override
	protected void load() {
		driver.get(PAGE_URL);
	}

	@Override
	protected void isLoaded() throws Error {
		assertThat(getUrl()).hasPath("/login");
	}

	public void login(WebElement element) {
		element.click();
	}
}
