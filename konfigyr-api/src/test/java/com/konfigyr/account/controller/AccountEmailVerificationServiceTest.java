package com.konfigyr.account.controller;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountEvent;
import com.konfigyr.entity.EntityEvent;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Recipient;
import com.konfigyr.test.AbstractIntegrationTest;
import com.konfigyr.test.TestAccounts;
import com.konfigyr.test.assertions.MailAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith({ MockitoExtension.class, PublishedEventsExtension.class })
class AccountEmailVerificationServiceTest extends AbstractIntegrationTest {

	final Account account = TestAccounts.john().build();

	@Captor
	ArgumentCaptor<Mail> captor;

	@Autowired
	AccountEmailVerificationService service;

	@Autowired
	AccountEmailVerificationProperties properties;

	@AfterEach
	void cleanup() {
		properties.setAlgorithm(MacAlgorithm.HS384);
		properties.setExpiration(Duration.ofMinutes(5));
	}

	@Test
	@Transactional
	@DisplayName("should verify and update account email address using JWT tokens and OTP")
	void update(AssertablePublishedEvents events) {
		final var token = service.issue(account, "john-doe@konfigyr.com");

		assertThat(token)
				.isNotBlank();

		verify(mailer).send(captor.capture());

		MailAssert.assertThat(captor.getValue())
				.hasSubject("mail.email-verification.subject")
				.hasTemplate("mail/email-verification")
				.hasRecipients(Recipient.to("john-doe@konfigyr.com"))
				.hasAttribute("expiration", 5L)
				.hasAttributeSatisfying("code", code -> assertThat(code)
						.isNotNull()
						.asInstanceOf(InstanceOfAssertFactories.STRING)
						.hasSize(6)
				);

		assertThat(service.verify(account, token, captor.getValue().attributes().get("code").toString()))
				.isNotNull()
				.returns(account.id(), Account::id)
				.returns("john-doe@konfigyr.com", Account::email);

		events.assertThat()
				.contains(AccountEvent.Updated.class)
				.matching(EntityEvent::id, account.id());
	}

	@Test
	@DisplayName("should fail to issue verification code for same email address")
	void assertDifferentAddress() {
		assertThatIllegalStateException()
				.isThrownBy(() -> service.issue(account, account.email()))
				.withMessageContaining("email addresses are the same");
	}

	@Test
	@DisplayName("should fail to issue verification code for email address that is used by a different account")
	void assertAddressNotInUse() {
		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.issue(account, "jane.doe@konfigyr.com"))
				.withMessageContaining("Attempted to use an email address that is already in use by another account")
				.returns(AccountEmailVerificationException.ErrorCode.EMAIL_UNAVAILABLE, AccountEmailVerificationException::getCode)
				.withNoCause();
	}

	@Test
	@DisplayName("should fail to issue verification code for unsupported key algorithm")
	void issueWithUnsupportedKeyAlgorithm() {
		properties.setAlgorithm(MacAlgorithm.HS512);

		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.issue(account, "john-doe@konfigyr.com"))
				.withMessageContaining("JWT encoder failed to generate or encode the email verification token")
				.returns(AccountEmailVerificationException.ErrorCode.JWT_ENCODER_ERROR, AccountEmailVerificationException::getCode)
				.withCauseInstanceOf(JwtEncodingException.class);
	}

	@Test
	@DisplayName("should fail to issue verification code for unsupported key algorithm")
	void issueWithInvalidExpiration() {
		properties.setExpiration(Duration.ofMinutes(5).negated());

		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.issue(account, "john-doe@konfigyr.com"))
				.withMessageContaining("Unexpected error occurred while encoding verification token")
				.returns(AccountEmailVerificationException.ErrorCode.JWT_ENCODER_ERROR, AccountEmailVerificationException::getCode)
				.withCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("should fail to issue verification code due to SMTP connectivity issue")
	void issueWithMailerConnection() {
		doThrow(MailSendException.class).when(mailer).send(any());

		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.issue(account, "john-doe@konfigyr.com"))
				.withMessageContaining("Mailer failed to prepare or send the email verification mail message")
				.returns(AccountEmailVerificationException.ErrorCode.MAILER_ERROR, AccountEmailVerificationException::getCode)
				.withCauseInstanceOf(MailSendException.class);
	}

	@Test
	@DisplayName("should fail to issue verification code due to mailer runtime errors")
	void issueWithMailerRuntimeErrors() {
		doThrow(RuntimeException.class).when(mailer).send(any());

		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.issue(account, "john-doe@konfigyr.com"))
				.withMessageContaining("Unexpected error occurred while sending email verification mail message")
				.returns(AccountEmailVerificationException.ErrorCode.MAILER_ERROR, AccountEmailVerificationException::getCode)
				.withCauseInstanceOf(RuntimeException.class);
	}

	@Test
	@DisplayName("should fail to update email address due to invalid verification token")
	void invalidVerificationToken() {
		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.verify(account, "verification-token", "123456"))
				.withMessageContaining("Failed to update account email address due to invalid or expired email verification token")
				.returns(AccountEmailVerificationException.ErrorCode.INVALID_VERIFICATION_CODE, AccountEmailVerificationException::getCode)
				.withCauseInstanceOf(BadJwtException.class);
	}

	@Test
	@DisplayName("should fail to update email address due to invalid verification token signature algorithm")
	void invalidVerificationTokenAlgorithm() {
		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.verify(account, "verification-token", "123456"))
				.withMessageContaining("Failed to update account email address due to invalid or expired email verification token")
				.returns(AccountEmailVerificationException.ErrorCode.INVALID_VERIFICATION_CODE, AccountEmailVerificationException::getCode)
				.withCauseInstanceOf(BadJwtException.class);
	}

	@Test
	@DisplayName("should fail to update email address due to invalid verification code")
	void invalidVerificationCode() {
		final var token = service.issue(account, "john-doe@konfigyr.com");

		assertThatExceptionOfType(AccountEmailVerificationException.class)
				.isThrownBy(() -> service.verify(account, token, "123456"))
				.withMessageContaining("Failed to update account email address due to invalid email verification code")
				.returns(AccountEmailVerificationException.ErrorCode.INVALID_VERIFICATION_CODE, AccountEmailVerificationException::getCode)
				.withNoCause();
	}

}
