package com.konfigyr.test.smtp;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.jspecify.annotations.NonNull;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

@Slf4j
final class GreenMailConfiguration implements InitializingBean, DisposableBean {

	private final GreenMail server = new GreenMail(
			new ServerSetup(0, null, ServerSetup.PROTOCOL_SMTP)
	);

	@Bean
	GreenMail greenMailServer() {
		return server;
	}

	@Bean
	DynamicPropertyRegistrar greenMailPropertyRegistrar() {
		return registry -> {
			registry.add("spring.mail.host", server.getSmtp()::getBindTo);
			registry.add("spring.mail.port", server.getSmtp()::getPort);
			registry.add("spring.mail.protocol", server.getSmtp()::getProtocol);
			registry.add("spring.mail.ssl.enabled", () -> false);
			registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
			registry.add("spring.mail.properties.mail.smtp.starttls.required", () -> false);
		};
	}

	@Override
	public void afterPropertiesSet() {
		if (server.isRunning()) {
			return;
		}

		server.start();

		log.info("Started GreenMail SMTP server on: {}:{}", server.getSmtp().getBindTo(), server.getSmtp().getPort());
	}

	@Override
	public void destroy() {
		if (!server.isRunning()) {
			return;
		}

		server.stop();

		log.info("GreenMail SMTP server has been stopped.");
	}

	static class GreenMailTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestMethod(@NonNull TestContext context) throws FolderException {
			final ApplicationContext applicationContext = context.getApplicationContext();

			final GreenMail server = applicationContext.getBean(GreenMail.class);
			server.purgeEmailFromAllMailboxes();
		}

	}

}
