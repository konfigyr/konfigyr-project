package com.konfigyr.account.controller;

import com.konfigyr.account.Account;
import com.konfigyr.account.AccountEvent;
import com.konfigyr.mail.Mail;
import com.konfigyr.mail.Mailer;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static com.konfigyr.account.controller.AccountEmailVerificationException.ErrorCode.*;
import static com.konfigyr.data.tables.Accounts.ACCOUNTS;

@Slf4j
@Component
@EnableConfigurationProperties(AccountEmailVerificationProperties.class)
class AccountEmailVerificationService {

	private static final Marker MARKER = MarkerFactory.getMarker("ACCOUNT_EMAIL_VERIFICATION");
	private static final List<String> AUDIENCE_CLAIM = Collections.singletonList("email-verification");

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private final Mailer mailer;
	private final DSLContext context;
	private final ApplicationEventPublisher eventPublisher;
	private final AccountEmailVerificationProperties properties;

	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;

	@Autowired
	AccountEmailVerificationService(
			DSLContext context,
			Mailer mailer,
			ApplicationEventPublisher eventPublisher,
			AccountEmailVerificationProperties properties
	) throws IOException {
		final JWKSource<SecurityContext> keys = createKeySource(properties.getSecret(), properties.getAlgorithm());

		this.mailer = mailer;
		this.context = context;
		this.properties = properties;
		this.eventPublisher = eventPublisher;
		this.jwtEncoder = new NimbusJwtEncoder(keys);
		this.jwtDecoder = createJwtDecoder(keys, properties.getAlgorithm());
	}

	/**
	 * Generates a one-time password {@code OTP} that should be sent to the given email address in order to verify
	 * if the person behind the user account is really the email address owner.
	 * <p>
	 * This method would return a signed and serialized {@link Jwt JSON Web Token} that <strong>must</strong> be
	 * sent along with the issued {@code OTP} to verify the email address.
	 *
	 * @param account account that initiates the email verification process, can't be {@literal null}.
	 * @param email email address to be verified, can't be {@literal null}.
	 * @return verification token, never {@literal null}.
	 */
	@NonNull
	@Transactional(readOnly = true, label = "account-email-verification.issue")
	String issue(@NonNull Account account, @NonNull String email) {
		Assert.hasText(email, "email address must not be empty");
		Assert.state(!account.email().equalsIgnoreCase(email), "email addresses are the same");

		if (context.fetchExists(ACCOUNTS, ACCOUNTS.EMAIL.equalIgnoreCase(email))) {
			throw new AccountEmailVerificationException(EMAIL_UNAVAILABLE,
					"Attempted to use an email address that is already in use by another account");
		}

		log.debug(MARKER, "Attempting to issue email verification mail for Account({})", account.id());

		final String otp = generateOtp();
		final Jwt jwt;

		try {
			final Instant timestamp = Instant.now();

			final JwsHeader header = JwsHeader.with(properties.getAlgorithm())
					.type(JOSEObjectType.JWT.getType())
					.build();

			final String id = passwordEncoder.encode(otp);
			Assert.hasText(id, "Failed to generate JWT ID from one-time password");

			final JwtClaimsSet claims = JwtClaimsSet.builder()
					.id(id)
					.subject(email.toLowerCase())
					.issuedAt(timestamp)
					.notBefore(timestamp)
					.expiresAt(timestamp.plus(properties.getExpiration()))
					.audience(AUDIENCE_CLAIM)
					.build();

			jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
		} catch (JwtEncodingException ex) {
			throw new AccountEmailVerificationException(JWT_ENCODER_ERROR,
					"JWT encoder failed to generate or encode the email verification token", ex);
		} catch (Exception ex) {
			throw new AccountEmailVerificationException(JWT_ENCODER_ERROR,
					"Unexpected error occurred while encoding verification token", ex);
		}

		try {
			final Mail mail = Mail.builder()
					.subject("mail.email-verification.subject")
					.template("mail/email-verification")
					.to(email.toLowerCase())
					.attribute("code", otp)
					.attribute("expiration", properties.getExpiration().toMinutes())
					.build();

			mailer.send(mail);
		} catch (MailException ex) {
			throw new AccountEmailVerificationException(MAILER_ERROR,
					"Mailer failed to prepare or send the email verification mail message", ex);
		} catch (Exception ex) {
			throw new AccountEmailVerificationException(MAILER_ERROR,
					"Unexpected error occurred while sending email verification mail message", ex);
		}

		log.info(MARKER, "Account email verification code has been sent to Account({}) with expiration date: {}",
				account.id(), jwt.getExpiresAt());

		return jwt.getTokenValue();
	}

