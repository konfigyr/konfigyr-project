package com.konfigyr.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.LoadableComponent;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.net.URI;

/**
 * Abstract HTML Unit Page Object that should be used as a base class when creating specific
 * pages to be tested using {@link WebDriver}.
 *
 * @param <T> generic page type
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see <a href="https://www.selenium.dev/documentation/test_practices/encouraged/page_object_models/">Page Object Models</a>
 **/
public abstract class AbstractPage<T extends AbstractPage<T>> extends LoadableComponent<T> {

	@NonNull
	protected final WebDriver driver;

	protected AbstractPage(WebDriver driver) {
		Assert.notNull(driver, "Driver can not be null");
		this.driver = driver;

		PageFactory.initElements(driver, this);
	}

	public String getTitle() {
		return driver.getTitle();
	}

	public URI getUrl() {
		return URI.create(driver.getCurrentUrl());
	}
}
