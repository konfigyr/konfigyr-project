package com.konfigyr.identity.authentication.idenitity;

import com.konfigyr.identity.authentication.AccountIdentity;
import com.konfigyr.identity.authentication.AccountIdentityEvent;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Spring component that defines listeners for {@link AccountIdentityEvent Account Identity events}.
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AccountIdentityListener {

	private final Mailer mailer;

	@Async
	@Retryable
	@TransactionalEventListener(id = "welcome-mail", classes = AccountIdentityEvent.Created.class)
	void sendWelcomeMail(AccountIdentityEvent.Created event) {
		final AccountIdentity account = event.identity();

		log.debug("Attempting to send out account welcome mail to account: {}", event.identity());

		final Mail mail = Mail.builder()
				.to(account.getEmail())
				.subject("mail.welcome")
				.template("mail/welcome")
				.attribute("name", account.getDisplayName())
				.build();

		mailer.send(mail);

		log.info("Successfully sent an account welcome email to account: {}", account.getId());
	}

}
