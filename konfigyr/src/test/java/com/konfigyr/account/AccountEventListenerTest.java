package com.konfigyr.account;

import com.konfigyr.entity.EntityId;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import com.konfigyr.mail.Recipient;
import com.konfigyr.mail.Subject;
import com.konfigyr.test.TestAccounts;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountEventListenerTest {

	@Mock
	Mailer mailer;

	@Mock
	AccountManager manager;

	@Captor
	ArgumentCaptor<Mail> captor;

	AccountEventListener listener;

	@BeforeEach
	void setup() {
		listener = new AccountEventListener(mailer, manager);
	}

	@Test
	@DisplayName("should send welcome email to registered user")
	void shouldSendWelcomeMail() {
		final var account = TestAccounts.john()
				.id(4857612456L)
				.build();

		doReturn(Optional.of(account)).when(manager).findById(account.id());

		assertThatNoException().isThrownBy(() -> listener.sendWelcomeMail(
				new AccountEvent.Registered(account.id())
		));

		verify(manager).findById(account.id());
		verify(mailer).send(captor.capture());

		Assertions.assertThat(captor.getValue())
				.isNotNull()
				.returns("mail/welcome", Mail::template)
				.satisfies(it -> Assertions.assertThat(it.subject())
						.returns("mail.welcome", Subject::value)
						.returns(new Object[0], Subject::arguments)
				)
				.satisfies(it -> Assertions.assertThat(it.recipients())
						.containsExactly(Recipient.to(account.email()))
				)
				.satisfies(it -> Assertions.assertThat(it.attributes())
						.containsEntry("name", account.displayName())
				);
	}

	@Test
	@DisplayName("should fail to send welcome email when user is not found")
	void shouldNotSendWelcomeMailWhenUserIsNotFound() {
		final var event = new AccountEvent.Registered(EntityId.from(123));

		assertThatThrownBy(() -> listener.sendWelcomeMail(event))
				.isInstanceOf(AccountNotFoundException.class)
				.hasMessageContaining("Failed to find account")
				.hasNoCause();

		verify(manager).findById(event.id());
		Mockito.verifyNoInteractions(mailer);
	}

	@Test
	@DisplayName("should fail to send welcome email due to mailer exception")
	void shouldNotSendWelcomeMailWhenMailerFails() {
		final var account = TestAccounts.john()
				.id(857163L)
				.build();

		final var event = new AccountEvent.Registered(account.id());
		final var cause = new MailPreparationException("Failed to prepare mail");

		doReturn(Optional.of(account)).when(manager).findById(account.id());
		doThrow(cause).when(mailer).send(any());

		assertThatThrownBy(() -> listener.sendWelcomeMail(event))
				.isEqualTo(cause);

		verify(manager).findById(account.id());
		verify(mailer).send(any());
	}

}
