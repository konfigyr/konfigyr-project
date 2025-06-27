package com.konfigyr.test.smtp;

import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class GreenMailConfigurationTest {

	ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(GreenMailConfiguration.class);

	@Test
	@DisplayName("should register GreenMail test SMTP server")
	void setupGreenMailServer() {
		runner.run(context -> {
			assertThat(context)
					.hasSingleBean(GreenMail.class)
					.hasSingleBean(DynamicPropertyRegistrar.class);

			final var server = context.getBean(GreenMail.class);
			final var registry = context.getBean(DynamicPropertyRegistrar.class);
			final var properties = new LinkedHashMap<String, Object>();

			registry.accept((name, supplier) -> properties.put(name, supplier.get()));

			assertThat(properties)
					.hasSize(6)
					.containsEntry("spring.mail.host", server.getSmtp().getBindTo())
					.containsEntry("spring.mail.port", server.getSmtp().getPort())
					.containsEntry("spring.mail.protocol", server.getSmtp().getProtocol())
					.containsEntry("spring.mail.ssl.enabled", false)
					.containsEntry("spring.mail.properties.mail.smtp.starttls.enable", false)
					.containsEntry("spring.mail.properties.mail.smtp.starttls.required", false);
		});
	}

	@TestSmtpServer
	static class GreenMailTestConfiguration {

	}

}
