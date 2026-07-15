package com.konfigyr.autoconfigure;

import com.konfigyr.Hostnames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class KonfigyrAutoConfigurationTest {

	final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(KonfigyrAutoConfiguration.class)
	);

	@Test
	@DisplayName("should register default hostnames")
	void shouldRegisterDefaultHostnames() {
		runner.run(ctx -> assertThat(ctx)
				.hasNotFailed()
				.getBean(Hostnames.class)
				.returns(URI.create("https://api.konfigyr.com"), Hostnames::api)
				.returns(URI.create("https://id.konfigyr.com"), Hostnames::identity)
				.returns(URI.create("https://konfigyr.com"), Hostnames::web)
		);
	}

	@Test
	@DisplayName("should register custom hostnames")
	void shouldBindConfiguredHostnames() {
		runner.withPropertyValues(
				"konfigyr.hostnames.api=https://api.example.com",
				"konfigyr.hostnames.identity=https://id.example.com",
				"konfigyr.hostnames.web=https://example.com"
		).run(ctx -> assertThat(ctx)
				.hasNotFailed()
				.getBean(Hostnames.class)
				.returns(URI.create("https://api.example.com"), Hostnames::api)
				.returns(URI.create("https://id.example.com"), Hostnames::identity)
				.returns(URI.create("https://example.com"), Hostnames::web)
		);
	}

	@Test
	@DisplayName("should fail to start when hostname is not a valid URL")
	void shouldFailToStartWhenHostnameIsNotValidUrl() {
		runner.withPropertyValues("konfigyr.hostnames.api=ht$tp://example.com:abc")
				.run(ctx -> assertThat(ctx).hasFailed());
	}

}
