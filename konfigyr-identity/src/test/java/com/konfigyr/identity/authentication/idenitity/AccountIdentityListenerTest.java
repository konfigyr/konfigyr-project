package com.konfigyr.identity.authentication.idenitity;

import com.konfigyr.identity.AccountIdentities;
import com.konfigyr.identity.authentication.AccountIdentityEvent;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import com.konfigyr.mail.Recipient;
import com.konfigyr.mail.Subject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountIdentityListenerTest {

	@Mock
	Mailer mailer;

	@Captor
	ArgumentCaptor<Mail> captor;

	AccountIdentityListener listener;

	@BeforeEach
	void setup() {
		listener = new AccountIdentityListener(mailer);
	}

	@Test
	@DisplayName("should send welcome email to created identity")
	void shouldSendWelcomeMail() {
		final var account = AccountIdentities.john().build();

		assertThatNoException().isThrownBy(() -> listener.sendWelcomeMail(
				new AccountIdentityEvent.Created(account)
		));

		verify(mailer).send(captor.capture());

		Assertions.assertThat(captor.getValue())
				.isNotNull()
				.returns("mail/welcome", Mail::template)
				.satisfies(it -> Assertions.assertThat(it.subject())
						.returns("mail.welcome", Subject::value)
						.returns(new Object[0], Subject::arguments)
				)
				.satisfies(it -> Assertions.assertThat(it.recipients())
						.containsExactly(Recipient.to(account.getEmail()))
				)
				.satisfies(it -> Assertions.assertThat(it.attributes())
						.containsEntry("name", account.getDisplayName())
				);
	}

	@Test
	@DisplayName("should fail to send welcome email due to mailer exception")
	void shouldNotSendWelcomeMailWhenMailerFails() {
		final var account = AccountIdentities.jane().build();
		final var cause = new MailPreparationException("Failed to prepare mail");

		doThrow(cause).when(mailer).send(any());

		assertThatThrownBy(() -> listener.sendWelcomeMail(
				new AccountIdentityEvent.Created(account)
		)).isEqualTo(cause);

		verify(mailer).send(any());
	}

}
