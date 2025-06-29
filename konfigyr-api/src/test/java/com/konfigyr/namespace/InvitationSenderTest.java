package com.konfigyr.namespace;

import com.konfigyr.entity.EntityId;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Recipient;
import com.konfigyr.support.FullName;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.assertions.MailAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class InvitationSenderTest extends AbstractIntegrationTest {

	static UriComponents host = UriComponentsBuilder.fromUriString("https://konfigyr.com:8443/namespaces?foo=bar#segment").build();

	@Autowired
	DSLContext context;

	@Captor
	ArgumentCaptor<Mail> captor;

	InvitationSender sender;

	@BeforeEach
	void setup() {
		sender = new InvitationSender(mailer, context);
	}

	@Test
	@DisplayName("should send invitation email when created event is published")
	void shouldSendInvitation() {
		final var event = new InvitationEvent.Created(EntityId.from(2), "09320d7f8e21143b2957f1caded74cbc", host);

		assertThatNoException().isThrownBy(() -> sender.send(event));

		verify(mailer).send(captor.capture());

		MailAssert.assertThat(captor.getValue())
				.isNotNull()
				.hasSubject("mail.invitation.subject", "Konfigyr")
				.hasTemplate("mail/invitation")
				.hasRecipients(Recipient.to("invitee@konfigyr.com"))
				.hasAttribute("sender", FullName.of("John", "Doe"))
				.hasAttribute("namespace", "Konfigyr")
				.hasAttribute("link", "https://konfigyr.com:8443/namespace/konfigyr/members/invitation/09320d7f8e21143b2957f1caded74cbc")
				.hasAttributeSatisfying("expiration", date -> Assertions.assertThat(date)
						.isInstanceOf(OffsetDateTime.class)
						.asInstanceOf(InstanceOfAssertFactories.OFFSET_DATE_TIME)
						.isInTheFuture()
				);
	}

	@Test
	@DisplayName("should fail to send email when invitation is not found")
	void shouldFailToSendUnknownInvitation() {
		final var event = new InvitationEvent.Created(EntityId.from(2), "unknown", host);

		assertThatThrownBy(() -> sender.send(event))
				.isInstanceOf(InvitationException.class)
				.extracting("code")
				.isEqualTo(InvitationException.ErrorCode.INVITATION_NOT_FOUND);

		Mockito.verifyNoInteractions(mailer);
	}

	@Test
	@DisplayName("should fail to send invitation email due to mailer exception")
	void shouldNotCatchMailerExceptions() {
		final var event = new InvitationEvent.Created(EntityId.from(2), "09320d7f8e21143b2957f1caded74cbc", host);

		doThrow(MailSendException.class).when(mailer).send(any());

		assertThatThrownBy(() -> sender.send(event))
				.isInstanceOf(MailSendException.class);

		verify(mailer).send(any());
	}

}
