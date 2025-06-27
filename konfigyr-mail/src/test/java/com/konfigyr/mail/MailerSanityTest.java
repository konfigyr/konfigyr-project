package com.konfigyr.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.konfigyr.test.smtp.TestSmtpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@TestSmtpServer
@EnableAutoConfiguration
@SpringBootTest(classes = MailerSanityTest.class)
class MailerSanityTest {

	@Autowired
	Mailer mailer;

	@Autowired
	GreenMail server;

	@Test
	@DisplayName("should render and send test mail")
	void shouldSendTestMail() {
		final var mail = Mail.builder()
				.template("mail/test")
				.subject("Sanity testing")
				.from("no-reply@konfigyr.com")
				.to("test@konfigyr.com")
				.attribute("title", "Test mail")
				.attribute("contents", "Test mail contents")
				.build();

		assertThatNoException().isThrownBy(() -> mailer.send(mail));

		assertThat(server.getReceivedMessages())
				.hasSize(1)
				.satisfiesExactly(mime -> {
					assertThat(mime.getSubject())
							.isEqualTo("Sanity testing");

					assertThat(mime.getContentType())
							.isEqualTo("text/html;charset=UTF-8");

					final var document = Jsoup.parse(mime.getContent().toString());

					assertThat(document.title())
							.isEqualTo("Konfigyr - Test mail");

					assertThat(document.select("#header h1"))
							.hasSize(1)
							.first()
							.returns("Test mail", Element::text);

					assertThat(document.select("#content p"))
							.hasSize(1)
							.first()
							.returns("Test mail contents", Element::text);

					assertThat(document.select("#footer p"))
							.hasSize(1)
							.first()
							.returns("Made with ❤️ in Berlin.", Element::text);
				});
	}

}
