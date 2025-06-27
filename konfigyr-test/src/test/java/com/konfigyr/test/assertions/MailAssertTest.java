package com.konfigyr.test.assertions;

import com.konfigyr.mail.Address;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Recipient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

class MailAssertTest {

	final Mail mail = Mail.builder()
			.subject("test-subject", 1, "foo", false)
			.template("template-name")
			.to("recipient@konfigyr.com", "Recipient")
			.cc("cc@konfigyr.com", "CC")
			.bcc("bcc@konfigyr.com")
			.from("sender@konfigyr.com", "Sender")
			.replyTo("no-reply@konfigyr.com", "No reply")
			.attribute("foo", "bar")
			.encoding(StandardCharsets.UTF_8)
			.locale(Locale.JAPANESE)
			.build();

	@Test
	@DisplayName("should assert mail message")
	void shouldAssertMailMessage() {
		MailAssert.assertThat(mail)
				.hasSubject("test-subject")
				.hasSubject("test-subject", 1, "foo", false)
				.hasTemplate("template-name")
				.containsRecipient(Recipient.to("recipient@konfigyr.com", "Recipient"))
				.containsRecipients(
						Recipient.cc("cc@konfigyr.com", "CC"),
						Recipient.bcc("bcc@konfigyr.com")
				)
				.sentBy(new Address("sender@konfigyr.com", "Sender"))
				.hasReplyTo(new Address("no-reply@konfigyr.com", "No reply"))
				.hasEncoding(StandardCharsets.UTF_8)
				.hasLocale(Locale.JAPANESE)
				.hasAttributeSatisfying("foo", it -> assertThat(it).isEqualTo("bar"))
				.hasAttribute("foo", "bar");
	}

	@Test
	@DisplayName("should fail to assert mail message")
	void assertMailMessageFailure() {
		final var instance = assertThat(mail)
				.asInstanceOf(MailAssert.factory());

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasSubject("subject"))
				.withMessageContaining("Expected that Mail message should have a value of \"subject\" but was \"test-subject\"");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasSubject("subject", 1))
				.withMessageContaining("Expected that Mail message should have a value of \"subject\" but was \"test-subject\"");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasSubject("test-subject", 1))
				.withMessageContaining("Expected that Mail message should have following formatting arguments: \"[1]\" but was \"[1, foo, false]\"");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasTemplate("template"))
				.withMessageContaining("Expected that Mail message should have a template name of \"template\" but was \"template-name\"");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasEncoding(StandardCharsets.ISO_8859_1))
				.withMessageContaining("Expected that Mail message should have a encoding of ISO-8859-1 but was UTF-8");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasLocale(Locale.ENGLISH))
				.withMessageContaining("Expected that Mail message should have a locale of en but was ja");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasRecipients(Recipient.to("recipient@konfigyr.com")))
				.withMessageContaining("Mail message should contain the following recipient");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.containsRecipient(Recipient.to("recipient@konfigyr.com")))
				.withMessageContaining("Mail message should contain the following recipient");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.containsRecipients(Recipient.to("recipient@konfigyr.com")))
				.withMessageContaining("Mail message should contain the following recipients");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasReplyTo(new Address("recipient@konfigyr.com")))
				.withMessageContaining("Expected that Mail message should have a reply-to of");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.sentBy(new Address("recipient@konfigyr.com")))
				.withMessageContaining("Expected that Mail message should be sent from");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> instance.hasAttribute("foo", "baz"))
				.withMessageContaining("Mail message should contain the following attribute entry: foo -> baz");
	}

}
