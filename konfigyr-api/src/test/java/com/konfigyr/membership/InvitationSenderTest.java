package com.konfigyr.membership;

import com.konfigyr.Hostnames;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Recipient;
import com.konfigyr.mail.test.MailAssert;
import com.konfigyr.namespace.NamespaceManager;
import com.konfigyr.support.FullName;
import com.konfigyr.test.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationSenderTest extends AbstractIntegrationTest {

	@Autowired
	NamespaceManager namespaces;

	@Autowired
	DSLContext context;

	@Autowired
	Hostnames hostnames;

	@Captor
	ArgumentCaptor<Mail> captor;

	InvitationSender sender;

	@BeforeEach
	void setup() {
		sender = new InvitationSender(mailer, context, hostnames);
	}

	@Test
	@DisplayName("should send invitation email when created event is published")
	void shouldSendInvitation() {
		final var namespace = namespaces.findBySlug("konfigyr").orElseThrow();
		final var event = new InvitationEvent.Created(namespace, "09320d7f8e21143b2957f1caded74cbc");

		assertThatNoException().isThrownBy(() -> sender.send(event));

		verify(mailer).send(captor.capture());

		MailAssert.assertThat(captor.getValue())
				.isNotNull()
				.hasSubject("mail.invitation.subject", "Konfigyr")
				.hasTemplate("mail/invitation")
				.hasRecipients(Recipient.to("invitee@konfigyr.com"))
				.hasAttribute("sender", FullName.of("John", "Doe"))
				.hasAttribute("namespace", "Konfigyr")
				.hasAttribute("link", "https://konfigyr.com/join/09320d7f8e21143b2957f1caded74cbc")
				.hasAttributeSatisfying("expiration", date -> Assertions.assertThat(date)
						.isInstanceOf(OffsetDateTime.class)
						.asInstanceOf(InstanceOfAssertFactories.OFFSET_DATE_TIME)
						.isInTheFuture()
				);
	}

	@Test
	@DisplayName("should fail to send email when invitation is not found")
	void shouldFailToSendUnknownInvitation() {
		final var namespace = namespaces.findBySlug("konfigyr").orElseThrow();
		final var event = new InvitationEvent.Created(namespace, "unknown");

		assertThatThrownBy(() -> sender.send(event))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.INVITATION_NOT_FOUND);

		Mockito.verifyNoInteractions(mailer);
	}

	@Test
	@DisplayName("should fail to send invitation email due to mailer exception")
	void shouldNotCatchMailerExceptions() {
		final var namespace = namespaces.findBySlug("konfigyr").orElseThrow();
		final var event = new InvitationEvent.Created(namespace, "09320d7f8e21143b2957f1caded74cbc");

		doThrow(MailSendException.class).when(mailer).send(any());

		assertThatThrownBy(() -> sender.send(event))
				.isInstanceOf(MailSendException.class);

		verify(mailer).send(any());
	}

}