	/**
	 * Attempts to verify the issued one-time password {@code OTP} and the {@link Jwt verification token} and
	 * update the {@link Account} with the new email address.
	 * <p>
	 * This method verifies the JWTs signature and expiration, then hashes the submitted OTP and compares it
	 * to the hash that is stored as {@link JwtClaimNames#JTI token identifier claim}.
	 * <p>
	 * When verification is successful, this method would return the {@link Account} with the email address
	 * that received the {@code OTP}.
	 *
	 * @param account account to be updated, can't be {@literal null}.
	 * @param token issued verification token, can't be {@literal null}.
	 * @param code issued one-time password, can't be {@literal null}.
	 * @return update account information, never {@literal null}.
	 */
	@Transactional(label = "account-email-verification.update")
	Account verify(@NonNull Account account, @NonNull String token, @NonNull String code) {
		Assert.hasText(token, "Verification token must not be empty");
		Assert.hasText(code, "Verification code must not be empty");

		log.debug(MARKER, "Attempting to verify email verification that was sent to Account({})", account.id());

		final Jwt jwt;

		try {
			jwt = jwtDecoder.decode(token);
		} catch (BadJwtException ex) {
			throw new AccountEmailVerificationException(INVALID_VERIFICATION_CODE,
					"Failed to update account email address due to invalid or expired email verification token", ex);
		} catch (Exception ex) {
			throw new AccountEmailVerificationException(JWT_DECODER_ERROR,
					"Failed to update account email address due to an unexpected JWT decoder error", ex);
		}

		if (!passwordEncoder.matches(code, jwt.getId())) {
			throw new AccountEmailVerificationException(INVALID_VERIFICATION_CODE,
					"Failed to update account email address due to invalid email verification code");
		}

		context.update(ACCOUNTS)
				.set(ACCOUNTS.EMAIL, jwt.getSubject())
				.set(ACCOUNTS.UPDATED_AT, OffsetDateTime.now())
				.where(ACCOUNTS.ID.eq(account.id().get()))
				.execute();

		eventPublisher.publishEvent(new AccountEvent.Updated(account.id()));

		log.info(MARKER, "Successfully updated email address for an Account({})", account.id());

		return Account.builder(account)
				.email(jwt.getSubject())
				.updatedAt(OffsetDateTime.now())
				.build();
	}

	private String generateOtp() {
		final SecureRandom random = new SecureRandom();
		StringBuilder otp = new StringBuilder();
		for (int i = 0; i < properties.getDigits(); i++) {
			otp.append(random.nextInt(10));
		}
		return otp.toString();
	}

	static JWKSource<SecurityContext> createKeySource(Resource secret, MacAlgorithm algorithm) throws IOException {
		final JWK key = new OctetSequenceKey.Builder(secret.getContentAsByteArray())
				.algorithm(JWSAlgorithm.parse(algorithm.getName()))
				.keyID("email-verification")
				.keyUse(KeyUse.SIGNATURE)
				.build();

		return new ImmutableJWKSet<>(new JWKSet(key));
	}

	static JwtDecoder createJwtDecoder(JWKSource<SecurityContext> keys, MacAlgorithm algorithm) {
		final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
		processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.parse(algorithm.getName()), keys));

		final NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, AUDIENCE_CLAIM::equals),
				new JwtClaimValidator<String>(JwtClaimNames.JTI, StringUtils::hasText),
				new JwtClaimValidator<String>(JwtClaimNames.SUB, StringUtils::hasText),
				new JwtTimestampValidator()
		));
		return decoder;
	}

}
