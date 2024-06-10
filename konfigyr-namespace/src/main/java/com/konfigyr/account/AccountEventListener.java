package com.konfigyr.account;

import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.util.ClassUtils.getShortName;

/**
 * Spring component used as an event listener for {@link AccountEvent account events}.
 *
 * @author Vladimir Spasic
 **/
@Slf4j
@RequiredArgsConstructor
class AccountEventListener {

	private static final Marker WELCOME_SENT = MarkerFactory.getMarker("ACCOUNT_WELCOME_MAIL_SENT");

	private final Mailer mailer;
	private final AccountManager manager;

	@Async
	@Retryable
	@TransactionalEventListener(id = "account-welcome-mail", classes = AccountEvent.Registered.class)
	void sendWelcomeMail(@NonNull AccountEvent.Registered event) {
		log.debug("Attempting to send out account welcome mail to account: {}", event.id());

		final Account account = lookupAccountFor(event);

		final Mail mail = Mail.builder()
				.to(account.email())
				.subject("mail.welcome")
				.template("mail/welcome")
				.attribute("name", account.displayName())
				.build();

		mailer.send(mail);

		log.info(WELCOME_SENT, "Successfully sent an account welcome email to account: {}", account.id());
	}

	private Account lookupAccountFor(@NonNull AccountEvent event) {
		if (log.isDebugEnabled()) {
			log.debug("Looking up account for event [type={}, id={}]", getShortName(event.getClass()), event.id());
		}

		return manager.findById(event.id()).orElseThrow(() -> new AccountNotFoundException(
				"Failed to find account with " + event.id() + ", for event " + getShortName(event.getClass())
		));
	}

}
