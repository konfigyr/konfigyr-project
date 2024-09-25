package com.konfigyr.namespace;

import com.konfigyr.NamespaceTestConfiguration;
import com.konfigyr.entity.EntityId;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import com.konfigyr.mail.Recipient;
import com.konfigyr.mail.Subject;
import com.konfigyr.support.FullName;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = NamespaceTestConfiguration.class)
class InvitationSenderTest {

	static UriComponents host = UriComponentsBuilder.fromHttpUrl("https://localhost:8443/namespaces?foo=bar#segment").build();

	@Autowired
	DSLContext context;

	@Mock
	Mailer mailer;

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
		final var event = new InvitationEvent.Created(EntityId.from(2), "qT6uq2ZP1Yv2bWmt", host);

		assertThatNoException().isThrownBy(() -> sender.send(event));

		verify(mailer).send(captor.capture());

		assertThat(captor.getValue())
				.isNotNull()
				.returns("mail/invitation", Mail::template)
				.satisfies(it -> assertThat(it.subject())
						.returns("mail.invitation", Subject::value)
						.extracting(Subject::arguments)
						.asInstanceOf(InstanceOfAssertFactories.array(Object[].class))
						.containsExactly("Konfigyr")
				)
				.satisfies(it -> assertThat(it.recipients())
						.containsExactly(Recipient.to("invitee@konfigyr.com"))
				)
				.satisfies(it -> assertThat(it.attributes())
						.containsEntry("sender", FullName.of("John", "Doe"))
						.containsEntry("namespace", "Konfigyr")
						.containsEntry("namespaceLink", "https://localhost:8443/namespace/konfigyr")
						.containsEntry("link", "https://localhost:8443/namespace/konfigyr/members/invitation/qT6uq2ZP1Yv2bWmt")
						.hasEntrySatisfying("expiration", date -> assertThat(date)
								.isInstanceOf(OffsetDateTime.class)
								.asInstanceOf(InstanceOfAssertFactories.OFFSET_DATE_TIME)
								.isCloseTo(OffsetDateTime.now().plusDays(7), within(5, ChronoUnit.MINUTES))
						)
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

		verifyNoInteractions(mailer);
	}

	@Test
	@DisplayName("should fail to send invitation email due to mailer exception")
	void shouldNotCatchMailerExceptions() {
		final var event = new InvitationEvent.Created(EntityId.from(2), "qT6uq2ZP1Yv2bWmt", host);

		doThrow(MailSendException.class).when(mailer).send(any());

		assertThatThrownBy(() -> sender.send(event))
				.isInstanceOf(MailSendException.class);

		verify(mailer).send(any());
	}

}