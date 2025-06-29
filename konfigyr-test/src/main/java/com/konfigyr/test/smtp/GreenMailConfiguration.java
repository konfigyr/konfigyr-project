package com.konfigyr.test.smtp;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

final class GreenMailConfiguration implements SmartLifecycle, InitializingBean {

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
		start();
	}

	@Override
	public void start() {
		server.start();
	}

	@Override
	public boolean isRunning() {
		return server.isRunning();
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void stop() {
		server.stop();
	}

	static class GreenMailTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestMethod(@NonNull TestContext context) {
			final ApplicationContext applicationContext = context.getApplicationContext();
			final GreenMail server = applicationContext.getBean(GreenMail.class);
			server.reset();
		}

	}

}
