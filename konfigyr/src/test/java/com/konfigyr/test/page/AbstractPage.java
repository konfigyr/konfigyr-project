package com.konfigyr.test.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.LoadableComponent;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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

	@NonNull
	protected UriComponents host;

	protected final int port;

	protected AbstractPage(WebDriver driver, int port) {
		Assert.notNull(driver, "Driver can not be null");
		Assert.isTrue(port > 0, "Port must be greater than 0");

		this.driver = driver;
		this.port = port;
		this.host = UriComponentsBuilder.newInstance()
						.scheme("http")
						.host("localhost")
						.port(port)
						.build();

		PageFactory.initElements(driver, this);
	}

	public UriComponentsBuilder getUriBuilder() {
		return UriComponentsBuilder.newInstance().uriComponents(host);
	}

	public UriComponentsBuilder getUriFor(String path) {
		return getUriBuilder().path(path);
	}

	public String getTitle() {
		return driver.getTitle();
	}

	public URI getUrl() {
		return URI.create(driver.getCurrentUrl());
	}
}
