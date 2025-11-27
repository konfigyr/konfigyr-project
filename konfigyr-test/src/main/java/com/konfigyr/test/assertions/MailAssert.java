package com.konfigyr.test.assertions;

import com.konfigyr.mail.Address;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Recipient;
import org.assertj.core.api.*;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.jspecify.annotations.NonNull;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Assert class that should be used to test {@link Mail}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
public final class MailAssert extends AbstractObjectAssert<MailAssert, Mail> {

	/**
	 * Creates a new {@link MailAssert} with the given {@link Mail mail message} to check.
	 *
	 * @param mail the actual value to verify
	 * @return Mail message assert
	 */
	@NonNull
	public static MailAssert assertThat(Mail mail) {
		return new MailAssert(mail);
	}

	/**
	 * Create an {@link InstanceOfAssertFactory} that can be used to create {@link MailAssert} for
	 * an asserted object.
	 *
	 * @return Mail message assert factory
	 */
	@NonNull
	public static InstanceOfAssertFactory<Mail, MailAssert> factory() {
		return new InstanceOfAssertFactory<>(Mail.class, MailAssert::new);
	}

	MailAssert(Mail mail) {
		super(mail, MailAssert.class);
	}

	/**
	 * Checks if the given {@link Mail} has a matching subject.
	 *
	 * @param subject subject line
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasSubject(String subject) {
		isNotNull();

		if (!Objects.equals(subject, actual.subject().value())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have a value of %s but was %s",
					subject, actual.subject().value()
			));
		}

		return this;
	}

	/**
	 * Checks if the given {@link Mail} has a matching subject.
	 *
	 * @param subject subject line
	 * @param args subject message formating arguments
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasSubject(String subject, Object... args) {
		isNotNull();

		if (!Objects.equals(subject, actual.subject().value())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have a value of %s but was %s",
					subject, actual.subject().value()
			));
		}

		if (!Arrays.equals(args, actual.subject().arguments())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have following formatting arguments: %s but was %s",
					Arrays.toString(args), Arrays.toString(actual.subject().arguments())
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link Mail} has a matching template name.
	 *
	 * @param template template name
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasTemplate(String template) {
		isNotNull();

		if (!Objects.equals(template, actual.template())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have a template name of %s but was %s",
					template, actual.template()
			));
		}

		return this;
	}

	/**
	 * Checks if the given {@link Mail} was sent by the given sender address.
	 *
	 * @param sender expected sender address
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert sentBy(Address sender) {
		isNotNull();

		if (!Objects.equals(sender, actual.from())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should be sent from %s but was %s",
					sender, actual.from()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link Mail} has the following reply-to address.
	 *
	 * @param address expected reply-to address
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasReplyTo(Address address) {
		isNotNull();

		if (!actual.replyTo().contains(address)) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have a reply-to of %s but was %s",
					address, actual.from()
			));
		}

		return myself;
	}

	/**
	 * Checks if the given {@link Mail} has contains exactly the following recipients.
	 *
	 * @param recipients expected recipients
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasRecipients(Recipient... recipients) {
		isNotNull();

		Assertions.assertThat(actual.recipients())
				.as("Mail message should contain the following recipients: %s", Arrays.toString(recipients))
				.containsExactlyInAnyOrder(recipients);

		return myself;
	}

	/**
	 * Checks if the given {@link Mail} has contains the following recipient.
	 *
	 * @param recipient expected recipient
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert containsRecipient(Recipient recipient) {
		isNotNull();

		Assertions.assertThat(actual.recipients())
				.as("Mail message should contain the following recipient: %s", recipient)
				.contains(recipient);

		return myself;
	}

	/**
	 * Checks if the given {@link Mail} has contains any of the following recipients.
	 *
	 * @param recipient expected recipients
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert containsRecipients(Recipient... recipient) {
		isNotNull();

		Assertions.assertThat(actual.recipients())
				.as("Mail message should contain the following recipients: %s", Arrays.toString(recipient))
				.containsAnyOf(recipient);

		return myself;
	}

	/**
	 * Checks if the given {@link Mail} has a matching encoding.
	 *
	 * @param encoding template character encoding
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasEncoding(Charset encoding) {
		isNotNull();

		if (!Objects.equals(encoding, actual.encoding())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have a encoding of %s but was %s",
					encoding, actual.encoding()
			));
		}

		return this;
	}

	/**
	 * Checks if the given {@link Mail} has a matching locale.
	 *
	 * @param locale template locale
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasLocale(Locale locale) {
		isNotNull();

		if (!Objects.equals(locale, actual.locale())) {
			throwAssertionError(new BasicErrorMessageFactory(
					"Expected that Mail message should have a locale of %s but was %s",
					locale, actual.locale()
			));
		}

		return this;
	}

	/**
	 * Checks if the given {@link Mail} contains the matching attribute.
	 *
	 * @param key template attribute key
	 * @param value template attribute value
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasAttribute(String key, Object value) {
		attributes().as("Mail message should contain the following attribute entry: %s -> %s", key, value)
				.containsEntry(key, value);

		return this;
	}

	/**
	 * Checks if the given {@link Mail} contains the matching attribute.
	 *
	 * @param key template attribute key
	 * @param consumer template attribute value consumer
	 * @return the mail assert object, never {@literal null}
	 */
	public MailAssert hasAttributeSatisfying(String key, ThrowingConsumer<Object> consumer) {
		attributes().as("Mail message should contain the attribute that satisfies the asserting conditions: %s", key)
				.hasEntrySatisfying(key, consumer);

		return this;
	}

	/**
	 * Returns the {@link MapAssert} to assert mail message attributes.
	 *
	 * @return the mail attributes map assert instance, never {@literal null}
	 */
	private MapAssert<String, Object> attributes() {
		isNotNull();
		return Assertions.assertThat(actual.attributes());
	}
}
